import time
from pathlib import Path
from threading import RLock

import pygame


class AudioManager:

    def __init__(
        self,
        sample_rate=48000,
        buffer_size=256,
        max_volume=0.30,
        smoothing=0.25,
    ):
        self._lock = RLock()

        # Hardware/system maximum volume.
        self.max_volume = float(max_volume)

        # Android-controlled master volume (0.0 to 1.0).
        self.master_volume = 1.0

        self.smoothing = float(smoothing)

        self.current_volume = 0.0
        self.target_volume = 0.0

        self.pressure = 0
        self.current_sound = None

        pygame.mixer.pre_init(
            frequency=sample_rate,
            size=-16,
            channels=2,
            buffer=buffer_size,
        )

        pygame.init()
        pygame.mixer.init()

        pygame.mixer.music.set_volume(0.0)

    @property
    def is_playing(self):
        return bool(pygame.mixer.music.get_busy())

    def play(self, path):
        """Load and continuously loop a sound, starting silently."""

        path = Path(path).resolve()

        if not path.is_file():
            raise FileNotFoundError(path)

        with self._lock:
            pygame.mixer.music.stop()

            pygame.mixer.music.load(str(path))
            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.play(-1)

            self.current_volume = 0.0
            self.target_volume = 0.0

            self.pressure = 0
            self.current_sound = str(path)

    def stop(self):
        with self._lock:
            self.target_volume = 0.0
            self.current_volume = 0.0
            self.pressure = 0

            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.stop()

            self.current_sound = None

    def pressure_to_volume(self, pressure):
        """
        Convert pressure to audio volume.

        Arduino pressure is normalized from 0 to 100.
        Master volume from Android is applied on top.
        """

        pressure = max(0, min(100, int(pressure)))

        normalized_pressure = pressure / 100.0

        return (
            normalized_pressure
            * self.max_volume
            * self.master_volume
        )

    def set_pressure(self, pressure):
        with self._lock:
            self.pressure = max(
                0,
                min(100, int(pressure))
            )

            self.target_volume = self.pressure_to_volume(
                self.pressure
            )

    def set_master_volume(self, volume):
        """
        Set Android-controlled master volume.

        volume must be between 0.0 and 1.0.
        """

        with self._lock:
            self.master_volume = max(
                0.0,
                min(1.0, float(volume))
            )

            # Immediately recalculate based on current pressure.
            self.target_volume = self.pressure_to_volume(
                self.pressure
            )

    def get_master_volume(self):
        with self._lock:
            return self.master_volume

    def fade_out(self, duration=0.015):
        """Fade to zero before switching amplifiers/sounds."""

        duration = max(0.0, float(duration))

        if duration == 0:
            with self._lock:
                self.current_volume = 0.0
                self.target_volume = 0.0
                pygame.mixer.music.set_volume(0.0)

            return

        steps = max(
            1,
            int(duration / 0.005)
        )

        with self._lock:
            start = self.current_volume
            self.target_volume = 0.0

        for step in range(1, steps + 1):

            value = start * (
                1.0 - step / steps
            )

            with self._lock:
                self.current_volume = max(
                    0.0,
                    value
                )

                pygame.mixer.music.set_volume(
                    self.current_volume
                )

            time.sleep(
                duration / steps
            )

    def update(self):
        with self._lock:

            if not self.is_playing:
                return

            delta = (
                self.target_volume
                - self.current_volume
            )

            self.current_volume += (
                delta * self.smoothing
            )

            if (
                abs(
                    self.target_volume
                    - self.current_volume
                )
                < 0.001
            ):
                self.current_volume = (
                    self.target_volume
                )

            pygame.mixer.music.set_volume(
                self.current_volume
            )

    def get_volume(self):
        with self._lock:
            return self.current_volume

    def get_state(self):
        with self._lock:
            return {
                "playing": self.is_playing,
                "sound": (
                    Path(self.current_sound).stem
                    if self.current_sound
                    else None
                ),
                "pressure": self.pressure,
                "volume": round(
                    self.current_volume,
                    4
                ),
                "targetVolume": round(
                    self.target_volume,
                    4
                ),
                "masterVolume": round(
                    self.master_volume,
                    4
                ),
            }

    def close(self):
        with self._lock:
            pygame.mixer.music.stop()
            pygame.mixer.quit()