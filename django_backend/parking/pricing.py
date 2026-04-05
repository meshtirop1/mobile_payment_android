from decimal import Decimal


def calculate_parking_fee(duration_minutes):
    """Calculate parking fee based on duration tiers."""
    if duration_minutes <= 30:
        return Decimal('0.00')
    elif duration_minutes <= 120:
        return Decimal('2.00')
    elif duration_minutes <= 300:
        return Decimal('5.00')
    else:
        return Decimal('10.00')
