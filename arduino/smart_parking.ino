#include <Servo.h>
#include <LiquidCrystal.h>

// ========================
// PIN ASSIGNMENTS
// ========================
#define SERVO_PIN      9    // Gate barrier servo motor
#define IR_SENSOR_PIN  2    // IR sensor (vehicle detection at gate)
#define GREEN_LED      3    // Entry approved indicator
#define RED_LED        4    // Entry denied indicator
#define BUZZER_PIN     5    // Buzzer for audio feedback
#define TRIG_PIN       6    // Ultrasonic sensor trigger (parking spot)
#define ECHO_PIN       7    // Ultrasonic sensor echo (parking spot)

// LCD pins (4-bit mode): RS=8, EN=10, D4=11, D5=12, D6=13, D7=A0
LiquidCrystal lcd(8, 10, 11, 12, 13, A0);
Servo gateServo;

// ========================
// STATE
// ========================
bool gateOpen = false;
bool lastIRState = HIGH;
unsigned long lastSensorSend = 0;
const unsigned long SENSOR_INTERVAL = 2000; // Send sensor data every 2 seconds

// ========================
// SETUP
// ========================
void setup() {
  Serial.begin(9600);

  // Servo setup
  gateServo.attach(SERVO_PIN);
  gateServo.write(0); // Gate closed position

  // Pin modes
  pinMode(IR_SENSOR_PIN, INPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  // LCD setup
  lcd.begin(16, 2);
  lcd.print("Smart Parking");
  lcd.setCursor(0, 1);
  lcd.print("System Ready");

  // Default state: red LED on (gate closed)
  digitalWrite(RED_LED, HIGH);
  digitalWrite(GREEN_LED, LOW);

  Serial.println("READY");
}

// ========================
// MAIN LOOP
// ========================
void loop() {
  // 1. Check IR sensor for vehicle arrival
  checkIRSensor();

  // 2. Periodically read and send ultrasonic sensor data
  if (millis() - lastSensorSend > SENSOR_INTERVAL) {
    sendSensorData();
    lastSensorSend = millis();
  }

  // 3. Process incoming serial commands from backend
  processSerialCommands();
}

// ========================
// IR SENSOR (Vehicle Detection)
// ========================
void checkIRSensor() {
  int irState = digitalRead(IR_SENSOR_PIN);

  // Detect rising edge (vehicle arrives)
  if (irState == LOW && lastIRState == HIGH) {
    Serial.println("IR:DETECTED");

    // Show detection on LCD
    lcd.clear();
    lcd.print("Vehicle");
    lcd.setCursor(0, 1);
    lcd.print("Detected!");

    // Short beep to acknowledge
    tone(BUZZER_PIN, 1000, 100);

    delay(500); // Debounce
  }

  lastIRState = irState;
}

// ========================
// ULTRASONIC SENSOR (Spot Occupancy)
// ========================
long readUltrasonic() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  long duration = pulseIn(ECHO_PIN, HIGH, 30000);
  if (duration == 0) return 999; // No echo = no object
  return duration * 0.034 / 2; // Convert to cm
}

void sendSensorData() {
  long distance = readUltrasonic();
  bool occupied = (distance < 10); // Less than 10cm = car present

  // Send data to backend
  Serial.print("ULTRA:");
  Serial.println(distance);
  Serial.print("SPOT:");
  Serial.println(occupied ? "occupied" : "free");
}

// ========================
// SERIAL COMMAND PROCESSING
// ========================
void processSerialCommands() {
  if (Serial.available() == 0) return;

  String cmd = Serial.readStringUntil('\n');
  cmd.trim();

  if (cmd == "OPEN") {
    openGate();
  }
  else if (cmd == "DENY") {
    denyEntry();
  }
  else if (cmd.startsWith("LCD:")) {
    updateLCD(cmd.substring(4));
  }
  else if (cmd.startsWith("BUZZ:")) {
    int duration = cmd.substring(5).toInt();
    tone(BUZZER_PIN, 1000, duration);
  }
  else if (cmd == "STATUS") {
    // Report current state
    Serial.print("GATE:");
    Serial.println(gateOpen ? "open" : "closed");
    Serial.print("ULTRA:");
    Serial.println(readUltrasonic());
  }
}

// ========================
// GATE CONTROL
// ========================
void openGate() {
  gateOpen = true;

  // Visual feedback
  lcd.clear();
  lcd.print("Welcome!");
  lcd.setCursor(0, 1);
  lcd.print("Gate Opening...");

  // LEDs
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(RED_LED, LOW);

  // Success beep (two short beeps)
  tone(BUZZER_PIN, 1500, 150);
  delay(200);
  tone(BUZZER_PIN, 2000, 150);

  // Open gate (servo to 90 degrees)
  for (int pos = 0; pos <= 90; pos += 5) {
    gateServo.write(pos);
    delay(30);
  }

  // Keep gate open for 5 seconds
  delay(5000);

  // Close gate
  for (int pos = 90; pos >= 0; pos -= 5) {
    gateServo.write(pos);
    delay(30);
  }

  // Reset indicators
  digitalWrite(GREEN_LED, LOW);
  digitalWrite(RED_LED, HIGH);
  gateOpen = false;

  // Reset LCD
  lcd.clear();
  lcd.print("Smart Parking");
  lcd.setCursor(0, 1);
  lcd.print("System Ready");

  Serial.println("GATE:closed");
}

void denyEntry() {
  // Visual feedback
  lcd.clear();
  lcd.print("Access Denied!");
  lcd.setCursor(0, 1);
  lcd.print("Contact Admin");

  // LEDs: flash red
  digitalWrite(GREEN_LED, LOW);

  // Error buzzer: three short buzzes
  for (int i = 0; i < 3; i++) {
    digitalWrite(RED_LED, HIGH);
    tone(BUZZER_PIN, 500, 200);
    delay(300);
    digitalWrite(RED_LED, LOW);
    delay(100);
  }

  // Keep message for 3 seconds
  digitalWrite(RED_LED, HIGH);
  delay(3000);

  // Reset LCD
  lcd.clear();
  lcd.print("Smart Parking");
  lcd.setCursor(0, 1);
  lcd.print("System Ready");

  Serial.println("GATE:denied");
}

// ========================
// LCD UPDATE
// ========================
void updateLCD(String data) {
  int sep = data.indexOf('|');
  lcd.clear();

  if (sep >= 0) {
    // Two lines separated by |
    String line1 = data.substring(0, sep);
    String line2 = data.substring(sep + 1);

    // Truncate to 16 chars
    if (line1.length() > 16) line1 = line1.substring(0, 16);
    if (line2.length() > 16) line2 = line2.substring(0, 16);

    lcd.print(line1);
    lcd.setCursor(0, 1);
    lcd.print(line2);
  } else {
    // Single line
    if (data.length() > 16) data = data.substring(0, 16);
    lcd.print(data);
  }
}
