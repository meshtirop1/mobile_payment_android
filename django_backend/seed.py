#!/usr/bin/env python
"""Seed the database with sample data."""
import os
import sys
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'smart_parking.settings')

# Setup Django
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
django.setup()

from accounts.models import User
from vehicles.models import Vehicle
from parking.models import ParkingSpot, Transaction


def seed():
    print("Seeding database with sample data...\n")

    # Create parking spots (20 spots)
    for i in range(1, 21):
        spot, created = ParkingSpot.objects.get_or_create(
            spot_number=i,
            defaults={
                'floor': 'G' if i <= 10 else '1',
                'spot_type': 'handicap' if i in [1, 11] else ('ev' if i in [2, 12] else 'regular'),
            }
        )
        if created:
            print(f"  Created spot {i} ({spot.spot_type}, floor {spot.floor})")

    # Create test users
    users_data = [
        {'email': 'john@example.com', 'name': 'John Doe', 'password': 'password123', 'balance': 50.00, 'is_admin': True},
        {'email': 'jane@example.com', 'name': 'Jane Smith', 'password': 'password123', 'balance': 10.00, 'is_admin': False},
        {'email': 'bob@example.com', 'name': 'Bob Wilson', 'password': 'password123', 'balance': 1.00, 'is_admin': False},
    ]

    for u in users_data:
        user, created = User.objects.get_or_create(
            email=u['email'],
            defaults={
                'username': u['email'],
                'first_name': u['name'],
                'balance': u['balance'],
                'is_parking_admin': u['is_admin'],
            }
        )
        if created:
            user.set_password(u['password'])
            user.save()
            print(f"  Created user: {u['email']} (balance: ${u['balance']:.2f}, admin: {u['is_admin']})")

    # Create vehicles
    vehicles_data = [
        ('john@example.com', ['ABC1234', 'XYZ5678']),
        ('jane@example.com', ['DEF9012']),
        ('bob@example.com', ['LOW0001']),
    ]

    for email, plates in vehicles_data:
        user = User.objects.get(email=email)
        for plate in plates:
            v, created = Vehicle.objects.get_or_create(
                plate_number=plate, defaults={'user': user}
            )
            if created:
                print(f"  Created vehicle: {plate} -> {email}")

    print("\nSeed complete!")
    print("\nTest accounts:")
    print("  john@example.com / password123  (balance: $50.00, admin, vehicles: ABC1234, XYZ5678)")
    print("  jane@example.com / password123  (balance: $10.00, vehicles: DEF9012)")
    print("  bob@example.com  / password123  (balance: $1.00,  vehicles: LOW0001)")
    print(f"\n  Parking spots: {ParkingSpot.objects.count()} (all available)")


if __name__ == '__main__':
    seed()
