"""Controls the seven MAX98357A SD_MODE GPIOs."""

from threading import Lock

from gpiozero import OutputDevice


class AmplifierManager:
    def __init__(self, pin_map, active_high=True):
        self._lock = Lock()
        self.amps = {}
        self.configured = {}

        for location in range(1, 8):
            gpio = pin_map.get(location)
            self.configured[location] = gpio is not None

            if gpio is None:
                continue

            self.amps[location] = OutputDevice(
                int(gpio),
                active_high=active_high,
                initial_value=False,
            )

        self.all_off()
        print(
            "[AMP] configured locations: "
            + str(self.get_configured_locations())
        )

    def all_off(self):
        with self._lock:
            for amp in self.amps.values():
                amp.off()

    def select(self, location):
        """Enable exactly one configured amplifier."""
        location = int(location)
        if not 1 <= location <= 7:
            raise ValueError("location must be 1..7")

        with self._lock:
            for amp in self.amps.values():
                amp.off()

            amp = self.amps.get(location)
            if amp is None:
                print(
                    f"[AMP] WARNING: location {location} has no SD GPIO configured"
                )
                return False

            amp.on()
            print(f"[AMP] location {location} ON; all others OFF")
            return True

    def is_configured(self, location):
        return bool(self.configured.get(int(location), False))

    def get_configured_locations(self):
        return [
            loc for loc in range(1, 8)
            if self.configured.get(loc, False)
        ]

    def close(self):
        with self._lock:
            for amp in self.amps.values():
                amp.off()
                amp.close()
            self.amps.clear()
