# Home Security System - Technical Specification

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Hardware Specifications](#hardware-specifications)
3. [Communication Protocols](#communication-protocols)
4. [Firmware Architecture](#firmware-architecture)
5. [Android Application Architecture](#android-application-architecture)
6. [Backend Service Architecture](#backend-service-architecture)
7. [State Machines](#state-machines)
8. [Timing Diagrams](#timing-diagrams)

---

## System Architecture

### Network Topology

```
                    Internet
                       │
                       │ MQTT (TCP/IP)
                       │
              ┌────────┴────────┐
              │  MQTT Broker    │
              │  (Cloud)        │
              └────────┬────────┘
                       │
                       │ Mobile Data
                       │
        ┌──────────────┴──────────────┐
        │                             │
   [Personal Phone]            [Digital Ocean]
   MQTT Client                 Spring Boot API
   SMS Sender                  Image Storage
        │                             │
        │ SMS                         │ HTTPS
        │                             │
        └──────────┬──────────────────┘
                   │
            [Old Android Phone]
         192.168.43.1 (Hotspot)
         ├─ nanoHTTPD (Port 8080)
         ├─ MQTT Client
         └─ SMS Receiver
                   │
        ┌──────────┴─────────┬───────────────┐
        │                    │               │
   WiFi (HTTP/UDP)      WiFi (HTTP)    Power Control
        │                    │               │
  [Main Arduino]      [ESP32-CAM]      [Relay Module]
  + ESP-01 WiFi                              │
        │                                    │
    Pin Comm                            ┌────┴────┐
        │                               │         │
  [Lock Arduino]                   [Buzzer]  [Light]
  + H-Bridge
        │
  [Linear Actuator]
```

### Component Communication Matrix

| From | To | Protocol | Data Type | Frequency |
|------|-----|----------|-----------|-----------|
| Main Arduino | Old Phone | HTTP POST | Sensor data | Every 10s (configurable) |
| Old Phone | Main Arduino | UDP | Commands | On demand |
| ESP32-CAM | Old Phone | HTTP POST | Motion alerts, pings | On motion, every 10s |
| Old Phone | ESP32-CAM | HTTP GET | Capture commands | On motion event |
| Old Phone | Personal Phone | MQTT | Status, alerts | Real-time |
| Personal Phone | Old Phone | MQTT, SMS | Commands, config | On user action |
| Old Phone | Digital Ocean | HTTP POST | Images | On capture |
| Main Arduino | Lock Arduino | Digital Pins | Lock/unlock commands | On demand |
| Lock Arduino | Main Arduino | Digital Pins | Status feedback | Continuous |

---

## Hardware Specifications

### Main Arduino Controller

#### Microcontroller
- **Model**: Arduino Nano (ATmega328P)
- **Clock Speed**: 16 MHz
- **Flash Memory**: 32 KB (0.5 KB used by bootloader)
- **SRAM**: 2 KB
- **EEPROM**: 1 KB
- **Operating Voltage**: 5V
- **Input Voltage**: 7-12V (via Vin)

#### Pin Configuration Detail

| Pin | Type | Function | Connection | Notes |
|-----|------|----------|------------|-------|
| D2 | Output | DC_POWER_PIN | Relay CH1 | Controls power to Lock Arduino |
| D3 | Input (Pull-up) | PAD_DOOR_PIN | Reed switch (padlock) | LOW when closed |
| D4 | Output | DOOR_LIGHT_CONTROL_PIN | Relay CH2 | Controls outdoor light |
| D5 | Input | MOTION_SENSOR_PIN | (Reserved) | Not actively used |
| D6 | TX | WIFI_TX_PIN | ESP-01 RX | Software serial 19200 baud |
| D7 | RX | WIFI_RX_PIN | ESP-01 TX | Software serial 19200 baud |
| D8 | Output | ALARM_CONTROL_PIN | Relay CH3 | Active LOW for buzzer |
| D9 | Output | CAMERA_CONTROL_PIN | Relay CH4 | Controls camera power |
| D10 | Input | DOOR_STATUS_PIN | Lock Arduino D10 | HIGH=door unlocked |
| D11 | Input | DC_STATUS_PIN | Lock Arduino D11 | HIGH=Lock Arduino powered |
| D12 | Output | DC_CONTROL_PIN | Lock Arduino D12 | Pulse HIGH to send command |

#### ESP-01 WiFi Module
- **Chip**: ESP8266
- **Flash**: 1 MB
- **Frequency**: 2.4 GHz (802.11 b/g/n)
- **Operating Voltage**: 3.3V
- **Current**: 80mA average, 170mA peak
- **Serial Baud Rate**: 19200 (configured)
- **Connection**: TX→D7, RX→D6, VCC→3.3V, GND→GND

**WiFi Configuration**:
```cpp
SSID: "<Wifi Name>"
Password: "<Wifi password>"
Mode: Station (STA)
Server IP: 192.168.43.1
Server Port: 8080
UDP Listen Port: 8085
```

#### Relay Module
- **Type**: 4-channel relay module
- **Trigger**: Active LOW (5V to GPIO)
- **Load Rating**: 10A 250VAC / 10A 30VDC
- **Connections**:
  - CH1: Lock Arduino power (12V line)
  - CH2: Outdoor light (110V/220V AC)
  - CH3: Buzzer (5V/12V DC)
  - CH4: Camera power (5V DC)

### Lock Controller Arduino

#### Microcontroller
- **Model**: Arduino Nano (ATmega328P)
- Specifications same as Main Controller

#### Pin Configuration Detail

| Pin | Type | Function | Connection | Notes |
|-----|------|----------|------------|-------|
| D2 | Output | ACTUATOR_IN1 | H-Bridge IN1 | Forward direction |
| D3 | Output | ACTUATOR_IN2 | H-Bridge IN2 | Reverse direction |
| D4 | Input | BTN_CLOSE | Push button | Manual close trigger |
| D5 | Input | BTN_OPEN | Push button | Manual open trigger |
| D6 | Input | OPENED_PIN | Limit switch | HIGH when fully open |
| D7 | Input | DOOR_SENSOR_PIN | Reed switch | HIGH when door closed |
| D10 | Output | DOOR_STATUS_PIN | To Main Arduino | Current lock state |
| D11 | Output | DC_STATUS_PIN | To Main Arduino | Indicates powered on |
| D12 | Input | DC_CONTROL_PIN | From Main Arduino | Receives commands |

#### Linear Actuator
- **Type**: 12V DC linear actuator
- **Stroke Length**: ~50mm (estimated)
- **Force**: Sufficient for door lock mechanism
- **Operating Time**: 8 seconds (hardcoded delay)
- **Control**: H-bridge driver (L298N or similar)

#### H-Bridge Driver Module
- **Model**: L298N (or compatible)
- **Input Voltage**: 12V
- **Logic Voltage**: 5V
- **Max Current**: 2A per channel
- **Connections**:
  - IN1 → D2 (Arduino)
  - IN2 → D3 (Arduino)
  - OUT1, OUT2 → Linear Actuator
  - VCC → 12V supply
  - 5V out → Not used (Arduino powered separately)

### ESP32-CAM Module

#### Specifications
- **SoC**: ESP32-S (Dual-core Tensilica LX6, 240 MHz)
- **Flash**: 4 MB
- **PSRAM**: 4 MB (for large images)
- **WiFi**: 2.4 GHz 802.11 b/g/n
- **Camera**: OV2640 (2MP)
- **Operating Voltage**: 5V input (regulated to 3.3V onboard)

#### Pin Configuration

| Pin | Function | Connection | Notes |
|-----|----------|------------|-------|
| GPIO 13 | MOTION_PIN | PIR sensor output | HIGH on motion |
| GPIO 12 | NIGHT_VISION_PIN | LED control | HIGH to turn on |
| Standard camera pins | OV2640 interface | See camera_pins.h | AI-Thinker pinout |

#### Camera Configuration
```cpp
Frame Size: FRAMESIZE_SVGA (800x600)
Initial Frame Size: FRAMESIZE_QVGA (320x240)
JPEG Quality: 12 (0-63, lower is better)
Frame Buffer Count: 1
Pixel Format: PIXFORMAT_JPEG
XCLK Frequency: 20 MHz
```

#### HTTP Server Endpoints
| Endpoint | Method | Purpose | Response |
|----------|--------|---------|----------|
| / | GET | Web interface | HTML page |
| /capture | GET | Single image capture | JPEG image |
| /stream | GET | MJPEG video stream | Multipart JPEG |
| /status | GET | Camera status | JSON |
| /control | GET | Camera settings | JSON |

#### PIR Motion Sensor
- **Model**: HC-SR501 (or similar)
- **Operating Voltage**: 5V
- **Detection Range**: 3-7 meters
- **Detection Angle**: ~110 degrees
- **Output**: Digital HIGH on motion
- **Delay Time**: Configured on sensor module

### Power System

#### Power Supply
- **Input**: 220V AC
- **Output**: 12V DC, 5A (60W)
- **Connections**:
  - Main output → UPS input
  - Provides power for: Relay coils, Linear actuator, 12V devices

#### UPS Battery Backup
- **Type**: 12V sealed lead-acid
- **Capacity**: Estimated 5-10Ah (provides several hours backup)
- **Purpose**: Maintain operation during power outages
- **Load**: All system components

#### Voltage Regulation
- **12V to 5V**: Linear regulator (7805) or buck converter
- **12V to 3.3V**: For ESP-01 (via voltage regulator)
- **Current Draw Estimates**:
  - Main Arduino: ~50mA
  - Lock Arduino: ~50mA (when powered)
  - ESP-01: ~80mA average, 170mA peak
  - ESP32-CAM: ~200mA average, 500mA peak
  - Relay Module: ~70mA per channel (coil)
  - Linear Actuator: 1-2A (when moving)
  - Buzzer: ~30mA
  - **Total**: ~3A peak (within 5A supply rating)

### Sensors & Actuators

#### Reed Switches (x2)
- **Type**: Magnetic reed switch, NO (Normally Open)
- **Locations**:
  1. Door frame - Detects door open/closed
  2. Padlock cover - Detects tamper attempts
- **Wiring**: One terminal to GPIO, other to GND (with internal pull-up)

#### Push Button Switches
- **Type**: Momentary tactile switch
- **Debouncing**: Software debouncing in code
- **Usage**:
  - Manual lock/unlock buttons (Lock Arduino)
  - Limit switch to detect actuator fully open

#### Buzzer
- **Type**: Active buzzer (5V or 12V)
- **Control**: Via relay (NC terminal)
- **Trigger Condition**: PAD_DOOR_PIN HIGH AND alarm_armed_state HIGH
- **Timeout**: Configurable (default 2 seconds)

#### Outdoor Light
- **Type**: Standard AC bulb (110V/220V)
- **Control**: Via relay (NO terminal)
- **Purpose**: Deterrence and visibility
- **Manual Control**: Via MQTT/SMS commands

---

## Communication Protocols

### 1. Main Arduino ↔ Old Phone (HTTP/UDP)

#### HTTP POST - Status Update (Arduino → Phone)
**Frequency**: Every 10 seconds (configurable via `data_send_interval`)

**Request**:
```http
POST /data HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: <length>

<doorOpen>,<padlockOpen>,<dcState>,<alarmState>,<lightState>,<alarmArmed>
```

**Data Format**:
- `doorOpen`: 0 (closed) or 1 (open)
- `padlockOpen`: 0 (closed) or 1 (open)
- `dcState`: 0 (off), 1 (on), or actual pin reading
- `alarmState`: 0 (off) or 1 (on)
- `lightState`: 0 (off) or 1 (on)
- `alarmArmed`: 0 (disarmed) or 1 (armed)

**Example**: `0,0,1,0,0,1` = Door closed, padlock closed, DC powered, alarm off, light off, alarm armed

**Response Format**:
```
<commandList>:<configList>
```
- `commandList`: Comma-separated commands (e.g., `d0,al1,dl1`)
- `configList`: Comma-separated config values (see Configuration section)

**Example Response**: `d0,al0:30,10,1800,0,1,2`

#### UDP - Emergency Commands (Phone → Arduino)
**Port**: 8085  
**Usage**: Fast, low-latency commands (lock/unlock, alarm)

**Packet Format**:
```
<command>
```

**Commands**:
- `d0` - Unlock door
- `d1` - Lock door
- `dc0` - Turn off door controller power
- `dc1` - Turn on door controller power
- `al0` - Turn off alarm
- `al1` - Turn on alarm
- `al2` - Disable alarm (disarm)
- `al3` - Enable alarm (arm)
- `dl0` - Turn off door light
- `dl1` - Turn on door light
- `rst` - Reset controller
- `crst` - Reset camera
- `c0` - Turn off camera
- `c1` - Turn on camera

**Response**:
```
OK
```

### 2. ESP32-CAM ↔ Old Phone (HTTP)

#### Motion Alert
```http
POST /cam/motion HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 15

Motion detected
```

**Flow**:
1. ESP32 detects motion (PIR sensor HIGH)
2. Sends POST to /cam/motion
3. Turns on night vision LED
4. Phone receives alert, sends capture command to ESP32
5. ESP32 captures image and uploads to Digital Ocean

#### Heartbeat/Ping
```http
POST /cam/ping HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 4

Ping
```
**Frequency**: Every 10 seconds (configurable via `ping_interval`)

#### Boot Notification
```http
POST /cam/boot HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 14

Boot completed
```
Sent once on ESP32 startup.

### 3. Main Arduino ↔ Lock Arduino (Digital Pin Protocol)

#### Command Sequence (Main → Lock)

**Signal**: DC_CONTROL_PIN (D12 on both)

**Timing Diagram**:
```
Main Arduino DC_CONTROL_PIN:
         _______________________________
  ______|                               |____________
        ^                               ^
        |                               |
     Main sets HIGH                  Main detects & sets LOW
             
Lock Arduino DC_STATUS_PIN:
						Lock executes Open/Close
    _________                                ________
             |______________________________|
             ^                              ^
             |                              |
        Lock detects & sets LOW           Lock sets HIGH (Ready)
```

**Protocol Steps**:
1. Main Arduino sets DC_CONTROL_PIN HIGH
2. Lock Arduino detects HIGH on DC_CONTROL_PIN
3. Lock Arduino sets DC_STATUS_PIN LOW (acknowledge)
4. Lock Arduino waits for DC_CONTROL_PIN to go LOW
5. Main Arduino detects acknowledgment, sets DC_CONTROL_PIN LOW
6. Lock Arduino sets DC_STATUS_PIN HIGH (ready)
7. Lock Arduino executes open or close based on current state

**Code (Main Arduino)**:
```cpp
void sendOpenOrCloseCommand() {
    digitalWrite(DC_CONTROL_PIN, HIGH);
    while (digitalRead(DC_STATUS_PIN)) {
        // Wait for pin to turn LOW (acknowledge)
    }
    digitalWrite(DC_CONTROL_PIN, LOW); // Finish the command
    
    DoorStatus previousStatus = doorStatus;
    while(getDoorStatus() == previousStatus) {
        // Wait for the operation to complete
    }
}
```

**Code (Lock Arduino)**:
```cpp
void readControlPins() {
    if(digitalRead(DC_CONTROL_PIN)) {
        // Control PIN is HIGH - command received
        digitalWrite(DC_STATUS_PIN, LOW); // Acknowledge
        while(digitalRead(DC_CONTROL_PIN)) {
            // Wait for control pin to go low
        }
        digitalWrite(DC_STATUS_PIN, HIGH); // Ready
        
        // Execute action
        if(actuatorState == CLOSED) {
            open();
        } else {
            close();
        }
    }
}
```

#### Status Signals (Lock → Main)

**DC_STATUS_PIN** (D11):
- HIGH: Lock Controller is powered and operational
- LOW: Lock Controller is off or acknowledging command

**DOOR_STATUS_PIN** (D10):
- HIGH: Door lock is OPEN (actuator retracted)
- LOW: Door lock is CLOSED (actuator extended)

### 4. MQTT Protocol

#### Topic Structure
```
Home/<clientId>/<topic>
```
Where `<clientId>` is configured in the Android app.

#### Topics Detail

**Commands Topic** (Subscribe - Old Phone, Publish - Personal Phone)
```
Topic: Home/<clientId>/commands
QoS: 1 (At least once delivery)
Retained: false

Payload Format (JSON):
{
  "commands": ["d0", "al1", "dl1"]
}
```

**Config Topic** (Subscribe - Old Phone, Publish - Personal Phone)
```
Topic: Home/<clientId>/config
QoS: 1
Retained: true (last known configuration)

Payload Format (JSON):
{
  "lockTimeout": 30,
  "dataInterval": 10,
  "resetInterval": 1800,
  "dcPowerState": 0,
  "alarmArmed": 1,
  "alarmTimeout": 2
}
```

**Data Topic** (Publish - Old Phone, Subscribe - Personal Phone)
```
Topic: Home/<clientId>/data
QoS: 0 (Fire and forget)
Retained: false

Payload Format (JSON):
{
  "doorOpen": 0,
  "padlockOpen": 0,
  "dcState": 1,
  "alarmState": 0,
  "lightState": 0,
  "alarmArmed": 1,
  "timestamp": 1644678900
}
```

**Motion Topic** (Publish - Old Phone, Subscribe - Personal Phone)
```
Topic: Home/<clientId>/motion
QoS: 1
Retained: false

Payload Format (JSON):
{
  "detected": true,
  "timestamp": 1644678900,
  "imageUrl": "https://server.com/images/xyz.jpg"
}
```

**Device Info Topic** (Publish - Old Phone)
```
Topic: Home/<clientId>/device
QoS: 0
Retained: true

Payload Format (JSON):
{
  "batteryLevel": 85,
  "charging": false,
  "wifiConnected": true,
  "mqttConnected": true,
  "timestamp": 1644678900
}
```

**SMS Topic** (Publish - Old Phone)
```
Topic: Home/<clientId>/sms
QoS: 1
Retained: false

Payload Format (JSON):
{
  "from": "+1234567890",
  "command": "d0",
  "result": "success",
  "timestamp": 1644678900
}
```

### 5. ESP32-CAM ↔ Digital Ocean (HTTP)

#### Image Upload
```http
POST /upload HTTP/1.1
Host: <server-domain>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
Authorization: Bearer <token>

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="capture.jpg"
Content-Type: image/jpeg

<binary image data>
------WebKitFormBoundary--
```

**Response**:
```json
{
  "id": "abc123",
  "url": "https://server.com/files/abc123",
  "timestamp": "2024-02-12T10:30:00Z"
}
```

---

## Firmware Architecture

### Main Arduino Controller

#### State Variables
```cpp
// Configuration (remotely updatable)
unsigned int lock_timeout = 30;           // Seconds before auto-lock
unsigned int data_send_interval = 10;     // Status update interval
unsigned long reset_interval = 60 * 30;   // Controller auto-reset (30 min)
unsigned int alarm_turn_off_timeout = 2;  // Alarm duration

// State tracking
int dc_power_state = LOW;                 // Door controller power state
int alarm_state = LOW;                    // Buzzer on/off
int door_light_state = LOW;               // Light on/off
int alarm_armed_state = HIGH;             // Alarm armed/disarmed
DoorStatus doorStatus = DS_CLOSED;        // Door lock status

// Timers
unsigned long last_sent_millis = 0;
unsigned long alarm_turned_on_millis = 0;
```

#### Main Loop Flow
```
┌─────────────────────────────────────┐
│  Initialize Serial, WiFi, UDP       │
│  Set pin modes                      │
│  Power on camera                    │
│  Set initial states                 │
└─────────────┬───────────────────────┘
              │
              ▼
      ┌───────────────┐
      │   Main Loop   │
      └───────┬───────┘
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
┌───────────┐    ┌────────────────┐
│ UDP       │    │ Check Alarm    │
│ Listen    │    │ (if armed)     │
└───────────┘    └────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │ Data Send Interval? │
              │ (every 10s)         │
              └──────────┬──────────┘
                         │ Yes
                         ▼
              ┌─────────────────────┐
              │ Send sensor data    │
              │ Execute commands    │
              │ Update config       │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │ Reset interval?     │
              │ (every 30 min)      │
              └──────────┬──────────┘
                         │ Yes
                         ▼
                   controllerReset()
```

#### Key Functions

**sendData()**
1. Read sensor states (door, padlock)
2. Get DC state and door status
3. Format data string: `doorOpen,padlockOpen,dcState,alarmState,lightState,alarmArmed`
4. Send HTTP POST to server
5. Parse response: `commands:config`
6. Execute commands via executeCommands()
7. Update config via updateConfig()

**executeCommand(String command)**
```cpp
Supported commands:
  - dc0: turnOffDoorControllerPower()
  - dc1: turnOnDoorControllerPower()
  - d0: unlockDoor()
  - d1: lockDoor()
  - al0: turnOffAlarm()
  - al1: turnOnAlarm()
  - al2: disableAlarm() (disarm)
  - al3: enableAlarm() (arm)
  - dl0: turnOffDoorLight()
  - dl1: turnOnDoorLight()
  - rst: controllerReset()
  - crst: resetCamera()
  - c0: turnOffCamera()
  - c1: turnOnCamera()
```

**unlockDoor() / lockDoor()**
1. Power on Lock Controller via turnOnDoorControllerPower()
2. Check current door status
3. If status matches desired state, return
4. Call sendOpenOrCloseCommand() to trigger Lock Arduino
5. Wait for operation to complete (status change)

**checkDoorPad()**
- Called every loop if alarm is armed
- If PAD_DOOR_PIN is HIGH (padlock opened): Turn on alarm
- Start timer
- After timeout (default 2s): Turn off alarm

### Lock Controller Arduino

#### State Machine

```
        Power On
           │
           ▼
    ┌──────────────┐
    │  Initialize  │
    │  Check Limit │
    │  Switch      │
    └──────┬───────┘
           │
           ▼
      ┌─────────┐
      │  IDLE   │◄─────────────┐
      └────┬────┘              │
           │                   │
    ┌──────┴──────┬────────────┴─────┬──────────┐
    │             │                  │          │
    ▼             ▼                  ▼          ▼
[Button]   [Main Arduino]    [Door Closed]  [Auto
 Press]     Command           & Timer         Close]
    │             │                  │          │
    ▼             ▼                  ▼          │
 Execute       Execute         Start Timer      │
  open/        open/                │           │
  close        close                ▼           │
    │             │           Wait 30s          │
    └─────────────┴────────────┬────────────────┘
                               │
                               ▼
                            close()
```

#### Actuator States
```cpp
enum ActuatorState {
    OPEN,    // Actuator retracted, door can be locked
    CLOSED   // Actuator extended, door is locked
};
```

#### Key Functions

**runLinearActuator()**
```
Check buttons:
  - BTN_CLOSE pressed & OPEN → close()
  - BTN_OPEN pressed & CLOSED → open()

Check auto-close logic:
  - If OPEN && door sensor HIGH && timer not started:
      Start timer (door_closed_millis)
  - If door opened: Reset timer
  - If timer >= 30 seconds: close()

Always call turnOff() at end of loop
```

**open()**
```
1. Check if already fully open (OPENED_PIN HIGH)
2. If not:
   - Set ACTUATOR_IN1 LOW, ACTUATOR_IN2 HIGH (reverse)
   - Poll OPENED_PIN until HIGH (limit switch triggered)
   - Delay 200ms between polls
3. Set state to OPEN
4. Update DOOR_STATUS_PIN HIGH
5. Call turnOff()
```

**close()**
```
1. Set ACTUATOR_IN1 HIGH, ACTUATOR_IN2 LOW (forward)
2. Delay 8000ms (8 seconds - hardcoded timeout)
3. Set state to CLOSED
4. Update DOOR_STATUS_PIN LOW
5. Reset timer (door_closed_millis = 0)
6. Call turnOff()
```

**Timing**: 
- Open operation: Variable (until limit switch, typically 5-8 seconds)
- Close operation: Fixed 8 seconds (no limit switch on closed position)

### ESP32-CAM Firmware

#### Main Loop Flow
```
Setup:
  - Initialize camera (SVGA, quality 12)
  - Connect to WiFi
  - Start HTTP server (/capture, /stream, etc.)
  - Configure motion sensor pin
  - Send boot notification

Loop:
  ├─ Check WiFi connection
  │
  ├─ Motion detection check (if interval elapsed):
  │  └─ If motion detected:
  │     ├─ Send /cam/motion to phone
  │     ├─ Turn on night vision LED
  │     └─ Start night vision timer
  │
  ├─ Night vision timeout check:
  │  └─ If timeout elapsed: Turn off LED
  │
  └─ Ping interval check (every 10s):
     └─ Send /cam/ping to phone
```

#### HTTP Handlers

**capture_handler()**
1. Capture frame: `esp_camera_fb_get()`
2. If face detection disabled or large image:
   - Send JPEG directly
3. If face detection enabled:
   - Convert to RGB888
   - Run face detection: `face_detect()`
   - Optionally run face recognition
   - Draw bounding boxes
   - Convert back to JPEG
4. Return image to client

**stream_handler()**
- Continuous MJPEG stream
- Capture frame in loop
- Send with multipart boundary
- Calculate FPS and timing stats
- Log performance metrics

**cmd_handler()**
- Parse query parameters (var, val)
- Update camera settings:
  - framesize, quality, contrast, brightness
  - saturation, special effects, etc.
  - face_detect, face_recognize, face_enroll

#### Night Vision Control
```cpp
Motion detected:
  - digitalWrite(NIGHT_VISION_PIN, HIGH)
  - nv_turned_on_millis = millis()

Loop check:
  - If (millis() - nv_turned_on_millis >= night_vision_timeout * 1000):
      - digitalWrite(NIGHT_VISION_PIN, LOW)
```

---

## Android Application Architecture

### Home Client App (Old Phone - Server)

#### Application Components

**Services**:
- `MainControlService`: Foreground service, coordinates all activities
- `AsyncService`: Background task executor

**MQTT Client**:
- `MqttService`: Singleton, manages MQTT connection
- Topic classes: `CommandsTopic`, `ConfigTopic`, `DataTopic`, `MotionTopic`, `SmsTopic`

**HTTP Server** (nanoHTTPD):
- Embedded in MainControlService
- Endpoints:
  - `POST /data` - Receive Arduino sensor data
  - `POST /cam/motion` - Motion alert from ESP32
  - `POST /cam/ping` - Camera heartbeat
  - `POST /cam/boot` - Camera startup

**Broadcast Receivers**:
- `BootCompletedReceiver`: Auto-start service on boot
- `SmsReceiver`: Process SMS commands

**Database**:
- ObjectBox: Local storage for logs, settings, captured data

#### Data Flow

**Incoming Data (Arduino → Phone)**:
```
HTTP POST /data
  ↓
MainControlService (nanoHTTPD handler)
  ↓
Parse data: doorOpen, padlockOpen, dcState, etc.
  ↓
Store in ObjectBox (optional)
  ↓
Publish to MQTT (DataTopic)
  ↓
Check for pending commands
  ↓
Return: commands:config
```

**Outgoing Commands (Phone → Arduino)**:
```
MQTT CommandsTopic (received)
  ↓
MainControlService (MQTT callback)
  ↓
Parse commands
  ↓
Send UDP packet to Arduino (192.168.43.x:8085)
  ↓
Wait for "OK" response
```

**SMS Command Processing**:
```
SMS received
  ↓
SmsReceiver.onReceive()
  ↓
Parse command from SMS body
  ↓
Send to Arduino (UDP or HTTP)
  ↓
Publish result to SmsTopic (MQTT)
  ↓
(Optional) Send SMS reply
```

#### Configuration Management
```java
LocalPropertyHelper.getMqttServerIp()
LocalPropertyHelper.getMqttServerPort()
LocalPropertyHelper.getMqttClientId()
LocalPropertyHelper.getArduinoIp()
// etc.
```
Stored in SharedPreferences or ObjectBox.

### Home Security App (Personal Phone - Client)

#### Application Components

**Activities**:
- `MainActivity`: Main UI, displays status, control buttons
- `SettingsActivity`: Configure MQTT, Arduino IPs, etc.
- `ImageViewActivity`: View captured images

**Services**:
- `CameraHandlerService`: Manage camera captures
- `ControllerHandlerService`: Send commands to Arduino

**MQTT Client**:
- Subscribe to: `DataTopic`, `MotionTopic`, `DeviceInfoTopic`
- Publish to: `CommandsTopic`, `ConfigTopic`

**Broadcast Receivers**:
- `SmsReceiver`: Send SMS commands as fallback
- `BootCompletedReceiver`: Auto-start on boot

**UI Features**:
- Real-time status display (door, padlock, alarm, light)
- Lock/Unlock button
- Arm/Disarm alarm toggle
- Light on/off toggle
- View captured images (from Digital Ocean)
- Connection status indicators

#### Command Flow
```
User taps "Unlock Door" button
  ↓
MainActivity.onClick()
  ↓
MqttService.publish(CommandsTopic, "d0")
  ↓
(If MQTT fails, fallback to SMS)
  ↓
Wait for status update via DataTopic
  ↓
Update UI when doorOpen = 1
```

---

## Backend Service Architecture

### Spring Boot Application

#### Layers

**Controllers**:
```java
@RestController
@RequestMapping("/files")
public class FileController {
    @PostMapping("/upload")
    public ResponseEntity<FileDescriptor> uploadFile(@RequestParam("file") MultipartFile file);
    
    @GetMapping("/{id}")
    public ResponseEntity<FileDownload> getFile(@PathVariable String id);
    
    @GetMapping
    public ResponseEntity<List<FileDescriptor>> listFiles();
}
```

**Services**:
```java
public interface StorageService {
    FileDescriptor storeFile(MultipartFile file);
    FileDownload getFile(String fileId);
    List<FileDescriptor> listFiles();
    void deleteFile(String fileId);
}
```

**Storage Drivers**:
- `AwsS3DriverImpl`: AWS S3 integration
- `GcpDriverImpl`: Google Cloud Storage
- `LocalStorageDriverImpl`: Local filesystem

**Repositories**:
```java
public interface FileDescriptorRepository extends JpaRepository<FileDescriptor, Long> {
    Optional<FileDescriptor> findByFileId(String fileId);
}
```

**Entities**:
```java
@Entity
public class FileDescriptor {
    @Id
    @GeneratedValue
    private Long id;
    
    private String fileId;          // Unique identifier
    private String fileName;         // Original filename
    private String contentType;      // MIME type
    private Long fileSize;          // Size in bytes
    private String storageLocation;  // S3 key or file path
    private LocalDateTime uploadedAt;
}
```

#### Configuration
```yaml
# application.properties
storage.type=s3  # or gcp, local
storage.s3.bucket=home-security-images
storage.s3.region=us-east-1
storage.local.path=/var/lib/home-security/images

spring.datasource.url=jdbc:postgresql://localhost:5432/homesecurity
spring.datasource.username=postgres
spring.datasource.password=<password>
```

---

## State Machines

### Main Arduino State Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     MAIN CONTROLLER                         │
│                                                             │
│  ┌───────────┐  Command: d0    ┌────────────┐               │
│  │   Door    ├────────────────►│   Door     │               │
│  │  Locked   │                 │  Unlocking │               │
│  │           │◄────────────────┤            │               │
│  └─────┬─────┘  Auto-close     └──────┬─────┘               │
│        │        (30s timer)           │ Status change       │
│        │                              │                     │
│        │                              ▼                     │
│        │                        ┌────────────┐              │
│        │                        │   Door     │              │
│        └────────────────────────┤  Unlocked  │              │
│                Command: d1      └────────────┘              │
│                                                             │
│  ┌───────────┐  Pad opened      ┌────────────┐              │
│  │  Alarm    ├───& armed───────►│   Alarm    │              │
│  │   OFF     │                  │    ON      │              │
│  │           │◄─────────────────┤            │              │
│  └───────────┘  Timeout (2s)    └────────────┘              │
│                 or al0 command                              │
│                                                             │
│  ┌───────────┐  al3 command    ┌────────────┐               │
│  │  Alarm    ├────────────────►│   Alarm    │               │
│  │ Disarmed  │                 │   Armed    │               │
│  │           │◄────────────────┤            │               │
│  └───────────┘  al2 command    └────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### Lock Arduino State Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    LOCK CONTROLLER                          │
│                                                             │
│        ┌──────────┐                                         │
│    ┌───┤  CLOSED  │◄──┐                                     │
│    │   └────┬─────┘   │                                     │
│    │        │         │                                     │
│    │ open() │         │ close()                             │
│    │        │         │                                     │
│    │        ▼         │                                     │
│    │   ┌──────────┐   │                                     │
│    │   │ OPENING  ├───┤                                     │
│    │   └────┬─────┘   │                                     │
│    │        │         │                                     │
│    │        │ Limit   │                                     │
│    │        │ switch  │                                     │
│    │        │ HIGH    │                                     │
│    │        ▼         │                                     │
│    │   ┌──────────┐   │                                     │
│    └──►│   OPEN   ├───┘                                     │
│        └────┬─────┘                                         │
│             │                                               │
│             │ Door closed                                   │
│             │ && 30s elapsed                                │
│             ▼                                               │
│        ┌──────────┐                                         │
│        │ CLOSING  │                                         │
│        └────┬─────┘                                         │
│             │                                               │
│             │ 8s timeout                                    │
│             └─────────────────────────────────────┐         │
│                                                   │         │
└───────────────────────────────────────────────────┼─────────┘
                                                    │
                                               Back to CLOSED
```

---
