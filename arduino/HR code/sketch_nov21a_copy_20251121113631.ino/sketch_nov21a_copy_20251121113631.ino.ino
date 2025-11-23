#include <PulseSensorPlayground.h>
#include <SoftwareSerial.h>


unsigned long lastSendTime = 0;
const unsigned long SEND_INTERVAL = 5000; // 5 seconds interval wait


const int START_BYTE = 0xA5; // Standard Start Byte Signal
const int ACK_BYTE = 0xB5; // Standard ACK Byte Signal
bool activated = false; // activation flag
char msg[90];
int bpm_temp = 50;
// --- ArduinoID ---
const uint8_t ID0 = 1;
const uint8_t ID1 = 0;
// ----------------- XBee Serial -------------------
SoftwareSerial mySerial(10, 11);  // RX=10 ← XBee TX, TX=11 → XBee RX


// ----------------- Pulse Sensor ------------------
const int PULSE_PIN = A0;
PulseSensorPlayground pulseSensor;


void initialize() {
  if (!activated) {
    // receive start byte
    digitalWrite(13, 1);  // for debugging
    while (mySerial.read() != START_BYTE)
      ;                   //BLOCKING START
    digitalWrite(13, 0);  // for debugging
    activated = true;
    mySerial.write(ACK_BYTE);  //go activate the handler
    sprintf(msg, "[ARDUINO %d%d] is active\n", ID1, ID0);
    mySerial.print(msg);
  }
}


void setup() {
  // Start XBee serial
  mySerial.begin(9600);


  // Debug LED
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH);


  // Pulse sensor setup
  pulseSensor.analogInput(PULSE_PIN);
  pulseSensor.setThreshold(550); // Tune if needed


  if (!pulseSensor.begin()) {
    mySerial.println("ERROR: PulseSensor initialization failed");
    while(1);
  }


}


int encode_HR(int bpm) {
    if (bpm > 100) return 3;
    else if (bpm >= 60) return 2;  // 60s + 70s
    else if (bpm >= 20) return 1;
    else return 1;
}


void loop() {
  initialize();
  // Always update sensor
  pulseSensor.sawNewSample();


  // If a beat was found, update bpm_temp
  if (pulseSensor.sawStartOfBeat()) {
    bpm_temp = pulseSensor.getBeatsPerMinute();
  }


  // --- Throttle sending to every 5 seconds ---
  unsigned long now = millis();
  if (now - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = now;


    int encoded = encode_HR(bpm_temp);
    mySerial.write(((ID1 << 7) | (ID0 << 6) | (encoded & 0x07)));


    // optional debug:
    Serial.print("Sending BPM: ");
    Serial.println(bpm_temp);
  }
}
