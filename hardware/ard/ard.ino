/*
 * LUNG AUSCULTATION TRAINER - Arduino Mega 2560
 *
 * Seven piezo sensing locations.
 *
 * Serial protocol:
 *   READY
 *   A,<sensor>,<pressure>
 *   P,<sensor>,<pressure>
 *
 * pressure is a normalized 0..100 intensity index derived from the strongest
 * observed piezo peak. It is NOT a calibrated force measurement.
 *
 * Important sensor limitation:
 * Piezo elements are dynamic/charge-based sensors. Their signal decays after
 * a press even if physical contact is maintained. Therefore this firmware
 * intentionally does NOT treat natural decay as a release event.
 */

// -----------------------------------------------------------------------------
// SENSOR PIN BLOCK - EDIT HERE IF YOUR FINAL WIRING DIFFERS
// -----------------------------------------------------------------------------
#define SENSOR_1_PIN A0
#define SENSOR_2_PIN A1
#define SENSOR_3_PIN A2
#define SENSOR_4_PIN A3
#define SENSOR_5_PIN A4
#define SENSOR_6_PIN A5
#define SENSOR_7_PIN A6

const uint8_t SENSOR_COUNT = 7;
const uint8_t SENSOR_PINS[SENSOR_COUNT] = {
  SENSOR_1_PIN,
  SENSOR_2_PIN,
  SENSOR_3_PIN,
  SENSOR_4_PIN,
  SENSOR_5_PIN,
  SENSOR_6_PIN,
  SENSOR_7_PIN
};

// -----------------------------------------------------------------------------
// TUNING
// -----------------------------------------------------------------------------
// Based on the observed sensor data. Tune these after testing all seven.
const int PRESS_THRESHOLD = 25;
const int MAX_PEAK = 150;

// Fast, low-latency smoothing.
const float FILTER_ALPHA = 0.50f;

// Require two consecutive scans before activating a sensor.
const uint8_t PRESS_CONFIRM_COUNT = 2;

// Keep watching a new activation for a short time to capture the true peak.
const unsigned long PEAK_CAPTURE_TIME_MS = 150;

// Do not generate serial updates for tiny changes.
const int PEAK_UPDATE_MARGIN = 8;
const int PRESSURE_UPDATE_MARGIN = 3;

// New-location detection is intentionally stricter than initial activation.
const int SENSOR_SWITCH_MARGIN = 15;
const int SENSOR_RISE_THRESHOLD = 8;
const int SENSOR_DOMINANCE_MARGIN = 10;
const uint8_t SWITCH_CONFIRM_COUNT = 2;
const unsigned long SENSOR_SWITCH_LOCKOUT_MS = 180;

// -----------------------------------------------------------------------------
// STATE
// -----------------------------------------------------------------------------
float filtered[SENSOR_COUNT];
int baseline[SENSOR_COUNT];
int peak[SENSOR_COUNT];
int previousSignal[SENSOR_COUNT];

int activeSensor = -1;
int activePressure = 0;
unsigned long captureStart = 0;
unsigned long activeSince = 0;

int candidateSensor = -1;
uint8_t candidateCount = 0;

int switchCandidate = -1;
uint8_t switchCandidateCount = 0;

enum SensorState {
  IDLE,
  CAPTURING,
  HOLDING
};

SensorState state = IDLE;

// -----------------------------------------------------------------------------
// SERIAL
// -----------------------------------------------------------------------------
void sendActivation(int sensor, int pressure) {
  Serial.print("A,");
  Serial.print(sensor + 1);
  Serial.print(",");
  Serial.println(pressure);
}

void sendPressureUpdate(int sensor, int pressure) {
  Serial.print("P,");
  Serial.print(sensor + 1);
  Serial.print(",");
  Serial.println(pressure);
}

// -----------------------------------------------------------------------------
// PRESSURE MAPPING
// -----------------------------------------------------------------------------
int peakToPressure(int value) {
  if (value <= PRESS_THRESHOLD) return 0;
  if (value >= MAX_PEAK) return 100;

  float x = (float)(value - PRESS_THRESHOLD) /
            (float)(MAX_PEAK - PRESS_THRESHOLD);
  x = constrain(x, 0.0f, 1.0f);

  // Gamma ≈ 1.5 gives a useful low-pressure response while still strongly
  // separating a hard press from a light one.
  float curved = x * sqrt(x);
  int pressure = (int)(curved * 100.0f + 0.5f);

  return constrain(pressure, 0, 100);
}

// -----------------------------------------------------------------------------
// SENSOR READING
// -----------------------------------------------------------------------------
int readSensor(uint8_t index) {
  int raw = analogRead(SENSOR_PINS[index]);

  filtered[index] =
    FILTER_ALPHA * raw +
    (1.0f - FILTER_ALPHA) * filtered[index];

  int signal = (int)filtered[index] - baseline[index];
  if (signal < 0) signal = 0;

  return signal;
}

int findStrongestSensor(const int signals[]) {
  int strongest = -1;
  int strongestValue = PRESS_THRESHOLD;

  for (int i = 0; i < SENSOR_COUNT; i++) {
    if (signals[i] > strongestValue) {
      strongestValue = signals[i];
      strongest = i;
    }
  }

  return strongest;
}

int findSecondStrongestValue(const int signals[], int excluded) {
  int second = 0;

  for (int i = 0; i < SENSOR_COUNT; i++) {
    if (i == excluded) continue;
    if (signals[i] > second) second = signals[i];
  }

  return second;
}

void activateSensor(int sensor, const int signals[]) {
  activeSensor = sensor;
  peak[sensor] = signals[sensor];
  activePressure = peakToPressure(peak[sensor]);

  captureStart = millis();
  activeSince = millis();
  state = CAPTURING;

  candidateSensor = -1;
  candidateCount = 0;
  switchCandidate = -1;
  switchCandidateCount = 0;

  sendActivation(sensor, activePressure);
}

void setup() {
  Serial.begin(115200);
  delay(300);

  // The stethoscope should be off all sensors during startup calibration.
  for (int i = 0; i < SENSOR_COUNT; i++) {
    long total = 0;

    for (int j = 0; j < 30; j++) {
      total += analogRead(SENSOR_PINS[i]);
      delay(2);
    }

    baseline[i] = total / 30;
    filtered[i] = baseline[i];
    previousSignal[i] = 0;
    peak[i] = 0;
  }

  Serial.println("READY");
}

void loop() {
  int signals[SENSOR_COUNT];

  for (int i = 0; i < SENSOR_COUNT; i++) {
    signals[i] = readSensor(i);
  }

  // ---------------------------------------------------------------------------
  // IDLE: detect a new stethoscope placement/press.
  // ---------------------------------------------------------------------------
  if (state == IDLE) {
    int strongest = findStrongestSensor(signals);

    if (strongest >= 0) {
      if (candidateSensor == strongest) {
        candidateCount++;
      } else {
        candidateSensor = strongest;
        candidateCount = 1;
      }

      if (candidateCount >= PRESS_CONFIRM_COUNT) {
        activateSensor(strongest, signals);
      }
    } else {
      candidateSensor = -1;
      candidateCount = 0;
    }
  }

  // ---------------------------------------------------------------------------
  // CAPTURING: capture the initial peak for the active location.
  // ---------------------------------------------------------------------------
  else if (state == CAPTURING) {
    int signal = signals[activeSensor];

    if (signal > peak[activeSensor] + PEAK_UPDATE_MARGIN) {
      peak[activeSensor] = signal;

      int newPressure = peakToPressure(peak[activeSensor]);
      if (newPressure > activePressure + PRESSURE_UPDATE_MARGIN) {
        activePressure = newPressure;
        sendPressureUpdate(activeSensor, activePressure);
      }
    }

    if (millis() - captureStart >= PEAK_CAPTURE_TIME_MS) {
      state = HOLDING;
    }
  }

  // ---------------------------------------------------------------------------
  // HOLDING: ignore natural piezo decay. Look only for genuinely new events.
  // ---------------------------------------------------------------------------
  else if (state == HOLDING) {
    int strongest = findStrongestSensor(signals);

    if (strongest >= 0 && strongest != activeSensor &&
        millis() - activeSince >= SENSOR_SWITCH_LOCKOUT_MS) {

      int newSignal = signals[strongest];
      int secondStrongest = findSecondStrongestValue(signals, strongest);
      int rising = newSignal - previousSignal[strongest];

      bool strongEnough =
        newSignal >= PRESS_THRESHOLD + SENSOR_SWITCH_MARGIN;
      bool risingEnough = rising >= SENSOR_RISE_THRESHOLD;
      bool dominantEnough =
        newSignal >= secondStrongest + SENSOR_DOMINANCE_MARGIN;

      if (strongEnough && risingEnough && dominantEnough) {
        if (switchCandidate == strongest) {
          switchCandidateCount++;
        } else {
          switchCandidate = strongest;
          switchCandidateCount = 1;
        }

        if (switchCandidateCount >= SWITCH_CONFIRM_COUNT) {
          activateSensor(strongest, signals);
        }
      } else {
        switchCandidate = -1;
        switchCandidateCount = 0;
      }
    }

    // Same sensor pressed harder: update volume from a new larger peak.
    int currentSignal = signals[activeSensor];

    if (currentSignal > peak[activeSensor] + PEAK_UPDATE_MARGIN) {
      peak[activeSensor] = currentSignal;

      int newPressure = peakToPressure(peak[activeSensor]);
      if (newPressure > activePressure + PRESSURE_UPDATE_MARGIN) {
        activePressure = newPressure;
        sendPressureUpdate(activeSensor, activePressure);
      }
    }
  }

  for (int i = 0; i < SENSOR_COUNT; i++) {
    previousSignal[i] = signals[i];
  }
}
