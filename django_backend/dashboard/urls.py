from django.urls import path
from . import views

urlpatterns = [
    path('parked-vehicles/', views.parked_vehicles),
    path('revenue/', views.revenue_report),
    path('gate-override/', views.gate_override),
    path('spots-overview/', views.spots_overview),
    path('users/', views.user_list),
]
