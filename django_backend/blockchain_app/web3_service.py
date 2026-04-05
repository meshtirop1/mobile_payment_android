import json
import os
import logging
from pathlib import Path
from django.conf import settings

logger = logging.getLogger(__name__)

w3 = None
contract = None
is_connected = False


def init_blockchain():
    global w3, contract, is_connected

    try:
        from web3 import Web3

        # Quick check if node is running
        w3 = Web3(Web3.HTTPProvider(settings.BLOCKCHAIN_RPC_URL, request_kwargs={'timeout': 3}))
        if not w3.is_connected():
            logger.info("Blockchain: Local node not running. Crypto payments disabled.")
            is_connected = False
            return False

        # Load contract
        backend_dir = Path(settings.BASE_DIR).parent / 'backend'
        address_path = backend_dir / 'contract-address.json'
        abi_path = backend_dir / 'contract-abi.json'

        if not address_path.exists() or not abi_path.exists():
            logger.info("Blockchain: Contract not deployed. Crypto payments disabled.")
            is_connected = False
            return False

        with open(address_path) as f:
            address_data = json.load(f)
        with open(abi_path) as f:
            abi = json.load(f)

        contract = w3.eth.contract(
            address=w3.to_checksum_address(address_data['address']),
            abi=abi
        )

        is_connected = True
        logger.info(f"Blockchain: Connected. Contract at {address_data['address']}")
        return True

    except Exception as e:
        logger.info(f"Blockchain: Not available - {e}")
        is_connected = False
        return False


def get_connected():
    return is_connected


def get_account(user_id):
    """Map user_id to a Hardhat account (1-19)."""
    if not w3:
        raise Exception("Blockchain not connected")
    accounts = w3.eth.accounts
    index = ((user_id - 1) % 19) + 1
    return accounts[index] if index < len(accounts) else accounts[1]


def deposit_for_user(user_id, amount_eth):
    if not is_connected:
        raise Exception("Blockchain not connected")

    account = get_account(user_id)
    tx = contract.functions.deposit().build_transaction({
        'from': account,
        'value': w3.to_wei(amount_eth, 'ether'),
        'gas': 200000,
        'nonce': w3.eth.get_transaction_count(account),
    })

    tx_hash = w3.eth.send_transaction(tx)
    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)

    return {
        'tx_hash': receipt.transactionHash.hex(),
        'block_number': receipt.blockNumber,
    }


def pay_for_parking(user_id, plate_number):
    if not is_connected:
        raise Exception("Blockchain not connected")

    account = get_account(user_id)
    balance = contract.functions.getBalance(account).call()
    fee = contract.functions.parkingFee().call()

    if balance < fee:
        raise Exception(
            f"Insufficient crypto balance. Have: {w3.from_wei(balance, 'ether')} ETH, "
            f"Need: {w3.from_wei(fee, 'ether')} ETH"
        )

    tx = contract.functions.payForParking(plate_number).build_transaction({
        'from': account,
        'gas': 200000,
        'nonce': w3.eth.get_transaction_count(account),
    })

    tx_hash = w3.eth.send_transaction(tx)
    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)

    return {
        'tx_hash': receipt.transactionHash.hex(),
        'block_number': receipt.blockNumber,
        'fee': str(w3.from_wei(fee, 'ether')),
    }


def get_crypto_balance(user_id):
    if not is_connected:
        raise Exception("Blockchain not connected")

    account = get_account(user_id)
    balance = contract.functions.getBalance(account).call()
    return str(w3.from_wei(balance, 'ether'))


def get_wallet_address(user_id):
    if not is_connected:
        raise Exception("Blockchain not connected")
    return get_account(user_id)
