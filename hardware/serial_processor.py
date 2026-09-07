"""Parse the Arduino serial protocol."""


def process_serial_line(line, sensor_manager):
    line = line.strip()
    if not line:
        return

    if line == "READY":
        print("[ARDUINO] READY")
        return

    parts = line.split(",")
    if len(parts) != 3:
        print(f"[SERIAL] Ignoring malformed line: {line}")
        return

    message_type = parts[0].strip()

    try:
        sensor = int(parts[1])
        pressure = int(parts[2])
    except ValueError:
        print(f"[SERIAL] Ignoring invalid line: {line}")
        return

    if not 1 <= sensor <= 7:
        return

    pressure = max(0, min(100, pressure))

    if message_type == "A":
        sensor_manager.activation(sensor, pressure)
    elif message_type == "P":
        sensor_manager.pressure_update(sensor, pressure)
