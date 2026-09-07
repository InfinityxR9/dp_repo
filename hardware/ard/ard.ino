/*
 * LUNG AUSCULTATION TRAINER - Arduino Mega 2560
 *
 * Seven piezo locations. Pin definitions are intentionally easy to change.
 *
 * Serial protocol:
 *   READY
 *   A,<sensor>,<pressure> = newly detected active location
 *   P,<sensor>,<pressure> = stronger pressure at the active location
 *
 * Pressure is a normalized intensity index derived from the piezo peak.
 * It is NOT a calibrated physical pressure measurement.
 *
 * Important piezo limitation:
 *   the piezo voltage naturally decays after a press, even if contact remains.
 *   Therefore decay is NOT treated as a reliable release signal.
 */

#include <Arduino.h>

// -----------------------------------------------------------------------------
// SENSOR PIN PLACEHOLDERS
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
    SENSOR_1_PIN, SENSOR_2_PIN, SENSOR_3_PIN, SENSOR_4_PIN,
    SENSOR_5_PIN, SENSOR_6_PIN, SENSOR_7_PIN
};

// -----------------------------------------------------------------------------
// CALIBRATION / EVENT TUNING
// -----------------------------------------------------------------------------
// Based on the observed readings: light press ~80-100, hard press ~150.
// Recalibrate these two values for the final physical assembly.
const int PRESS_THRESHOLD = 25;
const int MAX_PEAK = 150;

// Fast enough for responsive detection while reducing one-sample noise.
const float FILTER_ALPHA = 0.50f;

// Require more than one scan above threshold to confirm an event.
const uint8_t PRESS_CONFIRM_COUNT = 2;

// Capture the initial transient briefly so a hard press can establish its peak.
const unsigned long PEAK_CAPTURE_TIME_MS = 150;

// Only send meaningful peak/pressure changes.
const int PEAK_UPDATE_MARGIN = 8;
const int PRESSURE_UPDATE_MARGIN = 3;

// Cross-talk protection when switching between locations.
const int SENSOR_SWITCH_MARGIN = 15;
const int SENSOR_DOMINANCE_MARGIN = 10;
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
// SERIAL OUTPUT
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
// Default response is LINEAR so the Pi receives a pressure value that is
// proportional to the normalized piezo peak.
int peakToPressure(int value) {
    if (value <= PRESS_THRESHOLD) return 0;

    const float denominator = (float)(MAX_PEAK - PRESS_THRESHOLD);
    if (denominator <= 0.0f) return 100;

    float x = (float)(value - PRESS_THRESHOLD) / denominator;
    x = constrain(x, 0.0f, 1.0f);

    const int pressure = (int)(x * 100.0f + 0.5f);
    return constrain(pressure, 0, 100);
}

// -----------------------------------------------------------------------------
// SENSOR READING
// -----------------------------------------------------------------------------
int readSensor(uint8_t index) {
    const int raw = analogRead(SENSOR_PINS[index]);

    filtered[index] =
        FILTER_ALPHA * raw +
        (1.0f - FILTER_ALPHA) * filtered[index];

    int signal = (int)filtered[index] - baseline[index];
    if (signal < 0) signal = 0;

    return signal;
}

void findTopTwoSensors(
    const int signals[],
    int &bestSensor,
    int &bestValue,
    int &secondValue
) {
    bestSensor = -1;
    bestValue = 0;
    secondValue = 0;

    for (int i = 0; i < SENSOR_COUNT; i++) {
        const int value = signals[i];

        if (value > bestValue) {
            secondValue = bestValue;
            bestValue = value;
            bestSensor = i;
        } else if (value > secondValue) {
            secondValue = value;
        }
    }
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

// -----------------------------------------------------------------------------
// SETUP
// -----------------------------------------------------------------------------
void setup() {
    Serial.begin(115200);
    delay(500);

    // Nothing should touch the sensors during this baseline measurement.
    for (int i = 0; i < SENSOR_COUNT; i++) {
        long total = 0;

        for (int j = 0; j < 30; j++) {
            total += analogRead(SENSOR_PINS[i]);
            delay(2);
        }

        baseline[i] = total / 30;
        filtered[i] = (float)baseline[i];
        previousSignal[i] = 0;
        peak[i] = 0;
    }

    Serial.println("READY");
}

// -----------------------------------------------------------------------------
// MAIN LOOP
// -----------------------------------------------------------------------------
void loop() {
    int signals[SENSOR_COUNT];

    for (int i = 0; i < SENSOR_COUNT; i++) {
        signals[i] = readSensor(i);
    }

    int bestSensor;
    int bestValue;
    int secondValue;
    findTopTwoSensors(signals, bestSensor, bestValue, secondValue);

    // -------------------------------------------------------------------------
    // IDLE
    // -------------------------------------------------------------------------
    if (state == IDLE) {
        const bool validCandidate =
            bestSensor >= 0 &&
            bestValue >= PRESS_THRESHOLD &&
            bestValue >= secondValue + SENSOR_DOMINANCE_MARGIN;

        if (validCandidate) {
            if (candidateSensor == bestSensor) {
                if (candidateCount < PRESS_CONFIRM_COUNT) {
                    candidateCount++;
                }
            } else {
                candidateSensor = bestSensor;
                candidateCount = 1;
            }

            if (candidateCount >= PRESS_CONFIRM_COUNT) {
                activateSensor(bestSensor, signals);
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
        const int signal = signals[activeSensor];

        if (signal > peak[activeSensor] + PEAK_UPDATE_MARGIN) {
            peak[activeSensor] = signal;

            const int newPressure = peakToPressure(peak[activeSensor]);
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
        // Only a genuinely rising event on another sensor can switch location.
        if (bestSensor >= 0 && bestSensor != activeSensor &&
            millis() - activeSince >= SENSOR_SWITCH_LOCKOUT_MS) {

            const int newSignal = signals[bestSensor];
            const int rising = newSignal - previousSignal[bestSensor];

            const bool newEvent =
                newSignal >= PRESS_THRESHOLD + SENSOR_SWITCH_MARGIN &&
                newSignal >= secondValue + SENSOR_DOMINANCE_MARGIN &&
                rising >= SENSOR_RISE_THRESHOLD;

            if (newEvent) {
                activateSensor(bestSensor, signals);
            }
        }

        // A stronger press at the active location raises the pressure again.
        // Natural decay is intentionally ignored.
        const int currentSignal = signals[activeSensor];

        if (currentSignal > peak[activeSensor] + PEAK_UPDATE_MARGIN) {
            peak[activeSensor] = currentSignal;

            const int newPressure = peakToPressure(peak[activeSensor]);
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
