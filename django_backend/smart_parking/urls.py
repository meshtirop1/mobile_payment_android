from django.contrib import admin
from django.urls import path, include
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

@api_view(['GET'])
@permission_classes([AllowAny])
def health_check(request):
    return Response({'status': 'ok', 'message': 'Smart Parking API is running'})

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/health/', health_check),
    path('api/', include('accounts.urls')),
    path('api/vehicles/', include('vehicles.urls')),
    path('api/wallet/', include('wallet.urls')),
    path('api/parking/', include('parking.urls')),
    path('api/reservations/', include('reservations.urls')),
    path('api/notifications/', include('notifications.urls')),
    path('api/iot/', include('iot.urls')),
    path('api/crypto/', include('blockchain_app.urls')),
    path('api/admin-dashboard/', include('dashboard.urls')),
]
