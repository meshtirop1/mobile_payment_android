from .models import Notification


def create_notification(user, title, message, notification_type='system'):
    return Notification.objects.create(
        user=user, title=title, message=message, type=notification_type
    )
