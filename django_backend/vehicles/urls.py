from django.urls import path
from . import views

urlpatterns = [
    path('', views.vehicle_list_create),
    path('<int:pk>/', views.vehicle_delete),
]
