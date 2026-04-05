from django.db import models
from django.conf import settings


class Reservation(models.Model):
    STATUS_CHOICES = [
        ('pending', 'Pending'), ('active', 'Active'), ('completed', 'Completed'),
        ('expired', 'Expired'), ('cancelled', 'Cancelled'),
    ]

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='reservations')
    vehicle = models.ForeignKey('vehicles.Vehicle', on_delete=models.CASCADE)
    spot = models.ForeignKey('parking.ParkingSpot', on_delete=models.CASCADE)
    reserved_from = models.DateTimeField()
    reserved_until = models.DateTimeField()
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending')
    hold_expires_at = models.DateTimeField()
    penalty_charged = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f'Reservation: Spot {self.spot.spot_number} for {self.vehicle.plate_number}'
