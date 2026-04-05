from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .models import Notification


@api_view(['GET'])
def notification_list(request):
    notifications = Notification.objects.filter(user=request.user)[:50]
    data = [{
        'id': n.id, 'title': n.title, 'message': n.message,
        'type': n.type, 'is_read': n.is_read,
        'created_at': n.created_at.isoformat(),
    } for n in notifications]
    return Response({'notifications': data})


@api_view(['GET'])
def unread_count(request):
    count = Notification.objects.filter(user=request.user, is_read=False).count()
    return Response({'count': count})


@api_view(['POST'])
def mark_read(request, pk):
    try:
        notification = Notification.objects.get(id=pk, user=request.user)
    except Notification.DoesNotExist:
        return Response({'error': 'Notification not found'}, status=status.HTTP_404_NOT_FOUND)

    notification.is_read = True
    notification.save()
    return Response({'message': 'Marked as read'})


@api_view(['POST'])
def mark_all_read(request):
    Notification.objects.filter(user=request.user, is_read=False).update(is_read=True)
    return Response({'message': 'All notifications marked as read'})
