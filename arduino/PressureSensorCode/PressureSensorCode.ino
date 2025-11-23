const int numPads = 4;
const int sensorPins[numPads] = {A0, A1, A2, A3};
const int PRESSURE_THRESHOLD = 600;  // absolute pressure threshold (0–1023)
bool padStates[numPads] = {false, false, false, false};
const int START_BYTE = 0xA5;
const int ACK_BYTE = 0xB5;


// --- ArduinoID ---
const uint8_t ID0 = 0;
const uint8_t ID1 = 1;


unsigned long lastReadTime = 0;
const int readInterval = 10;
bool activated = false;
char msg[90];

void setup() {
  Serial.begin(9600);
}

void initialize() {
  if (!activated) {
    // receive start byte
    digitalWrite(13, 1);  // for debugging
    while (Serial.read() != START_BYTE)
      ;                   //BLOCKING START
    digitalWrite(13, 0);  // for debugging
    activated = true;
    Serial.write(ACK_BYTE);  //go activate the handler
    sprintf(msg, "[ARDUINO %d%d] is active\n", ID1, ID0);
    Serial.print(msg);
  }
}

void pressureSensorHandler() {
  unsigned long currentTime = millis();

  if (currentTime - lastReadTime >= readInterval) {
    lastReadTime = currentTime;

    for (int i = 0; i < numPads; i++) {
      int value = analogRead(sensorPins[i]);

      if (!padStates[i] && value > PRESSURE_THRESHOLD) {
        padStates[i] = true;
        Serial.write(((ID1 << 7) | (ID0 << 6) | ((i) & 0x07)));
      }

      // Reset when released (value drops below threshold - 100)
      if (padStates[i] && value < PRESSURE_THRESHOLD - 100) {
        padStates[i] = false;
      }
    }
  }
}
void loop() {
  initialize();
  pressureSensorHandler();
}