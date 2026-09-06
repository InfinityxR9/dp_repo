"""Continuous audio playback with pressure-controlled volume."""

from threading import Lock

import pygame


class AudioManager:
    def __init__(self, sample_rate=48000, buffer_size=256,
                 max_volume=0.30, smoothing=0.20):
        self._lock = Lock()
        self.max_volume = float(max_volume)
        self.smoothing = float(smoothing)
        self.current_volume = 0.0
        self.target_volume = 0.0
        self.current_sound = None

        pygame.mixer.pre_init(
            frequency=sample_rate,
            size=-16,
            channels=2,
            buffer=buffer_size,
        )
        pygame.mixer.init()
        pygame.mixer.music.set_volume(0.0)

    def play(self, path):
        """Load and loop a sound, but start it silently."""
        path = str(path)
        with self._lock:
            pygame.mixer.music.stop()
            pygame.mixer.music.load(path)
            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.play(-1)
            self.current_volume = 0.0
            self.target_volume = 0.0
            self.current_sound = path

    def stop(self):
        with self._lock:
            self.target_volume = 0.0
            self.current_volume = 0.0
            pygame.mixer.music.set_volume(0.0)
            pygame.mixer.music.stop()
            self.current_sound = None

    def pressure_to_volume(self, pressure):
        """Map Arduino pressure (0..100) to conservative mixer volume."""
        pressure = max(0, min(100, int(pressure)))
        x = pressure / 100.0

        # Slightly less aggressive than x^2 so light/medium pressure remains audible.
        curved = x ** 1.5
        return curved * self.max_volume

    def set_pressure(self, pressure):
        with self._lock:
            self.target_volume = self.pressure_to_volume(pressure)

    def update(self):
        """Smoothly approach the target volume. Called periodically by main.py."""
        with self._lock:
            delta = self.target_volume - self.current_volume
            self.current_volume += delta * self.smoothing
            pygame.mixer.music.set_volume(self.current_volume)

    def get_volume(self):
        with self._lock:
            return self.current_volume

    def close(self):
        with self._lock:
            pygame.mixer.music.stop()
            pygame.mixer.quit()
