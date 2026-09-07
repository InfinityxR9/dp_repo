"""Map Arduino events to amplifier selection and pressure-controlled audio."""

from threading import Lock


class SensorManager:
    def __init__(self, amplifier_manager, audio_manager, switch_mute_ms=12):
        self._lock = Lock()
        self.amplifier_manager = amplifier_manager
        self.audio_manager = audio_manager
        self.switch_mute_ms = max(0, int(switch_mute_ms))

        self.active_sensor = None
        self.pressure = 0

    def activation(self, sensor, pressure):
        sensor = int(sensor)
        pressure = max(0, min(100, int(pressure)))
        if not 1 <= sensor <= 7:
            return

        with self._lock:
            old_sensor = self.active_sensor
            self.active_sensor = sensor
            self.pressure = pressure

        # Mute before changing physical amplifier to avoid a hard output jump.
        if old_sensor is not None and old_sensor != sensor:
            self.audio_manager.ramp_to_zero(self.switch_mute_ms)

        selected = self.amplifier_manager.select(sensor)
        self.audio_manager.set_pressure(pressure)

        state = "configured" if selected else "NOT CONFIGURED"
        print(
            f"[ACTIVE] sensor={sensor} pressure={pressure}% amp={state}"
        )

    def pressure_update(self, sensor, pressure):
        sensor = int(sensor)
        pressure = max(0, min(100, int(pressure)))

        with self._lock:
            if sensor != self.active_sensor:
                return
            self.pressure = pressure

        self.audio_manager.set_pressure(pressure)
        print(f"[PRESSURE] sensor={sensor} pressure={pressure}%")

    def stop(self):
        with self._lock:
            self.active_sensor = None
            self.pressure = 0

        self.audio_manager.set_pressure(0)
        self.amplifier_manager.all_off()

    def get_state(self):
        with self._lock:
            return {
                "activeSensor": self.active_sensor,
                "pressure": self.pressure,
            }
