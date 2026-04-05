import re
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .models import Vehicle


@api_view(['GET', 'POST'])
def vehicle_list_create(request):
    if request.method == 'GET':
        vehicles = Vehicle.objects.filter(user=request.user).values(
            'id', 'plate_number', 'created_at'
        )
        return Response({'vehicles': list(vehicles)})

    plate = request.data.get('plate_number', '').strip().upper()
    if not plate or len(plate) < 2 or len(plate) > 15:
        return Response({'error': 'Plate number must be 2-15 characters'}, status=status.HTTP_400_BAD_REQUEST)

    if not re.match(r'^[A-Z0-9]+$', plate):
        return Response({'error': 'Plate number must contain only letters and numbers'}, status=status.HTTP_400_BAD_REQUEST)

    if Vehicle.objects.filter(plate_number=plate).exists():
        return Response({'error': 'This plate number is already registered'}, status=status.HTTP_409_CONFLICT)

    vehicle = Vehicle.objects.create(user=request.user, plate_number=plate)
    return Response({
        'message': 'Vehicle added successfully',
        'vehicle': {'id': vehicle.id, 'plate_number': vehicle.plate_number}
    }, status=status.HTTP_201_CREATED)


@api_view(['DELETE'])
def vehicle_delete(request, pk):
    try:
        vehicle = Vehicle.objects.get(id=pk, user=request.user)
    except Vehicle.DoesNotExist:
        return Response({'error': 'Vehicle not found'}, status=status.HTTP_404_NOT_FOUND)

    vehicle.delete()
    return Response({'message': 'Vehicle removed successfully'})
