from decimal import Decimal
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status


@api_view(['GET'])
def wallet_balance(request):
    return Response({'balance': float(request.user.balance)})


@api_view(['POST'])
def wallet_topup(request):
    try:
        amount = Decimal(str(request.data.get('amount', 0)))
    except (TypeError, ValueError):
        return Response({'error': 'Invalid amount'}, status=status.HTTP_400_BAD_REQUEST)

    if amount <= 0:
        return Response({'error': 'Amount must be positive'}, status=status.HTTP_400_BAD_REQUEST)
    if amount > 1000:
        return Response({'error': 'Maximum top-up is $1000'}, status=status.HTTP_400_BAD_REQUEST)

    request.user.balance += amount
    request.user.save()

    return Response({
        'message': f'Successfully added ${amount:.2f} to wallet',
        'balance': float(request.user.balance),
    })
