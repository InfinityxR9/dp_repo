import os
import time
from pathlib import Path
from threading import Lock
from typing import Optional

# Use ALSA, with ~/.asoundrc routing "default" to the MAX98357A.
os.environ["SDL_AUDIODRIVER"] = "alsa"

import pygame


class AudioManager:
    def __init__(
        self,
        sample_rate=48000,
        channels=2,
        buffer_size=256,
        stethoscope_max=0.05,
        test_max=0.20,
        smoothing=0.25,
        default_mode="stethoscope",
        default_multiplier=1.0,
    ):
        self._lock = Lock()

        self.stethoscope_max = float(stethoscope_max)
        self.test_max = float(test_max)
        self.smoothing = max(0.01, min(1.0, float(smoothing)))

        self.mode = str(default_mode).strip().lower()
        if self.mode not in {"stethoscope", "test"}:
            self.mode = "stethoscope"

        self.master_multiplier = max(
            0.0,
            min(1.0, float(default_multiplier)),
        )

        self._last_pressure = 0

        self.current_volume = 0.0
        self.target_volume = 0.0

        self.current_sound: Optional[str] = None
        self.playing = False

        pygame.mixer.pre_init(
            frequency=int(sample_rate),
            size=-16,
            channels=int(channels),
            buffer=int(buffer_size),
        )

        try:
            pygame.mixer.init(
                frequency=int(sample_rate),
                size=-16,
                channels=int(channels),
                buffer=int(buffer_size),
            )
        except pygame.error as exc:
            raise RuntimeError(
                f"Could not initialize pygame audio: {exc}"
            ) from exc

        pygame.mixer.music.set_volume(0.0)

        print(
            f"[AUDIO] Mixer initialized: "
            f"{pygame.mixer.get_init()}"
        )

    def _mode_ceiling_locked(self):
        if self.mode == "test":
            return self.test_max

        return self.stethoscope_max

    def _calculate_target_locked(self):
        pressure_fraction = self._last_pressure / 100.0

        return (
            pressure_fraction
            * self._mode_ceiling_locked()
            * self.master_multiplier
        )

    def _recalculate_target_locked(self):
        self.target_volume = self._calculate_target_locked()

    def play(self, path):
        path = Path(path).resolve()

        if not path.is_file():
            raise FileNotFoundError(
                f"Audio file not found: {path}"
            )

        with self._lock:
            pygame.mixer.music.stop()
            pygame.mixer.music.load(str(path))

            # Always begin silently.
            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.play(-1)

            self.current_volume = 0.0
            self.target_volume = self._calculate_target_locked()

            self.current_sound = str(path)
            self.playing = True

    def stop(self):
        with self._lock:
            self.current_volume = 0.0
            self.target_volume = 0.0

            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.stop()

            self.current_sound = None
            self.playing = False

    def set_mode(self, mode: str):
        mode = str(mode).strip().lower()

        if mode not in {"stethoscope", "test"}:
            raise ValueError(
                "mode must be 'stethoscope' or 'test'"
            )

        with self._lock:
            self.mode = mode
            self._recalculate_target_locked()

    def set_multiplier(self, multiplier: float):
        value = float(multiplier)

        # Accept 0..1 or 0..100.
        if value > 1.0:
            value /= 100.0

        value = max(0.0, min(1.0, value))

        with self._lock:
            self.master_multiplier = value
            self._recalculate_target_locked()

    def set_pressure(self, pressure: int):
        with self._lock:
            self._last_pressure = max(
                0,
                min(100, int(pressure)),
            )

            self._recalculate_target_locked()

    def ramp_to_zero(self, duration_ms: int = 12):
        duration_ms = max(0, int(duration_ms))

        if duration_ms == 0:
            with self._lock:
                self.current_volume = 0.0
                self.target_volume = 0.0
                pygame.mixer.music.set_volume(0.0)
            return

        steps = max(1, duration_ms // 2)
        delay = duration_ms / steps / 1000.0

        for _ in range(steps):
            with self._lock:
                self.current_volume *= 0.5

                if self.current_volume < 0.00005:
                    self.current_volume = 0.0

                pygame.mixer.music.set_volume(
                    self.current_volume
                )

            time.sleep(delay)

        with self._lock:
            self.current_volume = 0.0
            self.target_volume = 0.0
            pygame.mixer.music.set_volume(0.0)

    def update(self):
        with self._lock:
            delta = (
                self.target_volume
                - self.current_volume
            )

            self.current_volume += (
                delta * self.smoothing
            )

            pygame.mixer.music.set_volume(
                self.current_volume
            )

    def get_state(self):
        with self._lock:
            return {
                "mode": self.mode,
                "masterMultiplier": round(
                    self.master_multiplier,
                    4,
                ),
                "pressure": self._last_pressure,
                "currentVolume": round(
                    self.current_volume,
                    5,
                ),
                "targetVolume": round(
                    self.target_volume,
                    5,
                ),
                "modeCeiling": round(
                    self._mode_ceiling_locked(),
                    4,
                ),
                "playing": self.playing,
                "sound": self.current_sound,
            }

    def close(self):
        try:
            with self._lock:
                pygame.mixer.music.stop()
                pygame.mixer.quit()
        except pygame.error:
            pass
