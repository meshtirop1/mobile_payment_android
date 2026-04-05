#!/usr/bin/env python
"""Full API test suite for Django backend."""
import requests
import os
import sys

BASE = 'http://localhost:8000/api'
pass_count = 0
fail_count = 0


def check(name, ok, detail=''):
    global pass_count, fail_count
    if ok:
        pass_count += 1
        print(f'  PASS {name}')
    else:
        fail_count += 1
        print(f'  FAIL {name} {detail}')


def post(path, data=None, token=None):
    h = {'Content-Type': 'application/json'}
    if token:
        h['Authorization'] = f'Bearer {token}'
    return requests.post(f'{BASE}{path}', json=data, headers=h, timeout=10)


def get(path, token=None):
    h = {}
    if token:
        h['Authorization'] = f'Bearer {token}'
    return requests.get(f'{BASE}{path}', headers=h, timeout=10)


def delete(path, token=None):
    h = {}
    if token:
        h['Authorization'] = f'Bearer {token}'
    return requests.delete(f'{BASE}{path}', headers=h, timeout=10)


# 1. Health
r = get('/health/')
check('Health check', r.status_code == 200)

# 2. Register
import time
r = post('/register/', {'name': 'Test', 'email': f'test{int(time.time())}@test.com', 'password': 'test123'})
check('Register', r.status_code == 201 and 'token' in r.json(), f'{r.status_code} {r.text[:200]}')

# 3. Login
r = post('/login/', {'email': 'john@example.com', 'password': 'password123'})
check('Login', r.status_code == 200 and 'token' in r.json(), f'{r.status_code} {r.text[:200]}')
token = r.json()['token']

# 4. Bad password
r = post('/login/', {'email': 'john@example.com', 'password': 'wrong'})
check('Bad password rejected', r.status_code == 401)

# 5. Get vehicles
r = get('/vehicles/', token)
check('Get vehicles', r.status_code == 200 and len(r.json()['vehicles']) >= 2, f'{r.status_code} {r.text[:200]}')

# 6. Add vehicle
r = post('/vehicles/', {'plate_number': 'NEW001'}, token)
check('Add vehicle', r.status_code == 201, f'{r.status_code} {r.text[:200]}')
vid = r.json().get('vehicle', {}).get('id')

# 7. Delete vehicle
if vid:
    r = delete(f'/vehicles/{vid}/', token)
    check('Delete vehicle', r.status_code == 200, f'{r.status_code} {r.text[:200]}')
else:
    check('Delete vehicle', False, 'no vehicle id')

# 8. Wallet balance
r = get('/wallet/', token)
check('Wallet balance', r.status_code == 200 and 'balance' in r.json())
bal_before = r.json()['balance']

# 9. Top up
r = post('/wallet/topup/', {'amount': 10}, token)
check('Top up wallet', r.status_code == 200 and r.json()['balance'] == bal_before + 10, f'{r.status_code} {r.text[:200]}')

# 10. Parking spots
r = get('/parking/spots/', token)
check('Parking spots (20)', r.status_code == 200 and r.json()['total'] == 20, f'{r.status_code} {r.text[:200]}')

# 11. Parking entry
r = post('/parking/entry/', {'plate_number': 'ABC1234', 'spot_number': 5}, token)
check('Parking entry', r.status_code == 200 and r.json()['gate'] == 'open', f'{r.status_code} {r.text[:200]}')

# 12. Active session
r = get('/parking/active-session/', token)
check('Active session', r.status_code == 200 and r.json()['has_session'] == True, f'{r.status_code} {r.text[:200]}')

# 13. Parking exit (free under 30 min)
r = post('/parking/exit/', {'plate_number': 'ABC1234'}, token)
check('Parking exit (free <30min)', r.status_code == 200 and r.json()['fee_charged'] == 0, f'{r.status_code} {r.text[:200]}')

# 14. Duplicate entry blocked
r = post('/parking/entry/', {'plate_number': 'ABC1234', 'spot_number': 5}, token)
check('Duplicate entry blocked', r.status_code == 429, f'{r.status_code} {r.text[:200]}')

# 15. Unregistered plate
r = post('/parking/entry/', {'plate_number': 'FAKE999'}, token)
check('Unregistered plate rejected', r.status_code == 404)

# 16. Transaction history
r = get('/parking/transactions/history/', token)
check('Transaction history', r.status_code == 200 and len(r.json()['transactions']) > 0)

# 17. Notifications list
r = get('/notifications/', token)
check('Notifications list', r.status_code == 200)

# 18. Unread count
r = get('/notifications/unread-count/', token)
check('Unread count', r.status_code == 200 and 'count' in r.json())

# 19. Mark all read
r = post('/notifications/read-all/', {}, token)
check('Mark all read', r.status_code == 200)

# 20. Reservations list
r = get('/reservations/', token)
check('Reservations list', r.status_code == 200)

# 21. Create reservation
r = post('/reservations/', {'spot_number': 10, 'vehicle_id': 1}, token)
check('Create reservation', r.status_code == 201, f'{r.status_code} {r.text[:200]}')
rid = r.json().get('reservation', {}).get('id')

# 22. Cancel reservation
if rid:
    r = delete(f'/reservations/{rid}/', token)
    check('Cancel reservation', r.status_code == 200, f'{r.status_code} {r.text[:200]}')
else:
    check('Cancel reservation', False, 'no reservation id')

# 23. IoT gate status
r = get('/iot/gate-status/', token)
check('IoT gate status', r.status_code == 200)

# 24. Sensor update (no auth needed)
r = post('/iot/sensor-update/', {'spot_number': 1, 'distance_cm': 5.0})
check('Sensor update', r.status_code == 200 and r.json()['is_occupied'] == True, f'{r.status_code} {r.text[:200]}')

# 25. Blockchain status
r = get('/crypto/status/', token)
check('Blockchain status', r.status_code == 200)

# 26. No auth rejected
r = get('/wallet/')
check('No auth rejected', r.status_code in [401, 403])

# 27. Negative topup
r = post('/wallet/topup/', {'amount': -5}, token)
check('Negative topup rejected', r.status_code == 400)

# 28. Admin - parked vehicles (john is admin)
r = get('/admin-dashboard/parked-vehicles/', token)
check('Admin parked vehicles', r.status_code == 200, f'{r.status_code} {r.text[:200]}')

# 29. Admin - revenue
r = get('/admin-dashboard/revenue/', token)
check('Admin revenue', r.status_code == 200, f'{r.status_code} {r.text[:200]}')

# 30. Admin - spots overview
r = get('/admin-dashboard/spots-overview/', token)
check('Admin spots overview', r.status_code == 200, f'{r.status_code} {r.text[:200]}')

# 31. Non-admin rejected
r = post('/login/', {'email': 'jane@example.com', 'password': 'password123'})
jane_token = r.json()['token']
r = get('/admin-dashboard/parked-vehicles/', jane_token)
check('Non-admin rejected', r.status_code == 403)

# 32. Duplicate email rejected
r = post('/register/', {'name': 'John', 'email': 'john@example.com', 'password': 'test123'})
check('Duplicate email rejected', r.status_code == 400)

print(f'\n=========================')
print(f'  Results: {pass_count} passed, {fail_count} failed out of {pass_count + fail_count}')
print(f'=========================')
