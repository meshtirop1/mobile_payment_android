from django.urls import path
from . import views

urlpatterns = [
    path('deposit/', views.crypto_deposit),
    path('balance/', views.crypto_balance),
    path('status/', views.blockchain_status),
]
