"""Controls the SD_MODE pins of the seven MAX98357A amplifiers."""

from threading import Lock

from gpiozero import OutputDevice


class AmplifierManager:
    def __init__(self, pin_map):
        self._lock = Lock()
        self.amps = {}

        for location, gpio in pin_map.items():
            if gpio is None:
                continue

            self.amps[int(location)] = OutputDevice(
                int(gpio),
                active_high=True,
                initial_value=False,
            )

        self.all_off()

    def all_off(self):
        with self._lock:
            for amp in self.amps.values():
                amp.off()

    def select(self, location):
        """Turn every amp off, then enable the requested amp."""
        with self._lock:
            for amp in self.amps.values():
                amp.off()

            amp = self.amps.get(int(location))
            if amp is not None:
                amp.on()

    def close(self):
        with self._lock:
            for amp in self.amps.values():
                amp.off()
                amp.close()
            self.amps.clear()
