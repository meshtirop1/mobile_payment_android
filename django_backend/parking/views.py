from decimal import Decimal
from django.utils import timezone
from django.db import transaction as db_transaction
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from django.conf import settings as django_settings
from vehicles.models import Vehicle
from .models import ParkingSpot, ParkingSession, Transaction
from .pricing import calculate_parking_fee
from notifications.helpers import create_notification
from iot.serial_handler import send_gate_command


@api_view(['GET'])
def spot_list(request):
    spots = ParkingSpot.objects.all().order_by('spot_number')
    data = []
    from reservations.models import Reservation
    reserved_spots = set(
        Reservation.objects.filter(status__in=['pending', 'active'])
        .values_list('spot__spot_number', flat=True)
    )

    for s in spots:
        if s.is_occupied:
            spot_status = 'occupied'
        elif s.spot_number in reserved_spots:
            spot_status = 'reserved'
        else:
            spot_status = 'free'

        data.append({
            'spot_number': s.spot_number,
            'floor': s.floor,
            'spot_type': s.spot_type,
            'is_occupied': s.is_occupied,
            'status': spot_status,
            'current_vehicle': s.current_vehicle.plate_number if s.current_vehicle else None,
            'sensor_active': s.sensor_active,
        })

    total = spots.count()
    occupied = spots.filter(is_occupied=True).count()
    return Response({
        'spots': data,
        'total': total,
        'occupied': occupied,
        'available': total - occupied,
    })


@api_view(['POST'])
def parking_entry(request):
    return _do_parking_entry(request)


def _do_parking_entry(request):
    plate = request.data.get('plate_number', '').strip().upper()
    payment_method = request.data.get('payment_method', 'wallet')
    spot_number = request.data.get('spot_number')

    if not plate:
        return Response({'error': 'Plate number is required', 'gate': 'closed'}, status=status.HTTP_400_BAD_REQUEST)

    # Check vehicle exists
    try:
        vehicle = Vehicle.objects.select_related('user').get(plate_number=plate)
    except Vehicle.DoesNotExist:
        send_gate_command('DENY', plate, 'Not registered')
        return Response({
            'error': 'Vehicle not registered in the system',
            'plate_number': plate, 'gate': 'closed'
        }, status=status.HTTP_404_NOT_FOUND)

    # Check ownership
    if vehicle.user_id != request.user.id:
        send_gate_command('DENY', plate, 'Wrong owner')
        return Response({
            'error': 'This vehicle is not registered to your account',
            'plate_number': plate, 'gate': 'closed'
        }, status=status.HTTP_403_FORBIDDEN)

    # Check for active session (already parked)
    active = ParkingSession.objects.filter(vehicle=vehicle, status='active').first()
    if active:
        return Response({
            'error': 'Vehicle is already parked',
            'plate_number': plate, 'gate': 'closed',
            'spot_number': active.spot.spot_number if active.spot else None,
        }, status=status.HTTP_409_CONFLICT)

    # Check duplicate rapid entry
    window = timezone.now() - timezone.timedelta(seconds=django_settings.DUPLICATE_ENTRY_WINDOW_SECONDS)
    recent = Transaction.objects.filter(
        vehicle=vehicle, type='entry', status='success', created_at__gt=window
    ).exists()
    if recent:
        return Response({
            'error': f'Entry already recorded within the last {django_settings.DUPLICATE_ENTRY_WINDOW_SECONDS} seconds',
            'plate_number': plate, 'gate': 'closed'
        }, status=status.HTTP_429_TOO_MANY_REQUESTS)

    # Find or assign spot
    if spot_number:
        try:
            spot = ParkingSpot.objects.get(spot_number=spot_number)
        except ParkingSpot.DoesNotExist:
            return Response({'error': 'Spot not found', 'gate': 'closed'}, status=status.HTTP_404_NOT_FOUND)
        if spot.is_occupied:
            return Response({'error': f'Spot {spot_number} is occupied', 'gate': 'closed'}, status=status.HTTP_409_CONFLICT)
    else:
        spot = ParkingSpot.objects.filter(is_occupied=False).order_by('spot_number').first()
        if not spot:
            send_gate_command('DENY', plate, 'Lot full')
            return Response({'error': 'No available parking spots', 'gate': 'closed'}, status=status.HTTP_409_CONFLICT)

    # Entry is free (billing at exit), just create session
    with db_transaction.atomic():
        session = ParkingSession.objects.create(
            user=request.user, vehicle=vehicle, spot=spot,
            payment_method=payment_method, status='active'
        )
        spot.is_occupied = True
        spot.current_vehicle = vehicle
        spot.save()

        Transaction.objects.create(
            user=request.user, vehicle=vehicle, session=session,
            plate_number=plate, amount=Decimal('0.00'),
            type='entry', status='success',
            message=f'Gate opened - Parked at spot {spot.spot_number}',
            payment_method=payment_method,
        )

    send_gate_command('OPEN', plate, f'Spot {spot.spot_number}')
    create_notification(
        request.user, 'Vehicle Parked',
        f'{plate} parked at spot {spot.spot_number}. First 30 min free!',
        'entry'
    )

    total = ParkingSpot.objects.count()
    occupied = ParkingSpot.objects.filter(is_occupied=True).count()

    return Response({
        'message': f'Gate opened - Parked at spot {spot.spot_number}',
        'plate_number': plate,
        'spot_number': spot.spot_number,
        'entry_time': session.entry_time.isoformat(),
        'session_id': session.id,
        'gate': 'open',
        'payment_method': payment_method,
        'available_spots': total - occupied,
    })


@api_view(['POST'])
def parking_exit(request):
    session_id = request.data.get('session_id')
    plate = request.data.get('plate_number', '').strip().upper()

    # Find session by session_id or plate_number
    session = None
    if session_id:
        session = ParkingSession.objects.filter(
            id=session_id, user=request.user, status='active'
        ).select_related('vehicle', 'spot').first()
        if session:
            plate = session.vehicle.plate_number

    if not session:
        if not plate:
            # No session_id and no plate — try to find any active session
            session = ParkingSession.objects.filter(
                user=request.user, status='active'
            ).select_related('vehicle', 'spot').first()
            if session:
                plate = session.vehicle.plate_number
            else:
                return Response({'error': 'No active parking session', 'gate': 'closed'}, status=status.HTTP_404_NOT_FOUND)
        else:
            try:
                vehicle = Vehicle.objects.get(plate_number=plate, user=request.user)
            except Vehicle.DoesNotExist:
                return Response({'error': 'Vehicle not found', 'gate': 'closed'}, status=status.HTTP_404_NOT_FOUND)

            session = ParkingSession.objects.filter(vehicle=vehicle, status='active').first()
            if not session:
                return Response({'error': 'No active parking session for this vehicle', 'gate': 'closed'}, status=status.HTTP_404_NOT_FOUND)

    now = timezone.now()
    duration = int((now - session.entry_time).total_seconds() / 60)
    fee = calculate_parking_fee(duration)

    payment_method = session.payment_method

    vehicle = session.vehicle

    # Check balance for wallet payment
    if payment_method == 'wallet' and request.user.balance < fee:
        send_gate_command('DENY', plate, 'Low balance')
        Transaction.objects.create(
            user=request.user, vehicle=vehicle, session=session,
            plate_number=plate, amount=fee,
            type='exit', status='failed',
            message='Insufficient wallet balance for exit',
            payment_method='wallet',
        )
        return Response({
            'error': 'Insufficient wallet balance',
            'required': float(fee),
            'current_balance': float(request.user.balance),
            'duration_minutes': duration,
            'plate_number': plate, 'gate': 'closed',
        }, status=status.HTTP_402_PAYMENT_REQUIRED)

    with db_transaction.atomic():
        # Deduct fee
        if fee > 0 and payment_method == 'wallet':
            request.user.balance -= fee
            request.user.save()

        # Complete session
        session.exit_time = now
        session.duration_minutes = duration
        session.amount_charged = fee
        session.status = 'completed'
        session.save()

        # Free spot
        if session.spot:
            session.spot.is_occupied = False
            session.spot.current_vehicle = None
            session.spot.save()

        Transaction.objects.create(
            user=request.user, vehicle=vehicle, session=session,
            plate_number=plate, amount=fee,
            type='exit', status='success',
            message=f'Exit - {duration} min, charged ${fee:.2f}',
            payment_method=payment_method,
        )

    send_gate_command('OPEN', plate, f'Exit ${fee:.2f}')

    fee_breakdown = 'Free (under 30 min)' if fee == 0 else f'${fee:.2f}'
    create_notification(
        request.user, 'Vehicle Exited',
        f'{plate} exited after {duration} min. Fee: {fee_breakdown}',
        'exit'
    )

    return Response({
        'message': f'Gate opened - Exit successful',
        'plate_number': plate,
        'spot_number': session.spot.spot_number if session.spot else 0,
        'duration_minutes': duration,
        'fee_charged': float(fee),
        'remaining_balance': float(request.user.balance),
        'exit_time': now.isoformat(),
        'gate': 'open',
        'payment_method': payment_method,
    })


@api_view(['GET'])
def active_session(request):
    sessions = ParkingSession.objects.filter(user=request.user, status='active').select_related('vehicle', 'spot')
    now = timezone.now()

    # Build list for multi-session support
    session_list = []
    for s in sessions:
        duration = int((now - s.entry_time).total_seconds() / 60)
        est_fee = calculate_parking_fee(duration)
        session_list.append({
            'id': s.id,
            'plate_number': s.vehicle.plate_number,
            'spot_number': s.spot.spot_number if s.spot else None,
            'entry_time': s.entry_time.isoformat(),
            'duration_minutes': duration,
            'estimated_fee': float(est_fee),
            'payment_method': s.payment_method,
        })

    # Return both flat fields (for Android) and list (for web/test)
    if session_list:
        first = session_list[0]
        return Response({
            'has_session': True,
            'session_id': first['id'],
            'plate_number': first['plate_number'],
            'spot_number': first['spot_number'],
            'entry_time': first['entry_time'],
            'duration_minutes': first['duration_minutes'],
            'estimated_fee': first['estimated_fee'],
            'sessions': session_list,
        })
    else:
        return Response({
            'has_session': False,
            'sessions': [],
        })


@api_view(['GET'])
def transaction_history(request):
    txns = Transaction.objects.filter(user=request.user)[:50]
    data = [{
        'id': t.id, 'plate_number': t.plate_number,
        'amount': float(t.amount), 'type': t.type, 'status': t.status,
        'message': t.message, 'payment_method': t.payment_method,
        'created_at': t.created_at.isoformat(),
    } for t in txns]
    return Response({'transactions': data})


# Legacy endpoint for backward compatibility
@api_view(['POST'])
def simulate_entry(request):
    return _do_parking_entry(request)
