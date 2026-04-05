from django.urls import path
from . import views

urlpatterns = [
    path('gate-command/', views.gate_command),
    path('gate-status/', views.gate_status),
    path('sensor-update/', views.sensor_update),
    path('sensor-readings/', views.sensor_readings),
]
