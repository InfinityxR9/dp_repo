"""Raspberry Pi configuration for the lung auscultation trainer."""

from pathlib import Path

# -----------------------------------------------------------------------------
# Serial / Arduino
# -----------------------------------------------------------------------------
SERIAL_PORT = "/dev/ttyACM0"       # Change if the Mega appears as another device.
BAUD_RATE = 115200
SERIAL_TIMEOUT = 0.02

# -----------------------------------------------------------------------------
# MAX98357A SD_MODE GPIOs
# -----------------------------------------------------------------------------
# PLACEHOLDERS: fill these in only after you decide the final Pi GPIO allocation.
# The numbers below are intentionally NOT chosen for you.
AMP_SD_PINS = {
    1: None,
    2: None,
    3: None,
    4: None,
    5: None,
    6: None,
    7: None,
}

# -----------------------------------------------------------------------------
# Audio
# -----------------------------------------------------------------------------
AUDIO_SAMPLE_RATE = 48000
AUDIO_BUFFER = 256

# Keep the physical room volume conservative.  Tune this on the real system.
MAX_AUDIO_VOLUME = 0.30

# Faster response = larger value.  This is applied every ~5 ms.
VOLUME_SMOOTHING = 0.20

# -----------------------------------------------------------------------------
# Audio library
# -----------------------------------------------------------------------------
AUDIO_DIR = Path(__file__).resolve().parent / "audio"

# IDs used by Android / REST. Values are the exact filenames shown in your
# audio/ directory screenshot.
AUDIO_FILES = {
    "bronchial": AUDIO_DIR / "Bronchial BS.mp3.mpeg",
    "crackle": AUDIO_DIR / "Crackle lung.mp3.mpeg",
    "pleural_rub": AUDIO_DIR / "Pleural rub lung .mp3.mpeg",
    "ronchi": AUDIO_DIR / "Ronchi lung.mp3.mpeg",
    "stridor": AUDIO_DIR / "Stridor lung.mp3.mpeg",
    "vesicular": AUDIO_DIR / "Vesicular BS.mp3.mpeg",
    "wheeze": AUDIO_DIR / "Wheeze lung.mp3.mpeg",
}

DEFAULT_SOUND_ID = "vesicular"

# -----------------------------------------------------------------------------
# HTTP server
# -----------------------------------------------------------------------------
API_HOST = "0.0.0.0"
API_PORT = 8000
