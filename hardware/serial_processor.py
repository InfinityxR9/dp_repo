"""Strict parser for the Arduino serial protocol."""


def process_serial_line(line, sensor_manager):
    line = line.strip()
    if not line:
        return

    if line == "READY":
        print("[ARDUINO] READY")
        return

    parts = line.split(",")
    if len(parts) != 3:
        print(f"[SERIAL] ignored malformed line: {line}")
        return

    message_type = parts[0].strip().upper()

    try:
        sensor = int(parts[1])
        pressure = int(parts[2])
    except ValueError:
        print(f"[SERIAL] ignored non-numeric event: {line}")
        return

    if message_type not in {"A", "P"}:
        print(f"[SERIAL] ignored invalid event type: {line}")
        return

    if not 1 <= sensor <= 7:
        print(f"[SERIAL] ignored invalid sensor: {line}")
        return

    pressure = max(0, min(100, pressure))

    if message_type == "A":
        sensor_manager.activation(sensor, pressure)
    else:
        sensor_manager.pressure_update(sensor, pressure)
