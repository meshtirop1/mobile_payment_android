from django.db import models


class GateLog(models.Model):
    gate_id = models.CharField(max_length=20, default='entry_gate')
    action = models.CharField(max_length=10)
    plate_number = models.CharField(max_length=15, blank=True)
    detail = models.CharField(max_length=200, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-timestamp']


class SensorReading(models.Model):
    spot_number = models.IntegerField()
    distance_cm = models.FloatField()
    is_occupied = models.BooleanField()
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-timestamp']
