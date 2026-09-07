"""Control the seven MAX98357A SD_MODE pins.

SD_MODE HIGH = amplifier enabled.
SD_MODE LOW  = amplifier shutdown.

The I2S audio bus itself is shared by all seven amplifiers; only one SD_MODE
pin is enabled at a time during normal operation.
"""

from threading import RLock

from gpiozero import OutputDevice


class AmplifierManager:
    def __init__(self, pin_map):
        self._lock = RLock()
        self.amps = {}

        for location, gpio in pin_map.items():
            if gpio is None:
                continue

            location = int(location)
            gpio = int(gpio)
            if not 1 <= location <= 7:
                raise ValueError(f"Invalid amplifier location: {location}")

            self.amps[location] = OutputDevice(
                gpio,
                active_high=True,
                initial_value=False,
            )

        self.all_off()

    def all_off(self):
        with self._lock:
            for amp in self.amps.values():
                amp.off()

    def select(self, location):
        """Disable every amplifier, then enable exactly one."""
        location = int(location)
        with self._lock:
            self.all_off()
            amp = self.amps.get(location)
            if amp is None:
                raise ValueError(
                    f"No SD_MODE GPIO configured for amplifier {location}"
                )
            amp.on()

    def get_active_location(self):
        with self._lock:
            for location, amp in self.amps.items():
                if amp.value:
                    return location
        return None

    def close(self):
        with self._lock:
            self.all_off()
            for amp in self.amps.values():
                amp.close()
            self.amps.clear()
