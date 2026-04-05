from decimal import Decimal
from django.utils import timezone
from datetime import timedelta
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from vehicles.models import Vehicle
from parking.models import ParkingSpot, Transaction
from notifications.helpers import create_notification
from .models import Reservation


RESERVATION_PENALTY = Decimal('1.00')
HOLD_MINUTES = 15


@api_view(['GET', 'POST'])
def reservation_list_create(request):
    if request.method == 'GET':
        reservations = Reservation.objects.filter(user=request.user).order_by('-created_at')
        data = [{
            'id': r.id, 'spot_number': r.spot.spot_number,
            'plate_number': r.vehicle.plate_number,
            'reserved_from': r.reserved_from.isoformat(),
            'reserved_until': r.reserved_until.isoformat(),
            'hold_expires_at': r.hold_expires_at.isoformat(),
            'status': r.status, 'penalty_charged': r.penalty_charged,
            'created_at': r.created_at.isoformat(),
        } for r in reservations]
        return Response({'reservations': data})

    # Create reservation
    spot_number = request.data.get('spot_number')
    vehicle_id = request.data.get('vehicle_id')
    plate_number = request.data.get('plate_number', '').strip().upper()
    reserved_from = request.data.get('reserved_from')
    reserved_until = request.data.get('reserved_until')

    if not spot_number:
        return Response({'error': 'spot_number is required'}, status=status.HTTP_400_BAD_REQUEST)

    # Look up vehicle by vehicle_id or plate_number
    vehicle = None
    if vehicle_id:
        try:
            vehicle = Vehicle.objects.get(id=vehicle_id, user=request.user)
        except Vehicle.DoesNotExist:
            return Response({'error': 'Vehicle not found'}, status=status.HTTP_404_NOT_FOUND)
    elif plate_number:
        try:
            vehicle = Vehicle.objects.get(plate_number=plate_number, user=request.user)
        except Vehicle.DoesNotExist:
            return Response({'error': f'Vehicle {plate_number} not found or not yours'}, status=status.HTTP_404_NOT_FOUND)
    else:
        return Response({'error': 'vehicle_id or plate_number is required'}, status=status.HTTP_400_BAD_REQUEST)

    try:
        spot = ParkingSpot.objects.get(spot_number=spot_number)
    except ParkingSpot.DoesNotExist:
        return Response({'error': 'Spot not found'}, status=status.HTTP_404_NOT_FOUND)

    if spot.is_occupied:
        return Response({'error': 'Spot is currently occupied'}, status=status.HTTP_409_CONFLICT)

    # Check for conflicting reservations
    now = timezone.now()
    from_time = timezone.datetime.fromisoformat(reserved_from) if reserved_from else now
    until_time = timezone.datetime.fromisoformat(reserved_until) if reserved_until else now + timedelta(hours=2)

    if timezone.is_naive(from_time):
        from_time = timezone.make_aware(from_time)
    if timezone.is_naive(until_time):
        until_time = timezone.make_aware(until_time)

    conflict = Reservation.objects.filter(
        spot=spot, status__in=['pending', 'active'],
        reserved_from__lt=until_time, reserved_until__gt=from_time,
    ).exists()
    if conflict:
        return Response({'error': 'Spot already reserved for this time window'}, status=status.HTTP_409_CONFLICT)

    hold_expires = from_time + timedelta(minutes=HOLD_MINUTES)

    reservation = Reservation.objects.create(
        user=request.user, vehicle=vehicle, spot=spot,
        reserved_from=from_time, reserved_until=until_time,
        hold_expires_at=hold_expires, status='pending',
    )

    create_notification(
        request.user, 'Reservation Created',
        f'Spot {spot_number} reserved for {vehicle.plate_number}. Check in within {HOLD_MINUTES} min of start time.',
        'reservation'
    )

    return Response({
        'message': 'Reservation created successfully',
        'reservation': {
            'id': reservation.id, 'spot_number': spot.spot_number,
            'plate_number': vehicle.plate_number,
            'reserved_from': from_time.isoformat(),
            'reserved_until': until_time.isoformat(),
            'hold_expires_at': hold_expires.isoformat(),
            'status': 'pending',
        }
    }, status=status.HTTP_201_CREATED)


@api_view(['DELETE'])
def reservation_cancel(request, pk):
    try:
        reservation = Reservation.objects.get(id=pk, user=request.user)
    except Reservation.DoesNotExist:
        return Response({'error': 'Reservation not found'}, status=status.HTTP_404_NOT_FOUND)

    if reservation.status in ['completed', 'expired']:
        return Response({'error': 'Cannot cancel this reservation'}, status=status.HTTP_400_BAD_REQUEST)

    reservation.status = 'cancelled'
    reservation.save()

    return Response({'message': 'Reservation cancelled'})


@api_view(['POST'])
def reservation_checkin(request, pk):
    try:
        reservation = Reservation.objects.get(id=pk, user=request.user)
    except Reservation.DoesNotExist:
        return Response({'error': 'Reservation not found'}, status=status.HTTP_404_NOT_FOUND)

    if reservation.status != 'pending':
        return Response({'error': f'Cannot check in - reservation is {reservation.status}'}, status=status.HTTP_400_BAD_REQUEST)

    now = timezone.now()
    if now > reservation.hold_expires_at:
        reservation.status = 'expired'
        reservation.save()
        return Response({'error': 'Reservation has expired'}, status=status.HTTP_410_GONE)

    reservation.status = 'active'
    reservation.save()

    # Redirect to parking entry
    from rest_framework.test import APIRequestFactory
    request.data['plate_number'] = reservation.vehicle.plate_number
    request.data['spot_number'] = reservation.spot.spot_number
    request.data['payment_method'] = 'wallet'

    from parking.views import parking_entry
    return parking_entry(request)


@api_view(['POST'])
def check_expiry(request):
    """Check and expire overdue reservations. Can be called periodically."""
    now = timezone.now()
    expired = Reservation.objects.filter(
        status='pending', hold_expires_at__lt=now
    )

    count = 0
    for r in expired:
        r.status = 'expired'

        # Charge penalty
        if not r.penalty_charged and r.user.balance >= RESERVATION_PENALTY:
            r.user.balance -= RESERVATION_PENALTY
            r.user.save()
            r.penalty_charged = True

            Transaction.objects.create(
                user=r.user, vehicle=r.vehicle,
                plate_number=r.vehicle.plate_number,
                amount=RESERVATION_PENALTY, type='penalty', status='success',
                message=f'Reservation expired - penalty for spot {r.spot.spot_number}',
            )

            create_notification(
                r.user, 'Reservation Expired',
                f'Your reservation for spot {r.spot.spot_number} expired. ${RESERVATION_PENALTY} penalty charged.',
                'penalty'
            )

        r.save()
        count += 1

    return Response({'message': f'{count} reservations expired'})
