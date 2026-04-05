import logging
from django.conf import settings

logger = logging.getLogger(__name__)

serial_connection = None


def get_serial():
    global serial_connection
    if not settings.SERIAL_ENABLED:
        return None
    if serial_connection is None:
        try:
            import serial
            serial_connection = serial.Serial(
                settings.SERIAL_PORT,
                settings.SERIAL_BAUD_RATE,
                timeout=1
            )
            logger.info(f"Serial connected on {settings.SERIAL_PORT}")
        except Exception as e:
            logger.warning(f"Serial not available: {e}")
            return None
    return serial_connection


def send_gate_command(action, plate_number='', detail=''):
    """Send command to Arduino gate. Gracefully skips if serial not available."""
    # Log the command regardless
    try:
        from .models import GateLog
        GateLog.objects.create(
            action=action, plate_number=plate_number, detail=detail
        )
    except Exception:
        pass

    conn = get_serial()
    if conn is None:
        logger.debug(f"Gate command (no serial): {action}:{plate_number}:{detail}")
        return False

    try:
        cmd = f"{action}\n"
        conn.write(cmd.encode())

        # Send LCD update
        line1 = plate_number[:16] if plate_number else ''
        line2 = detail[:16] if detail else ''
        if line1 or line2:
            lcd_cmd = f"LCD:{line1}|{line2}\n"
            conn.write(lcd_cmd.encode())

        return True
    except Exception as e:
        logger.error(f"Serial write error: {e}")
        return False


def read_sensor_data():
    """Read sensor data from Arduino. Returns dict or None."""
    conn = get_serial()
    if conn is None:
        return None

    try:
        if conn.in_waiting > 0:
            line = conn.readline().decode().strip()
            if line.startswith('SPOT:'):
                return {'type': 'spot', 'value': line.split(':')[1]}
            elif line.startswith('ULTRA:'):
                return {'type': 'distance', 'value': float(line.split(':')[1])}
            elif line.startswith('IR:'):
                return {'type': 'ir', 'value': line.split(':')[1]}
    except Exception as e:
        logger.error(f"Serial read error: {e}")
    return None
