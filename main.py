"""Main Raspberry Pi application for the lung auscultation trainer.

This single process owns:
    - the local HTTP API used by Android/Retrofit
    - Arduino USB serial input
    - MAX98357A SD_MODE GPIO selection
    - continuous audio playback and pressure-based volume control
"""

from contextlib import asynccontextmanager
from pathlib import Path
import threading
import time

import serial
import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from amplifier_manager import AmplifierManager
from audio_manager import AudioManager
from config import *
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


# -----------------------------------------------------------------------------
# HTTP models
# -----------------------------------------------------------------------------
class PlayRequest(BaseModel):
    soundId: str


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
                print(f"Arduino connected on {SERIAL_PORT}.")

                while not stop_event.is_set():
                    raw = serial_port.readline()
                    if not raw:
                        continue

                    line = raw.decode("utf-8", errors="ignore").strip()
                    if line:
                        process_serial_line(line, sensor_manager)

        except Exception as exc:
            if not stop_event.is_set():
                print(f"Serial error: {exc}")
                time.sleep(1.0)


def audio_worker():
    """Keep the mixer volume responsive without blocking serial/HTTP."""
    while not stop_event.is_set():
        try:
            audio_manager.update()
        except Exception as exc:
            print(f"Audio update error: {exc}")
        time.sleep(0.005)


# -----------------------------------------------------------------------------
# FastAPI lifespan
# -----------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(_app: FastAPI):
    sensor_manager.stop()

    serial_thread = threading.Thread(target=serial_worker, name="serial", daemon=True)
    audio_thread = threading.Thread(target=audio_worker, name="audio", daemon=True)

    serial_thread.start()
    audio_thread.start()

    print(f"Lung trainer API listening on http://{API_HOST}:{API_PORT}")
    try:
        yield
    finally:
        stop_event.set()
        sensor_manager.stop()
        amplifier_manager.close()
        audio_manager.close()


app = FastAPI(
    title="Lung Auscultation Trainer",
    version="1.0.0",
    lifespan=lifespan,
)


# -----------------------------------------------------------------------------
# HTTP API used by Android
# -----------------------------------------------------------------------------
@app.get("/api/health")
def health():
    return {"success": True, "message": "running"}


@app.get("/api/state")
def state():
    sensor_state = sensor_manager.get_state()
    return {
        "running": True,
        "activeSensor": sensor_state["activeSensor"],
        "pressure": sensor_state["pressure"],
        "soundId": Path(audio_manager.current_sound).name if audio_manager.current_sound else None,
        "audioVolume": round(audio_manager.get_volume(), 4),
    }


@app.post("/api/play")
def play(request: PlayRequest):
    path = AUDIO_FILES.get(request.soundId)
    if path is None:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown soundId: {request.soundId}",
        )

    path = Path(path)
    if not path.is_file():
        raise HTTPException(
            status_code=404,
            detail=f"Audio file not found: {path.name}",
        )

    try:
        audio_manager.play(path)
        sensor_manager.stop()
        print(f"[PLAY] {request.soundId} -> {path.name}")
        return {
            "success": True,
            "message": "sound started",
            "soundId": request.soundId,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/stop")
def stop():
    sensor_manager.stop()
    audio_manager.stop()
    print("[STOP]")
    return {"success": True, "message": "stopped"}


@app.post("/api/select/{location}")
def select_location(location: int):
    if not 1 <= location <= 7:
        raise HTTPException(status_code=400, detail="location must be 1..7")

    sensor_manager.activation(location, 0)
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
