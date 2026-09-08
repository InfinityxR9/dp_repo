"""Raspberry Pi application for the lung auscultation trainer.

One process owns:
  - Retrofit-facing HTTP API
  - Arduino USB serial input
  - MAX98357A SD_MODE GPIO selection
  - continuous WAV playback
  - console debugging controls
"""

from contextlib import asynccontextmanager
from pathlib import Path
import threading

import serial
import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import AliasChoices, BaseModel, ConfigDict, Field

from amplifier_manager import AmplifierManager
from audio_manager import AudioManager
from config import *
from sensor_manager import SensorManager
from serial_processor import process_serial_line


amplifier_manager = AmplifierManager(
    AMP_SD_PINS,
    active_high=AMP_ACTIVE_HIGH,
)

audio_manager = AudioManager(
    sample_rate=AUDIO_SAMPLE_RATE,
    channels=AUDIO_CHANNELS,
    buffer_size=AUDIO_BUFFER,
    stethoscope_max=STETHOSCOPE_MAX_VOLUME,
    test_max=TEST_MAX_VOLUME,
    smoothing=VOLUME_SMOOTHING,
    default_mode=DEFAULT_VOLUME_MODE,
    default_multiplier=DEFAULT_VOLUME_MULTIPLIER,
)

sensor_manager = SensorManager(
    amplifier_manager,
    audio_manager,
    switch_mute_ms=AMP_SWITCH_MUTE_MS,
)

stop_event = threading.Event()
arduino_status_lock = threading.Lock()
arduino_connected = False
serial_thread = None
audio_thread = None
console_thread = None


class PlayRequest(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)
    sound_id: str = Field(
        validation_alias=AliasChoices("soundId", "sound_id", "sound")
    )


class VolumeRequest(BaseModel):
    """Current frontend contract: {"multiplier": 0..1}.

    Also accepts the richer debugging/future mode payload:
      {"multiplier": 0.8, "mode": "test"}
      {"testMode": true, "multiplier": 1.0}
      {"volume": 0.8}  # backwards-compatible alias
    """

    model_config = ConfigDict(extra="ignore", populate_by_name=True)
    multiplier: float | None = None
    volume: float | None = None
    mode: str | None = None
    test_mode: bool | None = Field(
        default=None,
        validation_alias=AliasChoices("testMode", "test_mode"),
    )


def set_arduino_status(value: bool):
    global arduino_connected
    with arduino_status_lock:
        arduino_connected = value


def get_arduino_status() -> bool:
    with arduino_status_lock:
        return arduino_connected


def serial_worker():
    """Read Arduino events forever and reconnect if USB disappears."""
    while not stop_event.is_set():
        serial_port = None
        try:
            serial_port = serial.Serial(
                SERIAL_PORT,
                BAUD_RATE,
                timeout=SERIAL_TIMEOUT,
            )
            set_arduino_status(True)
            print(f"[SERIAL] Arduino connected on {SERIAL_PORT}")

            while not stop_event.is_set():
                raw = serial_port.readline()
                if not raw:
                    continue
                line = raw.decode("utf-8", errors="ignore").strip()
                if line:
                    process_serial_line(line, sensor_manager)

        except (serial.SerialException, OSError) as exc:
            set_arduino_status(False)
            if not stop_event.is_set():
                print(f"[SERIAL] {exc}; retrying in {SERIAL_RECONNECT_DELAY}s")
                stop_event.wait(SERIAL_RECONNECT_DELAY)
        except Exception as exc:
            set_arduino_status(False)
            if not stop_event.is_set():
                print(f"[SERIAL] unexpected error: {exc}")
                stop_event.wait(SERIAL_RECONNECT_DELAY)
        finally:
            if serial_port is not None:
                try:
                    serial_port.close()
                except Exception:
                    pass

    set_arduino_status(False)


def audio_worker():
    while not stop_event.is_set():
        try:
            audio_manager.update()
        except Exception as exc:
            print(f"[AUDIO] update error: {exc}")
        stop_event.wait(0.005)


def print_state():
    sensor_state = sensor_manager.get_state()
    audio_state = audio_manager.get_state()
    print(
        "[STATE] "
        f"arduino={get_arduino_status()} "
        f"sensor={sensor_state['activeSensor']} "
        f"pressure={sensor_state['pressure']}% "
        f"sound={Path(audio_state['sound']).name if audio_state['sound'] else None} "
        f"mode={audio_state['mode']} "
        f"multiplier={audio_state['masterMultiplier']} "
        f"volume={audio_state['currentVolume']} "
        f"target={audio_state['targetVolume']}"
    )


def console_worker():
    """Interactive SSH debugging console; safe to leave disabled in services."""
    print(
        "[CONSOLE] commands: "
        "play <sound>, volume <0..1|0..100>, mode <test|stethoscope>, "
        "select <1..7>, stop, status, help, quit"
    )

    while not stop_event.is_set():
        try:
            command = input("debug> ").strip()
        except (EOFError, KeyboardInterrupt):
            return

        if not command:
            continue

        args = command.split()
        op = args[0].lower()

        try:
            if op == "play" and len(args) == 2:
                sound_id = args[1].strip().lower()
                path = AUDIO_FILES.get(sound_id)
                if path is None:
                    print(f"[CONSOLE] unknown sound: {sound_id}")
                    continue
                if not path.is_file():
                    print(f"[CONSOLE] missing WAV: {path}")
                    continue
                sensor_manager.stop()
                audio_manager.play(path)
                print(f"[CONSOLE] playing {sound_id}")

            elif op == "volume" and len(args) == 2:
                audio_manager.set_multiplier(float(args[1]))
                state = audio_manager.get_state()
                print(
                    f"[CONSOLE] multiplier={state['masterMultiplier']} "
                    f"target={state['targetVolume']}"
                )

            elif op == "mode" and len(args) == 2:
                audio_manager.set_mode(args[1])
                state = audio_manager.get_state()
                print(
                    f"[CONSOLE] mode={state['mode']} "
                    f"ceiling={state['modeCeiling']}"
                )

            elif op == "select" and len(args) == 2:
                location = int(args[1])
                if not 1 <= location <= 7:
                    raise ValueError("location must be 1..7")
                sensor_manager.activation(location, 0)
                print(f"[CONSOLE] selected location {location}")

            elif op == "stop" and len(args) == 1:
                audio_manager.stop()
                sensor_manager.stop()
                print("[CONSOLE] stopped")

            elif op == "status" and len(args) == 1:
                print_state()

            elif op == "help" and len(args) == 1:
                print(
                    "play <sound> | volume <0..1|0..100> | "
                    "mode <test|stethoscope> | select <1..7> | "
                    "stop | status | quit"
                )

            elif op in {"quit", "exit"} and len(args) == 1:
                print("[CONSOLE] stopping application")
                stop_event.set()
                return

            else:
                print("[CONSOLE] unknown command; use 'help'")

        except (ValueError, FileNotFoundError) as exc:
            print(f"[CONSOLE] error: {exc}")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    global serial_thread, audio_thread, console_thread

    # Guarantee a known-safe state on startup.
    sensor_manager.stop()
    stop_event.clear()

    serial_thread = threading.Thread(
        target=serial_worker,
        name="arduino-serial",
        daemon=True,
    )
    audio_thread = threading.Thread(
        target=audio_worker,
        name="audio-volume",
        daemon=True,
    )

    serial_thread.start()
    audio_thread.start()

    if CONSOLE_DEBUG:
        console_thread = threading.Thread(
            target=console_worker,
            name="debug-console",
            daemon=True,
        )
        console_thread.start()

    print(f"[HTTP] API listening on http://{API_HOST}:{API_PORT}")
    print(f"[AMP] configured: {amplifier_manager.get_configured_locations()}")

    try:
        yield
    finally:
        stop_event.set()
        audio_manager.stop()
        sensor_manager.stop()
        amplifier_manager.close()
        audio_manager.close()


app = FastAPI(
    title="Lung Auscultation Trainer Hardware API",
    version="3.0",
    lifespan=lifespan,
)


@app.get("/api/health")
def health():
    return {
        "success": True,
        "message": "running",
        "arduinoConnected": get_arduino_status(),
        "configuredAmplifiers": amplifier_manager.get_configured_locations(),
    }


@app.get("/api/state")
def state():
    sensor_state = sensor_manager.get_state()
    audio_state = audio_manager.get_state()

    return {
        "success": True,
        "running": not stop_event.is_set(),
        "arduinoConnected": get_arduino_status(),
        "activeSensor": sensor_state["activeSensor"],
        "pressure": sensor_state["pressure"],
        "soundId": (
            Path(audio_state["sound"]).stem
            if audio_state["sound"]
            else None
        ),
        "audioPlaying": audio_state["playing"],
        "audioVolume": audio_state["currentVolume"],
        "targetVolume": audio_state["targetVolume"],
        "masterMultiplier": audio_state["masterMultiplier"],
        "volumeMode": audio_state["mode"],
        "modeCeiling": audio_state["modeCeiling"],
        "configuredAmplifiers": amplifier_manager.get_configured_locations(),
    }


@app.post("/api/play")
def play(request: PlayRequest):
    sound_id = request.sound_id.strip().lower()
    path = AUDIO_FILES.get(sound_id)

    if path is None:
        raise HTTPException(
            status_code=400,
            detail=f"Unknown soundId '{sound_id}'. Valid IDs: {sorted(AUDIO_FILES)}",
        )

    path = Path(path)
    if not path.is_file():
        raise HTTPException(
            status_code=404,
            detail=f"Audio file not found: {path}",
        )

    try:
        # New sound begins silently and with no selected amplifier.
        sensor_manager.stop()
        audio_manager.play(path)
        return {
            "success": True,
            "message": "sound started",
            "soundId": sound_id,
            "playing": True,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/stop")
def stop():
    audio_manager.stop()
    sensor_manager.stop()
    return {
        "success": True,
        "message": "stopped",
        "playing": False,
        "activeSensor": None,
    }


@app.post("/api/volume")
def volume(request: VolumeRequest):
    try:
        if request.mode is not None:
            audio_manager.set_mode(request.mode)
        elif request.test_mode is not None:
            audio_manager.set_mode(
                "test" if request.test_mode else "stethoscope"
            )

        multiplier = request.multiplier
        if multiplier is None:
            multiplier = request.volume
        if multiplier is not None:
            audio_manager.set_multiplier(multiplier)

        state = audio_manager.get_state()
        return {
            "success": True,
            "message": "volume updated",
            "mode": state["mode"],
            "multiplier": state["masterMultiplier"],
            "modeCeiling": state["modeCeiling"],
            "pressure": state["pressure"],
            "targetVolume": state["targetVolume"],
        }
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/api/select/{location}")
def select_location(location: int):
    if not 1 <= location <= 7:
        raise HTTPException(status_code=400, detail="location must be 1..7")

    sensor_manager.activation(location, 0)
    configured = amplifier_manager.is_configured(location)

    return {
        "success": True,
        "message": "location selected",
        "location": location,
        "amplifierConfigured": configured,
        "pressure": 0,
    }

@app.post("/api/test/song")
def test_song():
    """
    Start the known-good test song for piezo/volume testing.

    The song loops silently until a piezo event arrives.
    Piezo pressure then controls its volume.
    """
    path = Path(TEST_SONG)

    if not path.is_file():
        raise HTTPException(
            status_code=404,
            detail=f"Test song not found: {path}",
        )

    try:
        # Reset Pi-side sensor/amplifier state.
        sensor_manager.stop()

        # Make the test clearly audible.
        audio_manager.set_mode("test")
        audio_manager.set_multiplier(1.0)

        # Start looping song silently.
        audio_manager.play(path)

        return {
            "success": True,
            "message": "piezo volume test started",
            "sound": path.name,
            "mode": "test",
            "multiplier": 1.0,
            "instruction": "Press a piezo sensor to control volume",
        }

    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=str(exc),
        ) from exc

if __name__ == "__main__":
    uvicorn.run(
        app,
        host=API_HOST,
        port=API_PORT,
        log_level="info",
    )
