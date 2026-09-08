"""Configuration for the Raspberry Pi side of the lung auscultation trainer."""

from pathlib import Path

# -----------------------------------------------------------------------------
# Serial / Arduino
# -----------------------------------------------------------------------------
SERIAL_PORT = "/dev/ttyACM0"
BAUD_RATE = 115200
SERIAL_TIMEOUT = 0.02
SERIAL_RECONNECT_DELAY = 1.0

# -----------------------------------------------------------------------------
# MAX98357A SD_MODE GPIOs
# -----------------------------------------------------------------------------
# BCM GPIO numbers, NOT physical header pin numbers.
# Fill these in only after physically wiring each SD_MODE pin.
# None means that location is not yet configured.
AMP_SD_PINS = {
    1: 4,
    2: 16,
    3: 6,
    4: 12,
    5: 13,
    6: None,
    7: None,
}

AMP_ACTIVE_HIGH = True
AMP_SWITCH_MUTE_MS = 12

# -----------------------------------------------------------------------------
# Audio
# -----------------------------------------------------------------------------
AUDIO_SAMPLE_RATE = 48000
AUDIO_CHANNELS = 2
AUDIO_BUFFER = 256

# These are deliberately low/high operating ceilings.
# IMPORTANT: these are software mixer levels, not physical SPL/dB values.
STETHOSCOPE_MAX_VOLUME = 0.05
TEST_MAX_VOLUME = 0.20

DEFAULT_VOLUME_MODE = "stethoscope"
DEFAULT_VOLUME_MULTIPLIER = 1.0

# Volume smoothing is applied by a 5 ms worker tick.
# 1.0 = immediate, lower = smoother.
VOLUME_SMOOTHING = 0.25

# "auto" finds an SDL playback device containing MAX98357A.
# If auto-detection fails, set this to the exact SDL device name printed at startup.
AUDIO_DEVICE = "auto"

# -----------------------------------------------------------------------------
# Audio library
# -----------------------------------------------------------------------------
AUDIO_DIR = Path(__file__).resolve().parent / "audio"

AUDIO_FILES = {
    "bronchial": AUDIO_DIR / "bronchial.wav",
    "vesicular": AUDIO_DIR / "vesicular.wav",
    "wheeze": AUDIO_DIR / "wheeze.wav",
    "crackle": AUDIO_DIR / "crackle.wav",
    "stridor": AUDIO_DIR / "stridor.wav",
    "pleural_rub": AUDIO_DIR / "pleural_rub.wav",
    "ronchi": AUDIO_DIR / "ronchi.wav",
}
TEST_SONG = Path(__file__).resolve().parent / "Without Me.wav"

DEFAULT_SOUND_ID = "vesicular"

# -----------------------------------------------------------------------------
# HTTP server
# -----------------------------------------------------------------------------
API_HOST = "0.0.0.0"
API_PORT = 8000

# -----------------------------------------------------------------------------
# Console debugging
# -----------------------------------------------------------------------------
# Allows commands such as:
#   play wheeze
#   volume 0.5
#   mode test
#   select 3
#   status
#   stop
CONSOLE_DEBUG = True

# -----------------------------------------------------------------------------
# Safety / diagnostics
# -----------------------------------------------------------------------------
# Do not let the user push the software master multiplier above 1.0.
MASTER_MULTIPLIER_MIN = 0.0
MASTER_MULTIPLIER_MAX = 1.0
