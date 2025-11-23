// --- Button pins ---
const int BTN0_PIN = 6;  // BTN0
const int BTN1_PIN = 7;  // BTN1
const int BTN2_PIN = 8;  // BTN2 (trigger)
const int PIN_R = 2;
const int PIN_G = 4;
const int PIN_B = 3;
const int PIN_BUZZER = 9;

// CONSTANTS OUTSIDE OF PROTOCOL FORMAT
const int START_BYTE = 0xA5;
const int ACK_BYTE = 0xB5;

// --- ArduinoID ---
const uint8_t ID0 = 1;
const uint8_t ID1 = 1;

// Status and control variables
bool activated = false;
char msg[90];

void ledOff() {
    digitalWrite(PIN_R, HIGH);
    digitalWrite(PIN_G, HIGH);
    digitalWrite(PIN_B, HIGH);
}

void setColor(int r_on, int g_on, int b_on) {
    digitalWrite(PIN_R, r_on ? LOW : HIGH);
    digitalWrite(PIN_G, g_on ? LOW : HIGH);
    digitalWrite(PIN_B, b_on ? LOW : HIGH);
}

// Convenience colors:
void red()    { setColor(0,1,0); }
void green()  { setColor(0,0,1); }


void setup() {
  pinMode(BTN0_PIN, INPUT);
  pinMode(BTN1_PIN, INPUT);
  pinMode(BTN2_PIN, INPUT);
  pinMode(PIN_R, OUTPUT);
  pinMode(PIN_G, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(13, OUTPUT);
  Serial.begin(9600);
}

void beep(int freq, int duration_ms) {
  tone(PIN_BUZZER, freq, duration_ms);
  delay(duration_ms);     // keep it dead simple / blocking
  noTone(PIN_BUZZER);
}


void initialize() {
  if (!activated) {
    red();
    // receive start byte
    digitalWrite(13, 1);  // for debugging
    while (Serial.read() != START_BYTE)
      ;                   //BLOCKING START
    digitalWrite(13, 0);  // for debugging
    activated = true;

    beep(500, 120); 
    delay(150);
    ledOff();

    Serial.write(ACK_BYTE);  //go activate the handler
    sprintf(msg, "[ARDUINO %d%d] is active\n", ID1, ID0);
    Serial.print(msg);
  }
  green();  
}

int buttonEncoder(bool b0, bool b1, bool b2) {
  int encoded = -1;  //start negative
  /*
  btn1 btn0
  0 0 -> b0
  0 1 -> b1
  1 0 -> b2
  11 unused
  */
  if (b0) return 0;
  if (b1) return 1;
  if (b2) return 2;
  return -1;
}

void ButtonsHandler() {
  bool b0 = digitalRead(BTN0_PIN);
  bool b1 = digitalRead(BTN1_PIN);
  bool b2 = digitalRead(BTN2_PIN);
  int encoded_packet = buttonEncoder(b0, b1, b2);
  if (encoded_packet != -1) {  // if any of the buttons is pressed -> send packet
    tone(PIN_BUZZER, 500);
    delay(500);
    noTone(PIN_BUZZER);
    // Construct byte: [ID1][ID0][0][0][0][0][BTN1][BTN0]
    uint8_t packet = (ID1 << 7) | (ID0 << 6) | (encoded_packet << 0);
    Serial.write(packet);  //send packet to user
    delay(500);            // Simple debounce
  }
}



void loop() {
  // WAIT for start byte from the computer, format is [0][0][1][1][1][0][1][0] 0x3A
  initialize();
  ButtonsHandler();
}
