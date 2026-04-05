from decimal import Decimal
from django.utils import timezone
from datetime import timedelta
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from accounts.models import User
from parking.models import ParkingSpot, ParkingSession, Transaction
from iot.serial_handler import send_gate_command


def admin_required(view_func):
    def wrapper(request, *args, **kwargs):
        if not request.user.is_parking_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)
        return view_func(request, *args, **kwargs)
    return wrapper


@api_view(['GET'])
@admin_required
def parked_vehicles(request):
    sessions = ParkingSession.objects.filter(status='active').select_related('user', 'vehicle', 'spot')
    now = timezone.now()
    data = [{
        'plate_number': s.vehicle.plate_number,
        'user_name': s.user.first_name,
        'user_email': s.user.email,
        'spot_number': s.spot.spot_number if s.spot else None,
        'entry_time': s.entry_time.isoformat(),
        'duration_minutes': int((now - s.entry_time).total_seconds() / 60),
    } for s in sessions]
    return Response({'parked_vehicles': data, 'count': len(data)})


@api_view(['GET'])
@admin_required
def revenue_report(request):
    now = timezone.now()
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    week_start = today_start - timedelta(days=today_start.weekday())
    month_start = today_start.replace(day=1)

    def sum_revenue(since):
        txns = Transaction.objects.filter(
            status='success', type__in=['exit', 'penalty'],
            created_at__gte=since
        )
        total = sum(t.amount for t in txns)
        return float(total)

    total_all = float(sum(
        t.amount for t in Transaction.objects.filter(
            status='success', type__in=['exit', 'penalty']
        )
    ))

    return Response({
        'today_revenue': sum_revenue(today_start),
        'week_revenue': sum_revenue(week_start),
        'month_revenue': sum_revenue(month_start),
        'total': total_all,
        'total_sessions_today': ParkingSession.objects.filter(
            entry_time__gte=today_start
        ).count(),
    })


@api_view(['POST'])
@admin_required
def gate_override(request):
    action = request.data.get('action', '').upper()
    if action not in ['OPEN', 'DENY']:
        return Response({'error': 'Action must be OPEN or DENY'}, status=status.HTTP_400_BAD_REQUEST)

    success = send_gate_command(action, 'ADMIN', f'Override by {request.user.email}')
    return Response({
        'message': f'Gate {action} command sent',
        'serial_connected': success,
    })


@api_view(['GET'])
@admin_required
def spots_overview(request):
    total = ParkingSpot.objects.count()
    occupied = ParkingSpot.objects.filter(is_occupied=True).count()

    from reservations.models import Reservation
    reserved = Reservation.objects.filter(status__in=['pending', 'active']).count()

    return Response({
        'total_spots': total,
        'occupied_spots': occupied,
        'free_spots': total - occupied,
        'reserved_spots': reserved,
    })


@api_view(['GET'])
@admin_required
def user_list(request):
    users = User.objects.all().order_by('-date_joined')
    data = [{
        'id': u.id,
        'name': u.first_name,
        'email': u.email,
        'balance': float(u.balance),
        'vehicle_count': u.vehicles.count(),
        'is_admin': u.is_parking_admin,
        'joined': u.date_joined.isoformat(),
    } for u in users]
    return Response({'users': data})
