from django.db import models
from django.conf import settings


class ParkingSpot(models.Model):
    SPOT_TYPES = [('regular', 'Regular'), ('handicap', 'Handicap'), ('ev', 'EV Charging')]

    spot_number = models.IntegerField(unique=True)
    floor = models.CharField(max_length=5, default='G')
    spot_type = models.CharField(max_length=20, choices=SPOT_TYPES, default='regular')
    is_occupied = models.BooleanField(default=False)
    current_vehicle = models.ForeignKey(
        'vehicles.Vehicle', null=True, blank=True, on_delete=models.SET_NULL, related_name='parked_at'
    )
    sensor_active = models.BooleanField(default=True)

    def __str__(self):
        status = 'Occupied' if self.is_occupied else 'Free'
        return f'Spot {self.spot_number} ({status})'


class ParkingSession(models.Model):
    STATUS_CHOICES = [('active', 'Active'), ('completed', 'Completed'), ('cancelled', 'Cancelled')]

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='parking_sessions')
    vehicle = models.ForeignKey('vehicles.Vehicle', on_delete=models.CASCADE)
    spot = models.ForeignKey(ParkingSpot, on_delete=models.SET_NULL, null=True)
    entry_time = models.DateTimeField(auto_now_add=True)
    exit_time = models.DateTimeField(null=True, blank=True)
    duration_minutes = models.IntegerField(null=True, blank=True)
    amount_charged = models.DecimalField(max_digits=10, decimal_places=2, null=True, blank=True)
    payment_method = models.CharField(max_length=10, default='wallet')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='active')
    tx_hash = models.CharField(max_length=66, null=True, blank=True)

    def __str__(self):
        return f'{self.vehicle.plate_number} at Spot {self.spot.spot_number if self.spot else "?"}'


class Transaction(models.Model):
    TYPE_CHOICES = [('entry', 'Entry'), ('exit', 'Exit'), ('topup', 'Top Up'),
                    ('reservation', 'Reservation'), ('penalty', 'Penalty')]
    STATUS_CHOICES = [('success', 'Success'), ('failed', 'Failed')]

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='transactions')
    vehicle = models.ForeignKey('vehicles.Vehicle', on_delete=models.CASCADE, null=True, blank=True)
    session = models.ForeignKey(ParkingSession, on_delete=models.SET_NULL, null=True, blank=True)
    plate_number = models.CharField(max_length=15)
    amount = models.DecimalField(max_digits=10, decimal_places=2)
    type = models.CharField(max_length=20, choices=TYPE_CHOICES)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES)
    message = models.TextField(blank=True)
    payment_method = models.CharField(max_length=10, default='wallet')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f'{self.type} - {self.plate_number} - ${self.amount}'
