"""Main Raspberry Pi application for the lung auscultation trainer.

Owns, in one process:
- local HTTP API used by Android/Retrofit
- Arduino USB serial input
- MAX98357A SD_MODE GPIO selection
- continuous WAV playback and pressure-based volume
"""

from contextlib import asynccontextmanager
from pathlib import Path
import threading
import time

import serial
import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from amplifier_manager import AmplifierManager
from audio_manager import AudioManager
from config import (
    AMP_SD_PINS,
    AUDIO_FILES,
    AUDIO_SAMPLE_RATE,
    AUDIO_BUFFER,
    AUDIO_UPDATE_INTERVAL,
    BAUD_RATE,
    API_HOST,
    API_PORT,
    MAX_AUDIO_VOLUME,
    SERIAL_PORT,
    SERIAL_TIMEOUT,
    VOLUME_SMOOTHING,
)
from sensor_manager import SensorManager
from serial_processor import process_serial_line


# -----------------------------------------------------------------------------
# Managers
# -----------------------------------------------------------------------------
amplifier_manager = AmplifierManager(AMP_SD_PINS)
audio_manager = AudioManager(
    sample_rate=AUDIO_SAMPLE_RATE,
    buffer_size=AUDIO_BUFFER,
    max_volume=MAX_AUDIO_VOLUME,
    smoothing=VOLUME_SMOOTHING,
)
sensor_manager = SensorManager(amplifier_manager, audio_manager)

stop_event = threading.Event()
arduino_connected = False
state_lock = threading.RLock()


# -----------------------------------------------------------------------------
# HTTP request models
# -----------------------------------------------------------------------------
class PlayRequest(BaseModel):
    soundId: str = Field(..., min_length=1)


class VolumeRequest(BaseModel):
    volume: float = Field(..., ge=0.0, le=1.0)


# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
def set_arduino_connected(value):
    global arduino_connected
    with state_lock:
        arduino_connected = bool(value)


def check_audio_files():
    missing = [str(path) for path in AUDIO_FILES.values() if not path.is_file()]
    if missing:
        print("[AUDIO] Missing canonical WAV files:")
        for path in missing:
            print(f"        {path}")
        print("[AUDIO] Convert/copy your seven lung sounds into audio/*.wav")

class VolumeRequest(BaseModel):
    volume: float


# -----------------------------------------------------------------------------
# Background workers
# -----------------------------------------------------------------------------
def serial_worker():
    """Read Arduino events forever and reconnect if USB disappears."""
    while not stop_event.is_set():
        try:
            with serial.Serial(
                SERIAL_PORT,
                BAUD_RATE,
                timeout=SERIAL_TIMEOUT,
            ) as serial_port:
                set_arduino_connected(True)
                print(f"[SERIAL] Arduino connected on {SERIAL_PORT}")

                while not stop_event.is_set():
                    raw = serial_port.readline()
                    if not raw:
                        continue

                    line = raw.decode("utf-8", errors="ignore").strip()
                    if line:
                        process_serial_line(line, sensor_manager)

        except Exception as exc:
            set_arduino_connected(False)
            if not stop_event.is_set():
                print(f"[SERIAL] Error: {exc}")
                time.sleep(1.0)

    set_arduino_connected(False)


def audio_worker():
    while not stop_event.is_set():
        try:
            audio_manager.update()
        except Exception as exc:
            print(f"[AUDIO] Update error: {exc}")
        time.sleep(AUDIO_UPDATE_INTERVAL)


# -----------------------------------------------------------------------------
# FastAPI lifespan
# -----------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(_app: FastAPI):
    stop_event.clear()
    sensor_manager.stop()
    check_audio_files()

    serial_thread = threading.Thread(
        target=serial_worker,
        name="serial",
        daemon=True,
    )
    audio_thread = threading.Thread(
        target=audio_worker,
        name="audio",
        daemon=True,
    )

    serial_thread.start()
    audio_thread.start()

    print(f"[API] Listening on http://{API_HOST}:{API_PORT}")

    try:
        yield
    finally:
        stop_event.set()
        sensor_manager.stop()
        audio_manager.stop()
        amplifier_manager.close()
        audio_manager.close()


app = FastAPI(
    title="Lung Auscultation Trainer",
    version="2.0.0",
    lifespan=lifespan,
)


# -----------------------------------------------------------------------------
# API used by Android / Retrofit
# -----------------------------------------------------------------------------
@app.get("/api/health")
def health():
    return {
        "success": True,
        "message": "running",
    }


@app.get("/api/state")
def state():
    sensor_state = sensor_manager.get_state()
    audio_state = audio_manager.get_state()
    with state_lock:
        connected = arduino_connected

        return {
        "running": audio_state["playing"],
        "arduinoConnected": connected,
        "activeSensor": sensor_state["activeSensor"],
        "pressure": sensor_state["pressure"],
        "soundId": audio_state["sound"],
        "audioPlaying": audio_state["playing"],
        "audioVolume": audio_state["volume"],
        "targetVolume": audio_state["targetVolume"],
        "masterVolume": audio_state["masterVolume"],
        "activeAmplifier": amplifier_manager.get_active_location(),
    }


@app.post("/api/play")
def play(request: PlayRequest):
    sound_id = request.soundId.strip().lower()
    path = AUDIO_FILES.get(sound_id)

    if path is None:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown soundId: {sound_id}",
        )

    if not path.is_file():
        raise HTTPException(
            status_code=404,
            detail=f"Audio file not found: {path.name}",
        )

    try:
        sensor_manager.stop()
        audio_manager.play(path)
        print(f"[PLAY] {sound_id} -> {path.name}")
        return {
            "success": True,
            "message": "sound started",
            "soundId": sound_id,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/stop")
def stop():
    sensor_manager.stop()
    audio_manager.stop()
    print("[STOP]")
    return {"success": True, "message": "stopped"}

@app.post("/api/volume")
def set_volume(request: VolumeRequest):
    if not 0.0 <= request.volume <= 1.0:
        raise HTTPException(
            status_code=400,
            detail="volume must be between 0.0 and 1.0",
        )

    audio_manager.set_master_volume(
        request.volume
    )

    return {
        "success": True,
        "message": "master volume updated",
        "volume": audio_manager.get_master_volume(),
    }


@app.post("/api/volume")
def volume(request: VolumeRequest):
    audio_manager.set_master_volume(request.volume)
    return {
        "success": True,
        "message": "volume updated",
        "volume": audio_manager.master_volume,
    }


@app.post("/api/select/{location}")
def select_location(location: int):
    if not 1 <= location <= 7:
        raise HTTPException(status_code=400, detail="location must be 1..7")

    try:
        sensor_manager.activation(location, 0)
    except ValueError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "success": True,
        "message": "location selected",
        "location": location,
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host=API_HOST,
        port=API_PORT,
        log_level="info",
    )
