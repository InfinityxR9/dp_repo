/*
 * LUNG AUSCULTATION TRAINER - Arduino Mega 2560
 * 7 piezo locations. Pin assignments remain editable placeholders.
 *
 * Serial protocol:
 *   READY
 *   A,<sensor>,<pressure>
 *   P,<sensor>,<pressure>
 *
 * Pressure is a normalized intensity index, NOT a direct physical-force value.
 * Piezo decay is intentionally ignored after a press is captured.
 */

// -----------------------------------------------------------------------------
// SENSOR PIN PLACEHOLDERS
// -----------------------------------------------------------------------------
#define SENSOR_1_PIN  A0
#define SENSOR_2_PIN  A1
#define SENSOR_3_PIN  A2
#define SENSOR_4_PIN  A3
#define SENSOR_5_PIN  A4
#define SENSOR_6_PIN  A5
#define SENSOR_7_PIN  A6

const uint8_t SENSOR_COUNT = 7;
const uint8_t SENSOR_PINS[SENSOR_COUNT] = {
    SENSOR_1_PIN, SENSOR_2_PIN, SENSOR_3_PIN, SENSOR_4_PIN,
    SENSOR_5_PIN, SENSOR_6_PIN, SENSOR_7_PIN
};

// -----------------------------------------------------------------------------
// TUNING
// -----------------------------------------------------------------------------
const int PRESS_THRESHOLD = 25;
const int MAX_PEAK = 150;
const float FILTER_ALPHA = 0.50f;
const uint8_t PRESS_CONFIRM_COUNT = 2;
const unsigned long PEAK_CAPTURE_TIME_MS = 150;
const int PEAK_UPDATE_MARGIN = 8;
const int PRESSURE_UPDATE_MARGIN = 3;
const int SENSOR_SWITCH_MARGIN = 15;
const int SENSOR_RISE_THRESHOLD = 8;
const unsigned long SENSOR_SWITCH_LOCKOUT_MS = 120;

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

enum SensorState { IDLE, CAPTURING, HOLDING };
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

    float x = (float)(value - PRESS_THRESHOLD) /
              (float)(MAX_PEAK - PRESS_THRESHOLD);
    x = constrain(x, 0.0f, 1.0f);

    // Softer than a square; still strongly favors a hard press.
    float curved = x * sqrt(x);
    int pressure = (int)(curved * 100.0f);

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

void activateSensor(int sensor, const int signals[]) {
    activeSensor = sensor;
    peak[sensor] = signals[sensor];
    activePressure = peakToPressure(peak[sensor]);
    captureStart = millis();
    activeSince = millis();
    state = CAPTURING;

    sendActivation(sensor, activePressure);
}

void setup() {
    Serial.begin(115200);
    delay(500);

    // Keep the stethoscope off all sensors during startup calibration.
    for (int i = 0; i < SENSOR_COUNT; i++) {
        long total = 0;

        for (int j = 0; j < 20; j++) {
            total += analogRead(SENSOR_PINS[i]);
            delay(2);
        }

        baseline[i] = total / 20;
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

    // -------------------------------------------------------------------------
    // IDLE
    // -------------------------------------------------------------------------
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
                candidateSensor = -1;
                candidateCount = 0;
            }
        } else {
            candidateSensor = -1;
            candidateCount = 0;
        }
    }

    // -------------------------------------------------------------------------
    // CAPTURING
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // HOLDING
    // -------------------------------------------------------------------------
    else if (state == HOLDING) {
        int strongest = findStrongestSensor(signals);

        // Try to detect a new press on another sensor rather than switching
        // merely because a decaying piezo value remains above threshold.
        if (strongest >= 0 && strongest != activeSensor &&
            millis() - activeSince >= SENSOR_SWITCH_LOCKOUT_MS) {

            int newSignal = signals[strongest];
            int rising = newSignal - previousSignal[strongest];

            if (newSignal >= PRESS_THRESHOLD + SENSOR_SWITCH_MARGIN &&
                rising >= SENSOR_RISE_THRESHOLD) {
                activateSensor(strongest, signals);
            }
        }

        // Same sensor pressed harder: capture a new peak and update volume.
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
