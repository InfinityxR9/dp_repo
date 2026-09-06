"""Bridges Arduino sensor events to amplifier selection and audio volume."""

from threading import Lock


class SensorManager:
    def __init__(self, amplifier_manager, audio_manager):
        self._lock = Lock()
        self.amplifier_manager = amplifier_manager
        self.audio_manager = audio_manager
        self.active_sensor = None
        self.pressure = 0

    def activation(self, sensor, pressure):
        sensor = int(sensor)
        pressure = max(0, min(100, int(pressure)))

        with self._lock:
            old_sensor = self.active_sensor
            self.active_sensor = sensor
            self.pressure = pressure

        # Audio is already looping, so this only changes the active transducer.
        # A future pop-reduction fade can be inserted here if needed.
        self.amplifier_manager.select(sensor)
        self.audio_manager.set_pressure(pressure)

        if old_sensor != sensor:
            print(f"[ACTIVE] sensor={sensor} pressure={pressure}%")

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
