from django.urls import path
from . import views

urlpatterns = [
    path('', views.wallet_balance),
    path('topup/', views.wallet_topup),
]
