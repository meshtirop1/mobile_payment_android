from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from . import web3_service


@api_view(['POST'])
def crypto_deposit(request):
    if not web3_service.get_connected():
        return Response({'error': 'Blockchain not available'}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

    amount = request.data.get('amount')
    if not amount or float(amount) <= 0:
        return Response({'error': 'Amount must be positive'}, status=status.HTTP_400_BAD_REQUEST)

    try:
        result = web3_service.deposit_for_user(request.user.id, float(amount))
        balance = web3_service.get_crypto_balance(request.user.id)
        return Response({
            'message': f'Deposited {amount} ETH to smart contract',
            'tx_hash': result['tx_hash'],
            'crypto_balance': balance + ' ETH',
        })
    except Exception as e:
        return Response({'error': f'Deposit failed: {str(e)}'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['GET'])
def crypto_balance(request):
    if not web3_service.get_connected():
        return Response({'balance': '0', 'connected': False, 'error': 'Blockchain not available'})

    try:
        balance = web3_service.get_crypto_balance(request.user.id)
        address = web3_service.get_wallet_address(request.user.id)
        return Response({'balance': balance + ' ETH', 'address': address, 'connected': True})
    except Exception as e:
        return Response({'error': str(e), 'connected': False}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@api_view(['GET'])
def blockchain_status(request):
    return Response({
        'connected': web3_service.get_connected(),
        'network': 'localhost (Hardhat)',
    })
