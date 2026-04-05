from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status
from django.conf import settings as django_settings
from parking.models import ParkingSpot
from .models import GateLog, SensorReading
from .serial_handler import send_gate_command


@api_view(['POST'])
def gate_command(request):
    action = request.data.get('action', '').upper()
    plate = request.data.get('plate_number', '')
    detail = request.data.get('detail', '')

    if action not in ['OPEN', 'DENY']:
        return Response({'error': 'Action must be OPEN or DENY'}, status=status.HTTP_400_BAD_REQUEST)

    success = send_gate_command(action, plate, detail)
    return Response({
        'message': f'Gate command sent: {action}',
        'serial_connected': success,
    })


@api_view(['GET'])
def gate_status(request):
    last_log = GateLog.objects.first()
    return Response({
        'serial_enabled': django_settings.SERIAL_ENABLED,
        'last_action': last_log.action if last_log else None,
        'last_plate': last_log.plate_number if last_log else None,
        'last_time': last_log.timestamp.isoformat() if last_log else None,
    })


@api_view(['POST'])
@permission_classes([AllowAny])
def sensor_update(request):
    """Arduino pushes sensor data to this endpoint."""
    spot_number = request.data.get('spot_number')
    distance = request.data.get('distance_cm')

    if spot_number is None or distance is None:
        return Response({'error': 'spot_number and distance_cm required'}, status=status.HTTP_400_BAD_REQUEST)

    is_occupied = float(distance) < 10

    SensorReading.objects.create(
        spot_number=int(spot_number),
        distance_cm=float(distance),
        is_occupied=is_occupied,
    )

    # Update parking spot
    try:
        spot = ParkingSpot.objects.get(spot_number=int(spot_number))
        spot.is_occupied = is_occupied
        if not is_occupied:
            spot.current_vehicle = None
        spot.save()
    except ParkingSpot.DoesNotExist:
        pass

    return Response({'message': 'Sensor data recorded', 'is_occupied': is_occupied})


@api_view(['GET'])
def sensor_readings(request):
    # Get latest reading for each spot
    spots = ParkingSpot.objects.all().order_by('spot_number')
    data = []
    for spot in spots:
        reading = SensorReading.objects.filter(spot_number=spot.spot_number).first()
        data.append({
            'spot_number': spot.spot_number,
            'distance_cm': reading.distance_cm if reading else None,
            'is_occupied': reading.is_occupied if reading else spot.is_occupied,
            'last_update': reading.timestamp.isoformat() if reading else None,
        })
    return Response({'readings': data})
