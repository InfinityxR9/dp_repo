"""Raspberry Pi configuration for the lung auscultation trainer."""

from pathlib import Path

# -----------------------------------------------------------------------------
# Arduino serial
# -----------------------------------------------------------------------------
SERIAL_PORT = "/dev/ttyACM0"
BAUD_RATE = 115200
SERIAL_TIMEOUT = 0.02

# -----------------------------------------------------------------------------
# MAX98357A SD_MODE GPIOs
# -----------------------------------------------------------------------------
# BCM GPIO numbers. These are a recommended starting allocation for a Pi 4.
# Change them here if your final physical wiring differs.
# IMPORTANT: SD_MODE must NOT remain connected directly to 3.3 V once you use
# individual amplifier selection.
AMP_SD_PINS = {
    1: 4,
    2: 5,
    3: 6,
    4: 12,
    5: 13,
    6: 16,
    7: 17,
}

# The I2S pins remain shared by every MAX98357A:
#   BCLK  -> BCM GPIO18
#   LRCLK -> BCM GPIO19
#   DIN   -> BCM GPIO21
# Do not assign those pins to SD_MODE.

# -----------------------------------------------------------------------------
# Audio
# -----------------------------------------------------------------------------
AUDIO_SAMPLE_RATE = 48000
AUDIO_BUFFER = 256
AUDIO_UPDATE_INTERVAL = 0.005  # 5 ms

# Global ceiling for physical speaker loudness.
# 0.30 is deliberately conservative for the stethoscope-oriented trainer.
MAX_AUDIO_VOLUME = 0.30
VOLUME_SMOOTHING = 0.25

# Pygame/SDL should use the MAX98357A ALSA device explicitly by name so card
# numbering changes do not redirect audio to HDMI/headphones.
ALSA_DEVICE = "plughw:CARD=MAX98357A,DEV=0"

# -----------------------------------------------------------------------------
# Audio library
# -----------------------------------------------------------------------------
AUDIO_DIR = Path(__file__).resolve().parent / "audio"

# These are the canonical names to create after converting your source MPEG/MP3
# files to 48 kHz, 16-bit PCM WAV files.
AUDIO_FILES = {
    "bronchial": AUDIO_DIR / "bronchial.wav",
    "crackle": AUDIO_DIR / "crackle.wav",
    "pleural_rub": AUDIO_DIR / "pleural_rub.wav",
    "ronchi": AUDIO_DIR / "ronchi.wav",
    "stridor": AUDIO_DIR / "stridor.wav",
    "vesicular": AUDIO_DIR / "vesicular.wav",
    "wheeze": AUDIO_DIR / "wheeze.wav",
}

DEFAULT_SOUND_ID = "vesicular"

# -----------------------------------------------------------------------------
# API
# -----------------------------------------------------------------------------
API_HOST = "0.0.0.0"
API_PORT = 8000
