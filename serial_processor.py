"""Parses the Arduino serial protocol."""


def process_serial_line(line, sensor_manager):
    parts = line.strip().split(",")

    # Arduino also sends: READY
    if parts == ["READY"]:
        print("Arduino reports READY.")
        return

    if len(parts) != 3:
        return

    message_type = parts[0].strip()

    try:
        sensor = int(parts[1])
        pressure = int(parts[2])
    except ValueError:
        return

    if not 1 <= sensor <= 7:
        return

    pressure = max(0, min(100, pressure))

    if message_type == "A":
        sensor_manager.activation(sensor, pressure)
    elif message_type == "P":
        sensor_manager.pressure_update(sensor, pressure)
