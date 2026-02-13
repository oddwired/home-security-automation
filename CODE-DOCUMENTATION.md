# Home Security System - Code Documentation Guide

## Overview

This document provides detailed code documentation for each component of the home security system, including function references, variable explanations, and code flow diagrams.

---

## Main Arduino Controller Code Documentation

### File: `main-arduino-controller/main_controller.ino`

#### Constants

```cpp
const int BAUD_RATE = 19200;                    // Serial communication speed
const char WIFI_SSID[] = "S_HOME";              // WiFi network name
const char WIFI_PASS[] = "3030031343dc";        // WiFi password
const char SERVER[] = "192.168.43.1";           // Old phone IP
const int PORT = 8080;                          // nanoHTTPD server port
const unsigned int udpPort = 8085;              // UDP command listener port
```

#### Pin Definitions

```cpp
const int DC_POWER_PIN = 2;              // Relay: Lock Arduino power
const int PAD_DOOR_PIN = 3;              // Input: Padlock cover reed switch (pull-up)
const int DOOR_LIGHT_CONTROL_PIN = 4;    // Relay: Outdoor light
const int MOTION_SENSOR_PIN = 5;         // Input: Motion sensor (unused)
const int WIFI_RX_PIN = 7;               // SoftwareSerial RX from ESP-01 TX
const int WIFI_TX_PIN = 6;               // SoftwareSerial TX to ESP-01 RX
const int ALARM_CONTROL_PIN = 8;         // Relay: Buzzer (active LOW)
const int CAMERA_CONTROL_PIN = 9;        // Relay: Camera power
const int DOOR_STATUS_PIN = 10;          // Input: Door lock state from Lock Arduino
const int DC_STATUS_PIN = 11;            // Input: Lock Arduino powered status
const int DC_CONTROL_PIN = 12;           // Output: Command to Lock Arduino
```

#### Global Variables

```cpp
// Configuration (remotely updatable)
unsigned int lock_timeout = 30;              // Auto-lock delay in seconds
unsigned int data_send_interval = 10;        // Status update frequency
unsigned long reset_interval = 60 * 30;      // Auto-reset interval (30 min)
unsigned int alarm_turn_off_timeout = 2;     // Buzzer duration

// State tracking
int dc_power_state = LOW;                    // Lock Arduino power state
int alarm_state = LOW;                       // Buzzer on/off
int door_light_state = LOW;                  // Outdoor light on/off
int alarm_armed_state = HIGH;                // Alarm system armed/disarmed
DoorStatus doorStatus = DS_CLOSED;           // Current door lock status

// Timers
unsigned long last_sent_millis = 0;          // Last status update timestamp
unsigned long alarm_turned_on_millis = 0;    // Alarm activation timestamp
```

#### Enum Definitions

```cpp
enum DoorStatus {
    DS_OPEN,      // Door lock is unlocked/open
    DS_CLOSED,    // Door lock is locked/closed
    DS_UNKNOWN    // Status cannot be determined
};
```

#### Key Functions

##### `void setup()`
**Purpose**: Initialize system on boot

**Actions**:
1. Initialize serial communication (19200 baud)
2. Initialize WiFi (ESP-01)
3. Connect to WiFi network
4. Initialize UDP listener (port 8085)
5. Set pin modes (inputs/outputs)
6. Turn on camera
7. Turn off alarm
8. Power off Lock Arduino
9. Set initial door status to CLOSED

##### `void loop()`
**Purpose**: Main execution loop

**Flow**:
```
1. Listen for UDP commands (udpListen())
2. Check door pad if alarm armed (checkDoorPad())
3. Check if data send interval elapsed
   └─> If yes: sendData()
4. Check if reset interval elapsed
   └─> If yes: controllerReset()
5. Manage Lock Arduino power state
```

##### `void sendData()`
**Purpose**: Send sensor data to old phone server

**Process**:
1. Read all sensor states
2. Format data string: `doorOpen,padlockOpen,dcState,alarmState,lightState,alarmArmed`
3. Send HTTP POST to `/data` endpoint
4. Parse response: `commands:config`
5. Execute commands via `executeCommands()`
6. Update configuration via `updateConfig()`

**Example Data**: `0,0,1,0,0,1` (door closed, padlock closed, DC on, alarm off, light off, armed)

**Example Response**: `d0,al0:30,10,1800,0,1,2`
- Commands: unlock door, turn off alarm
- Config: lock_timeout=30, data_interval=10, reset_interval=1800, etc.

##### `void executeCommand(String command)`
**Purpose**: Execute a single command

**Supported Commands**:
| Command | Function Called | Description |
|---------|----------------|-------------|
| `dc0` | `turnOffDoorControllerPower()` | Power off Lock Arduino |
| `dc1` | `turnOnDoorControllerPower()` | Power on Lock Arduino |
| `d0` | `unlockDoor()` | Unlock door |
| `d1` | `lockDoor()` | Lock door |
| `al0` | `turnOffAlarm()` | Silence buzzer |
| `al1` | `turnOnAlarm()` | Activate buzzer |
| `al2` | Disable alarm + `turnOffAlarm()` | Disarm alarm system |
| `al3` | Set `alarm_armed_state = HIGH` | Arm alarm system |
| `dl0` | `turnOffDoorLight()` | Turn off outdoor light |
| `dl1` | `turnOnDoorLight()` | Turn on outdoor light |
| `rst` | `controllerReset()` | Reboot Arduino |
| `crst` | `resetCamera()` | Power cycle camera |
| `c0` | `turnOffCamera()` | Disable camera |
| `c1` | `turnOnCamera()` | Enable camera |

##### `void unlockDoor()` / `void lockDoor()`
**Purpose**: Control door lock via Lock Arduino

**Process**:
1. Call `turnOnDoorControllerPower()` (powers Lock Arduino if off)
2. Check current door status via `getDoorStatus()`
3. If already in target state, return (no action needed)
4. Call `sendOpenOrCloseCommand()` to trigger Lock Arduino
5. Wait for status change (door state updates)

**Timing**: 5-8 seconds typical (depends on actuator speed)

##### `void sendOpenOrCloseCommand()`
**Purpose**: Send lock/unlock command to Lock Arduino via pin protocol

**Protocol**:
1. Set `DC_CONTROL_PIN` HIGH (signal command)
2. Wait for `DC_STATUS_PIN` to go LOW (acknowledgment)
3. Set `DC_CONTROL_PIN` LOW (complete handshake)
4. Wait for door status to change (operation complete)

**Timeout**: Function blocks until operation completes (use with caution)

##### `void checkDoorPad()`
**Purpose**: Monitor padlock cover and trigger alarm if tampered

**Logic**:
```cpp
if (PAD_DOOR_PIN is HIGH && alarm_armed_state == HIGH) {
    turnOnAlarm();
    start timer;
}
if (timer >= alarm_turn_off_timeout) {
    turnOffAlarm();
}
```

**Called**: Every loop iteration if alarm is armed

##### `void udpListen()`
**Purpose**: Listen for UDP command packets on port 8085

**Process**:
1. Check for incoming UDP packet
2. If packet received, read command string
3. Send "OK" response to sender
4. Execute command via `executeCommand()`

**Used For**: Fast, low-latency commands (especially from Android app)

##### `void updateConfig(String configList)`
**Purpose**: Update runtime configuration from server response

**Config Format**: `lock_timeout,data_interval,reset_interval,dc_power_state,alarm_armed,alarm_timeout`

**Example**: `60,15,3600,0,1,3`
- lock_timeout = 60s
- data_send_interval = 15s
- reset_interval = 3600s (1 hour)
- dc_power_state = 0 (auto)
- alarm_armed_state = 1 (armed)
- alarm_turn_off_timeout = 3s

##### Helper Functions

**WiFi Management**:
- `void initWifi()` - Initialize ESP-01 module
- `bool connectWifi()` - Connect to WiFi network
- `void initUdp()` - Start UDP listener

**HTTP Communication**:
- `String sendToServer(char data[])` - HTTP POST to old phone

**Pin Control**:
- `void turnOnDoorControllerPower()` / `void turnOffDoorControllerPower()` - Relay control
- `void turnOnAlarm()` / `void turnOffAlarm()` - Buzzer control
- `void turnOnDoorLight()` / `void turnOffDoorLight()` - Light control
- `void turnOnCamera()` / `void turnOffCamera()` - Camera control

**Status**:
- `DoorStatus getDoorStatus()` - Read door lock state from Lock Arduino
- `int getDCState()` - Check if Lock Arduino is powered

**Utilities**:
- `String getValueAtIndex(String data, char separator, int index)` - Parse CSV
- `void log(String data)` - Serial logging

---

## Lock Controller Arduino Code Documentation

### File: `arduino-lock-controller/door_controller.ino`

#### Constants

```cpp
const int BAUD_RATE = 19200;
const int ACTUATOR_IN1 = 2;              // H-bridge input 1 (forward)
const int ACTUATOR_IN2 = 3;              // H-bridge input 2 (reverse)
const int BTN_CLOSE = 4;                 // Manual close button
const int BTN_OPEN = 5;                  // Manual open button
const int OPENED_PIN = 6;                // Limit switch (HIGH when open)
const int DOOR_SENSOR_PIN = 7;           // Door closed sensor
const int DOOR_STATUS_PIN = 10;          // Output: Lock state to Main Arduino
const int DC_STATUS_PIN = 11;            // Output: Power status to Main Arduino
const int DC_CONTROL_PIN = 12;           // Input: Command from Main Arduino
```

#### Timing Constants

```cpp
const int CLOSE_DOOR_AFTER_SECS = 30;    // Auto-lock delay after door closes
const int WAIT_AFTER_OPEN_SECS = 5;      // Delay before starting auto-close timer (unused)
```

#### Global Variables

```cpp
ActuatorState actuatorState;             // Current actuator position
unsigned long door_closed_millis = 0;    // Timestamp when door closed (for auto-lock timer)
```

#### Enum Definitions

```cpp
enum ActuatorState {
    OPEN,      // Actuator retracted, door can be locked
    CLOSED     // Actuator extended, door is locked
};
```

#### Key Functions

##### `void setup()`
**Purpose**: Initialize on boot

**Actions**:
1. Initialize serial (19200 baud)
2. Set pin modes
3. Set `DC_STATUS_PIN` HIGH (indicate powered)
4. Check `OPENED_PIN` to determine initial state
5. Set `actuatorState` based on limit switch

##### `void loop()`
**Purpose**: Main execution loop

**Flow**:
```
1. runLinearActuator() - Handle actuator movement
2. readControlPins() - Check for commands from Main Arduino
```

##### `void runLinearActuator()`
**Purpose**: Control actuator based on inputs

**Logic**:
```
1. Read manual buttons
   - BTN_CLOSE pressed & OPEN → close()
   - BTN_OPEN pressed & CLOSED → open()

2. Check auto-close logic
   - If OPEN AND door sensor HIGH (closed) AND timer not started:
       Start timer (door_closed_millis = millis())
   - If door sensor LOW (open):
       Reset timer
   - If timer >= CLOSE_DOOR_AFTER_SECS:
       close()

3. Call turnOff() to ensure actuator is stopped
```

**Auto-Close Feature**: Automatically locks door 30 seconds after it's closed (if unlocked)

##### `void readControlPins()`
**Purpose**: Listen for commands from Main Arduino via pin protocol

**Protocol**:
1. Check if `DC_CONTROL_PIN` is HIGH
2. If HIGH, set `DC_STATUS_PIN` LOW (acknowledge)
3. Wait for `DC_CONTROL_PIN` to go LOW
4. Set `DC_STATUS_PIN` HIGH (ready)
5. Execute action:
   - If `actuatorState == CLOSED` → `open()`
   - If `actuatorState == OPEN` → `close()`

**Handshake**: Ensures reliable communication without data loss

##### `void open()`
**Purpose**: Retract actuator (unlock door)

**Process**:
1. Check if already open (`OPENED_PIN` HIGH)
2. If not open:
   - Set `ACTUATOR_IN1 = LOW`, `ACTUATOR_IN2 = HIGH` (reverse direction)
   - Poll `OPENED_PIN` every 200ms
   - Wait until `OPENED_PIN` goes HIGH (limit switch triggered)
3. Set `actuatorState = OPEN`
4. Set `DOOR_STATUS_PIN = HIGH` (signal Main Arduino)
5. Call `turnOff()` (stop motor)

**Timing**: Variable (5-8 seconds typical, until limit switch)

##### `void close()`
**Purpose**: Extend actuator (lock door)

**Process**:
1. Set `ACTUATOR_IN1 = HIGH`, `ACTUATOR_IN2 = LOW` (forward direction)
2. Delay 8000ms (8 seconds fixed timeout)
3. Set `actuatorState = CLOSED`
4. Set `DOOR_STATUS_PIN = LOW` (signal Main Arduino)
5. Reset timer (`door_closed_millis = 0`)
6. Call `turnOff()` (stop motor)

**Timing**: Fixed 8 seconds (no limit switch on closed position)

**Note**: Close operation uses timeout instead of limit switch. Ensure actuator reaches full extension within 8 seconds.

##### `void turnOff()`
**Purpose**: Stop actuator motor

**Actions**:
- Set `ACTUATOR_IN1 = LOW`
- Set `ACTUATOR_IN2 = LOW`
- Motor stops moving

**Called**: At end of every loop iteration and after open/close operations

##### `void setState(ActuatorState state)`
**Purpose**: Update actuator state and door status pin

**Actions**:
- If `OPEN`: Set `actuatorState = OPEN`, `DOOR_STATUS_PIN = HIGH`
- If `CLOSED`: Set `actuatorState = CLOSED`, `DOOR_STATUS_PIN = LOW`

---

## ESP32-CAM Code Documentation

### File: `esp32-camera/CameraWebServer.ino`

#### Constants

```cpp
const char* ssid = "S_HOME";
const char* password = "3030031343dc";
const char SERVER[] = "192.168.43.1";
const int PORT = 8080;

const int MOTION_PIN = 13;               // PIR sensor output
const int NIGHT_VISION_PIN = 12;         // Night vision LED control
```

#### Configuration Variables

```cpp
unsigned int ping_interval = 10;                // Heartbeat frequency (seconds)
unsigned int motion_detect_interval = 1;        // Motion check frequency (seconds)
unsigned int night_vision_timeout = 10;         // Night vision LED timeout (seconds)
```

#### Timers

```cpp
unsigned long last_ping_millis = 0;             // Last ping timestamp
unsigned long last_motion_detect = 0;           // Last motion check timestamp
unsigned long nv_turned_on_millis = 0;          // Night vision LED on timestamp
```

#### Key Functions

##### `void setup()`
**Purpose**: Initialize camera and network

**Actions**:
1. Initialize serial (115200 baud)
2. Configure camera (OV2640, SVGA, quality 12)
3. Initialize camera sensor
4. Connect to WiFi
5. Start HTTP server (`startCameraServer()`)
6. Configure pins (motion sensor, night vision LED)
7. Send boot notification to old phone

##### `void loop()`
**Purpose**: Main execution loop

**Flow**:
```
1. Ensure WiFi connected (reconnect if needed)
2. Check motion detection (if interval elapsed)
   └─> If motion: send /cam/motion, turn on night vision
3. Check night vision timeout
   └─> If timeout: turn off night vision LED
4. Check ping interval
   └─> If interval: send /cam/ping to old phone
```

##### `bool connectWifi()`
**Purpose**: Connect to WiFi network

**Process**:
1. Check if already connected
2. If not, attempt connection with 20-second timeout
3. Retry every 10 seconds
4. Return true if successful, false if failed

**Resilience**: Auto-reconnects if connection lost

##### `String sendToServer(String endpoint, char data[])`
**Purpose**: HTTP POST to old phone server

**Process**:
1. Ensure WiFi connected
2. Connect to server (192.168.43.1:8080)
3. Send HTTP POST request
4. Wait for response
5. Read response
6. Disconnect
7. Return response string

**Endpoints**:
- `/cam/motion` - Motion detected
- `/cam/ping` - Heartbeat
- `/cam/boot` - Startup notification

##### Night Vision Functions

```cpp
void turnOnNightVision() {
    digitalWrite(NIGHT_VISION_PIN, HIGH);
    nv_turned_on_millis = millis();
}

void turnOffNightVision() {
    digitalWrite(NIGHT_VISION_PIN, LOW);
}
```

**Auto-Off**: Night vision LED turns off after `night_vision_timeout` seconds

### File: `esp32-camera/app_httpd.cpp`

#### HTTP Server Endpoints

##### `static esp_err_t capture_handler(httpd_req_t *req)`
**Purpose**: Capture single image

**Process**:
1. Capture frame: `esp_camera_fb_get()`
2. If face detection disabled or large image:
   - Send JPEG directly
3. If face detection enabled:
   - Convert to RGB888
   - Run face detection
   - Draw bounding boxes
   - Convert back to JPEG
4. Set response headers
5. Send image to client

**Response**: JPEG image (Content-Type: image/jpeg)

##### `static esp_err_t stream_handler(httpd_req_t *req)`
**Purpose**: MJPEG video stream

**Process**:
1. Set response type to multipart
2. Loop continuously:
   - Capture frame
   - Optionally run face detection
   - Send frame with boundary
   - Calculate FPS
3. Continue until client disconnects

**Response**: Continuous MJPEG stream

##### `static esp_err_t cmd_handler(httpd_req_t *req)`
**Purpose**: Camera settings control

**Query Parameters**:
- `var`: Setting name
- `val`: Setting value

**Supported Settings**:
- `framesize` (0-10)
- `quality` (0-63)
- `brightness`, `contrast`, `saturation` (-2 to 2)
- `face_detect`, `face_recognize`, `face_enroll` (0/1)
- Many more (see code)

**Example**: `/control?var=quality&val=10`

##### `void startCameraServer()`
**Purpose**: Initialize HTTP server

**Actions**:
1. Create HTTP server configuration
2. Register URI handlers:
   - `/` - Web interface
   - `/capture` - Single image capture
   - `/stream` - Video stream
   - `/status` - Camera status JSON
   - `/control` - Settings control
3. Start server on port 80
4. Start stream server on port 81

---

## Summary Statistics

### Code Metrics

**Main Arduino Controller**:
- Lines of Code: ~605
- Functions: 30+
- Key Functions: 15
- Pin Definitions: 12

**Lock Controller**:
- Lines of Code: ~186
- Functions: 8
- Key Functions: 5
- Pin Definitions: 9

**ESP32-CAM**:
- Lines of Code: ~900+ (including app_httpd.cpp)
- Functions: 50+
- HTTP Endpoints: 5
- Configurable Parameters: 30+

**Android Apps**:
- Java Files: 110+
- Lines of Code: 15,000+ (estimated)
- Services: 6+
- MQTT Topics: 6

**Backend Service**:
- Java Files: 20+
- Lines of Code: 3,000+ (estimated)
- REST Endpoints: 4
- Storage Drivers: 3

---

## Code Quality Notes

### Strengths
✅ Clear pin definitions  
✅ Modular function design  
✅ Documented constants  
✅ State machine approach  
✅ Error handling in network code

### Areas for Improvement
⚠️ Limited code comments (this doc compensates)  
⚠️ Hardcoded credentials (should use config file)  
⚠️ Blocking operations in some functions  
⚠️ Limited error recovery in pin communication  
⚠️ No watchdog timer implementation

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Code Documentation Complete**
