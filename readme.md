# DP Lung Auscultation Trainer — Raspberry Pi

## Files

- `main.py` — single Pi application: FastAPI + serial worker + audio worker
- `config.py` — all configurable paths, audio IDs and placeholder GPIOs
- `audio_manager.py` — continuous looping playback + pressure-to-volume mapping
- `amplifier_manager.py` — one-at-a-time MAX98357A SD_MODE selection
- `sensor_manager.py` — applies Arduino events to the selected amplifier/audio
- `serial_processor.py` — Arduino serial protocol parser
- `audio/` — lung-sound files
- `ard/ard.ino` — Arduino Mega firmware
- `frontend/` — Android app; Retrofit calls this Pi HTTP API

## API

- `GET /api/health`
- `GET /api/state`
- `POST /api/play` body: `{"soundId":"wheeze"}`
- `POST /api/stop`
- `POST /api/select/{location}` (manual hardware test)

## Sound IDs

`bronchial`, `crackle`, `pleural_rub`, `ronchi`, `stridor`, `vesicular`, `wheeze`

The filenames are configured in `config.py` exactly as shown in the project's `audio/` screenshot.

## First boot

```bash
cd ~/dp_repo
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

The API listens on port 8000.

## Test commands

```bash
curl http://127.0.0.1:8000/api/health
curl http://127.0.0.1:8000/api/state
curl -X POST http://127.0.0.1:8000/api/play \
  -H 'Content-Type: application/json' \
  -d '{"soundId":"vesicular"}'

curl -X POST http://127.0.0.1:8000/api/stop
```

For a manual amplifier selection test after filling an SD GPIO placeholder:

```bash
curl -X POST http://127.0.0.1:8000/api/select/1
```
