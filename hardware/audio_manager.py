"""Continuous WAV playback with pressure + master-volume control.

Audio level model:
    final_level = (pressure / 100) * mode_ceiling * master_multiplier

The pressure comes from the Arduino. The Android slider is only a multiplier;
it does not replace or distort the pressure relationship.
"""

import os
import time
from pathlib import Path
from threading import Lock
from typing import Optional

# Prefer the ALSA backend so the Raspberry Pi does not accidentally use HDMI.
os.environ.setdefault("SDL_AUDIODRIVER", "alsa")

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
        device_name="auto",
        default_mode="stethoscope",
        default_multiplier=1.0,
    ):
        self._lock = Lock()
        self.stethoscope_max = float(stethoscope_max)
        self.test_max = float(test_max)
        self.smoothing = max(0.01, min(1.0, float(smoothing)))

        self.mode = "stethoscope"
        self.master_multiplier = max(0.0, min(1.0, float(default_multiplier)))
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

        # Initialise SDL first so device enumeration is available on pygame builds
        # that expose pygame._sdl2.audio.
        pygame.init()

        resolved = self._resolve_device_name(device_name)

        try:
            pygame.mixer.quit()
        except pygame.error:
            pass

        if resolved:
            pygame.mixer.init(
                frequency=int(sample_rate),
                size=-16,
                channels=int(channels),
                buffer=int(buffer_size),
                devicename=resolved,
            )
        else:
            pygame.mixer.init(
                frequency=int(sample_rate),
                size=-16,
                channels=int(channels),
                buffer=int(buffer_size),
            )

        pygame.mixer.music.set_volume(0.0)
        self.set_mode(default_mode)
        print(f"[AUDIO] output device: {resolved or 'SDL default'}")

    @staticmethod
    def _resolve_device_name(requested: str):
        if requested and requested.lower() != "auto":
            return requested

        try:
            from pygame._sdl2.audio import get_audio_device_names

            names = list(get_audio_device_names(False))
            print("[AUDIO] SDL playback devices:")
            for name in names:
                print(f"  - {name}")

            for name in names:
                if "max98357a" in name.lower():
                    return name

            raise RuntimeError(
                "MAX98357A was not found in SDL playback devices. "
                "Set AUDIO_DEVICE in config.py to the exact device name printed above."
            )
        except ImportError as exc:
            raise RuntimeError(
                "This pygame build does not expose pygame._sdl2.audio. "
                "Set AUDIO_DEVICE in config.py to the exact SDL playback name."
            ) from exc

    def _mode_ceiling_locked(self):
        return self.test_max if self.mode == "test" else self.stethoscope_max

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
        """Load and continuously loop a WAV file starting at zero volume."""
        path = Path(path).resolve()
        if not path.is_file():
            raise FileNotFoundError(f"Audio file not found: {path}")

        with self._lock:
            pygame.mixer.music.stop()
            pygame.mixer.music.load(str(path))
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
            raise ValueError("mode must be 'stethoscope' or 'test'")
        with self._lock:
            self.mode = mode
            self._recalculate_target_locked()

    def set_multiplier(self, multiplier: float):
        value = float(multiplier)
        # Accept both Android-style 0..1 and UI-style 0..100 input.
        if value > 1.0:
            value /= 100.0
        value = max(0.0, min(1.0, value))
        with self._lock:
            self.master_multiplier = value
            self._recalculate_target_locked()

    def set_pressure(self, pressure: int):
        with self._lock:
            self._last_pressure = max(0, min(100, int(pressure)))
            self._recalculate_target_locked()

    def ramp_to_zero(self, duration_ms: int = 12):
        """Mute quickly before an amplifier/location switch."""
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
                pygame.mixer.music.set_volume(self.current_volume)
            time.sleep(delay)

        with self._lock:
            self.current_volume = 0.0
            self.target_volume = 0.0
            pygame.mixer.music.set_volume(0.0)

    def update(self):
        with self._lock:
            delta = self.target_volume - self.current_volume
            self.current_volume += delta * self.smoothing
            pygame.mixer.music.set_volume(self.current_volume)

    def get_state(self):
        with self._lock:
            return {
                "mode": self.mode,
                "masterMultiplier": round(self.master_multiplier, 4),
                "pressure": self._last_pressure,
                "currentVolume": round(self.current_volume, 5),
                "targetVolume": round(self.target_volume, 5),
                "modeCeiling": round(self._mode_ceiling_locked(), 4),
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
