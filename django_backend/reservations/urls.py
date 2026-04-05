from django.urls import path
from . import views

urlpatterns = [
    path('', views.reservation_list_create),
    path('<int:pk>/', views.reservation_cancel),
    path('<int:pk>/check-in/', views.reservation_checkin),
    path('check-expiry/', views.check_expiry),
]
