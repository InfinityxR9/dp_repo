"""Map Arduino sensor events to one active amplifier and audio volume."""

from threading import RLock


class SensorManager:
    def __init__(self, amplifier_manager, audio_manager):
        self.amplifier_manager = amplifier_manager
        self.audio_manager = audio_manager
        self._lock = RLock()
        self.active_sensor = None
        self.pressure = 0

    def activation(self, sensor, pressure):
        sensor = int(sensor)
        pressure = max(0, min(100, int(pressure)))

        with self._lock:
            old_sensor = self.active_sensor
            self.active_sensor = sensor
            self.pressure = pressure

        if old_sensor != sensor:
            # Avoid an abrupt I2S output change while changing the active amp.
            self.audio_manager.fade_out(0.015)

        self.amplifier_manager.select(sensor)
        self.audio_manager.set_pressure(pressure)

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
