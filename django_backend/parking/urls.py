from django.urls import path
from . import views

urlpatterns = [
    path('spots/', views.spot_list),
    path('entry/', views.parking_entry),
    path('exit/', views.parking_exit),
    path('active-session/', views.active_session),
    path('transactions/history/', views.transaction_history),
    path('simulate-entry/', views.simulate_entry),
]
