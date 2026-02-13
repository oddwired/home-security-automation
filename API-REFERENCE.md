# Home Security System - API Reference

## Table of Contents
1. [Arduino Commands](#arduino-commands)
2. [nanoHTTPD Server API](#nanohttpd-server-api)
3. [ESP32-CAM HTTP API](#esp32-cam-http-api)
4. [MQTT Topics & Messages](#mqtt-topics--messages)
5. [SMS Commands](#sms-commands)
6. [Digital Ocean Image Server API](#digital-ocean-image-server-api)
7. [Configuration Parameters](#configuration-parameters)

---

## Arduino Commands

### Command Format
Commands are sent via UDP to Main Arduino or embedded in HTTP responses.

**Transport**: 
- UDP packets to `192.168.43.x:8085`
- HTTP POST response body format: `commands:config`

### Command Reference

| Command | Description | Action | Response Time |
|---------|-------------|--------|---------------|
| `d0` | Unlock Door | Powers on Lock Arduino, sends unlock signal | 5-8 seconds |
| `d1` | Lock Door | Powers on Lock Arduino, sends lock signal | 8 seconds |
| `dc0` | Power Off Door Controller | Cuts power to Lock Arduino via relay | Immediate |
| `dc1` | Power On Door Controller | Supplies power to Lock Arduino via relay | 2 seconds |
| `al0` | Turn Off Alarm | Deactivates buzzer | Immediate |
| `al1` | Turn On Alarm | Activates buzzer | Immediate |
| `al2` | Disable Alarm | Disarms alarm system (won't trigger on pad door open) | Immediate |
| `al3` | Enable Alarm | Arms alarm system (will trigger on pad door open) | Immediate |
| `dl0` | Turn Off Door Light | Deactivates outdoor light relay | Immediate |
| `dl1` | Turn On Door Light | Activates outdoor light relay | Immediate |
| `rst` | Reset Controller | Reboots Main Arduino (software reset) | 3-5 seconds |
| `crst` | Reset Camera | Power cycles ESP32-CAM via relay | 5-8 seconds |
| `c0` | Turn Off Camera | Cuts power to camera module | Immediate |
| `c1` | Turn On Camera | Supplies power to camera module | 3-5 seconds |

### Command Examples

#### Via UDP (Direct)
```bash
# Unlock door
echo -n "d0" | nc -u 192.168.43.10 8085

# Turn on alarm
echo -n "al1" | nc -u 192.168.43.10 8085

# Expected response: "OK"
```

#### Via HTTP Response (Embedded)
```
HTTP/1.1 200 OK
Content-Type: text/plain

d0,al1,dl1:30,10,1800,0,1,2
```
This response tells Arduino to:
- Unlock door (`d0`)
- Turn on alarm (`al1`)
- Turn on door light (`dl1`)
- And update configuration values

### Command States & Validation

**Door Lock Commands (`d0`, `d1`)**:
- Require Lock Arduino to be powered on (or will auto-power)
- Check current door status before executing
- Will not execute if already in target state

**Alarm Commands**:
- `al1` only activates buzzer immediately
- Alarm auto-triggers when `alarm_armed_state = HIGH` AND padlock door opened
- `al2` (disable) turns off buzzer AND disarms alarm
- `al3` (enable) arms alarm but doesn't trigger buzzer

**Power Commands**:
- `dc0` immediately cuts relay power
- `dc1` attempts to power on with 3 retries if DC_STATUS_PIN doesn't go HIGH
- Camera power commands (`c0`, `c1`) control relay CH4

---

## nanoHTTPD Server API

### Server Details
- **Host**: Old Android Phone (Central Hub)
- **IP Address**: 192.168.43.1 (WiFi Hotspot)
- **Port**: 8080
- **Protocol**: HTTP/1.1
- **Server**: nanoHTTPD (Embedded)

### Endpoints

#### 1. POST /data
Receives sensor data from Main Arduino Controller.

**Request**:
```http
POST /data HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 11

0,0,1,0,0,1
```

**Request Body Format**:
```
<doorOpen>,<padlockOpen>,<dcState>,<alarmState>,<lightState>,<alarmArmed>
```

**Field Definitions**:
- `doorOpen`: `0` = Door locked, `1` = Door unlocked
- `padlockOpen`: `0` = Padlock cover closed, `1` = Padlock cover opened
- `dcState`: `0` = Lock Arduino OFF, `1` = Lock Arduino ON
- `alarmState`: `0` = Buzzer OFF, `1` = Buzzer ON
- `lightState`: `0` = Outdoor light OFF, `1` = Outdoor light ON
- `alarmArmed`: `0` = Alarm disarmed, `1` = Alarm armed

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: <length>

<commandList>:<configList>
```

**Response Body Format**:
```
cmd1,cmd2,cmd3:lockTimeout,dataInterval,resetInterval,dcPowerState,alarmArmed,alarmTimeout
```

**Example Response**:
```
d0,al0:30,10,1800,0,1,2
```
This means:
- Commands: Unlock door, Turn off alarm
- Config: lockTimeout=30s, dataInterval=10s, resetInterval=1800s, dcPowerState=0 (off), alarmArmed=1 (armed), alarmTimeout=2s

**Response on No Commands/Config**:
```
:
```
(Empty command list and config list)

**Frequency**: Called every 10 seconds by Main Arduino (configurable via `data_send_interval`)

**Error Handling**:
- If server unreachable: Arduino continues local operation
- If response unparseable: Arduino uses default values

---

#### 2. POST /cam/motion
Motion detection alert from ESP32-CAM.

**Request**:
```http
POST /cam/motion HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 15

Motion detected
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: text/plain

<config_response>
```

**Server Actions**:
1. Parse motion event
2. Trigger image capture from ESP32-CAM (HTTP GET to /capture)
3. Publish motion event to MQTT (MotionTopic)
4. (Optional) Return configuration updates

**Response Body**: May contain configuration updates for camera (ping interval, motion detect interval, night vision timeout)

**Example**:
```
10,1,10
```
(pingInterval=10s, motionDetectInterval=1s, nightVisionTimeout=10s)

---

#### 3. POST /cam/ping
Heartbeat from ESP32-CAM.

**Request**:
```http
POST /cam/ping HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 4

Ping
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: text/plain

<config_response_or_empty>
```

**Purpose**: 
- Keep-alive to ensure camera is online
- Opportunity to send configuration updates
- Monitor camera health

**Frequency**: Every 10 seconds by default (configurable)

---

#### 4. POST /cam/boot
Camera boot notification.

**Request**:
```http
POST /cam/boot HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: 14

Boot completed
```

**Response**:
```http
HTTP/1.1 200 OK
```

**Purpose**: 
- Notify server that camera has (re)started
- Useful for tracking reboots/crashes
- May trigger re-initialization on server side

**Frequency**: Once on ESP32-CAM startup

---

### Error Responses

**500 Internal Server Error**:
```http
HTTP/1.1 500 Internal Server Error
Content-Type: text/plain

Error processing request
```

**404 Not Found**:
```http
HTTP/1.1 404 Not Found
Content-Type: text/plain

Endpoint not found
```

---

## ESP32-CAM HTTP API

### Server Details
- **Host**: ESP32-CAM Module
- **IP Address**: 192.168.43.x (Assigned by DHCP from phone hotspot)
- **Port**: 80
- **Protocol**: HTTP/1.1

### Endpoints

#### 1. GET /
Web interface for camera control.

**Request**:
```http
GET / HTTP/1.1
Host: 192.168.43.x
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: text/html
Content-Encoding: gzip

<compressed HTML page>
```

**Purpose**: 
- Web UI for camera configuration
- View live stream
- Adjust camera settings (brightness, contrast, etc.)
- Enable/disable face detection

---

#### 2. GET /capture
Capture a single image.

**Request**:
```http
GET /capture HTTP/1.1
Host: 192.168.43.x
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: image/jpeg
Content-Disposition: inline; filename=capture.jpg
Access-Control-Allow-Origin: *
Content-Length: <image_size>

<JPEG binary data>
```

**Image Specifications**:
- Format: JPEG
- Resolution: SVGA (800x600) or configured size
- Quality: 12 (0-63 scale, lower = better quality)
- Color Space: YUV422 (camera native)

**Optional Face Detection**: If enabled, image will have bounding boxes drawn around detected faces.

**Typical Image Size**: 50-100 KB

**Usage**: Called by Old Phone app after motion detection to capture evidence.

---

#### 3. GET /stream
MJPEG video stream.

**Request**:
```http
GET /stream HTTP/1.1
Host: 192.168.43.x
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: multipart/x-mixed-replace;boundary=123456789000000000000987654321
Access-Control-Allow-Origin: *

--123456789000000000000987654321
Content-Type: image/jpeg
Content-Length: 52348

<JPEG frame 1>
--123456789000000000000987654321
Content-Type: image/jpeg
Content-Length: 51234

<JPEG frame 2>
--123456789000000000000987654321
...
```

**Frame Rate**: Variable (typically 10-15 FPS)

**Purpose**: 
- Live video monitoring
- View in web browser or video player
- Continuous surveillance

---

#### 4. GET /status
Get current camera status and settings.

**Request**:
```http
GET /status HTTP/1.1
Host: 192.168.43.x
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Access-Control-Allow-Origin: *

{
  "framesize": 7,
  "quality": 12,
  "brightness": 0,
  "contrast": 0,
  "saturation": 0,
  "sharpness": 0,
  "special_effect": 0,
  "wb_mode": 0,
  "awb": 1,
  "awb_gain": 1,
  "aec": 1,
  "aec2": 0,
  "ae_level": 0,
  "aec_value": 204,
  "agc": 1,
  "agc_gain": 0,
  "gainceiling": 0,
  "bpc": 0,
  "wpc": 1,
  "raw_gma": 1,
  "lenc": 1,
  "vflip": 0,
  "hmirror": 0,
  "dcw": 1,
  "colorbar": 0,
  "face_detect": 0,
  "face_enroll": 0,
  "face_recognize": 0
}
```

**Field Definitions**:
- `framesize`: 0-10 (0=96x96, 7=SVGA 800x600, 10=UXGA 1600x1200)
- `quality`: 0-63 (JPEG quality, lower is better)
- `brightness`: -2 to 2
- `contrast`: -2 to 2
- `saturation`: -2 to 2
- `face_detect`: 0 (off), 1 (on)
- `face_recognize`: 0 (off), 1 (on)

---

#### 5. GET /control
Adjust camera settings.

**Request**:
```http
GET /control?var=framesize&val=7 HTTP/1.1
Host: 192.168.43.x
```

**Query Parameters**:
- `var`: Setting name (e.g., `framesize`, `quality`, `brightness`)
- `val`: Setting value

**Response**:
```http
HTTP/1.1 200 OK
Access-Control-Allow-Origin: *
```

**Supported Variables**:
- `framesize`: 0-10
- `quality`: 0-63
- `brightness`, `contrast`, `saturation`: -2 to 2
- `gainceiling`: 0-6
- `colorbar`: 0 (off), 1 (on)
- `awb`: Auto white balance 0/1
- `agc`: Auto gain control 0/1
- `aec`: Auto exposure control 0/1
- `hmirror`: Horizontal mirror 0/1
- `vflip`: Vertical flip 0/1
- `face_detect`: 0/1
- `face_recognize`: 0/1
- `face_enroll`: 0/1

**Example - Set Quality**:
```
GET /control?var=quality&val=10
```

**Example - Enable Face Detection**:
```
GET /control?var=face_detect&val=1
```

---

## MQTT Topics & Messages

### Broker Configuration
- **Broker**: External (cloud-based), e.g., HiveMQ, CloudMQTT, Mosquitto
- **Protocol**: MQTT 3.1.1
- **Port**: 1883 (unencrypted) or 8883 (TLS, if configured)
- **QoS**: 0 for data, 1 for commands
- **Retain**: true for config/device info, false for events

### Topic Naming Convention
```
Home/<clientId>/<topicName>
```

Where `<clientId>` is configured per device (e.g., `home_controller_001`).

---

### 1. Commands Topic

**Topic**: `Home/<clientId>/commands`  
**Direction**: Personal Phone (Publish) → Old Phone (Subscribe)  
**QoS**: 1 (At least once delivery)  
**Retained**: false

**Message Format** (JSON):
```json
{
  "commands": ["d0", "al1", "dl1"],
  "timestamp": 1644678900
}
```

**Fields**:
- `commands`: Array of command strings (see Arduino Commands section)
- `timestamp`: Unix timestamp (optional)

**Example Messages**:

Unlock door:
```json
{"commands": ["d0"]}
```

Lock door and turn off light:
```json
{"commands": ["d1", "dl0"]}
```

Emergency: Unlock door, turn off alarm, turn on light:
```json
{"commands": ["d0", "al0", "dl1"]}
```

---

### 2. Config Topic

**Topic**: `Home/<clientId>/config`  
**Direction**: Personal Phone (Publish) → Old Phone (Subscribe)  
**QoS**: 1  
**Retained**: true (last known good configuration)

**Message Format** (JSON):
```json
{
  "lockTimeout": 30,
  "dataInterval": 10,
  "resetInterval": 1800,
  "dcPowerState": 0,
  "alarmArmed": 1,
  "alarmTimeout": 2,
  "pingInterval": 10,
  "motionDetectInterval": 1,
  "nightVisionTimeout": 10
}
```

**Field Definitions**:
- `lockTimeout`: Seconds before auto-lock after door closed (default: 30)
- `dataInterval`: Seconds between status updates from Arduino (default: 10)
- `resetInterval`: Seconds before Arduino auto-reset (default: 1800 = 30 min)
- `dcPowerState`: 0 = Auto power management, 1 = Keep powered
- `alarmArmed`: 0 = Disarmed, 1 = Armed
- `alarmTimeout`: Seconds buzzer stays on (default: 2)
- `pingInterval`: Seconds between camera pings (default: 10)
- `motionDetectInterval`: Seconds between motion checks (default: 1)
- `nightVisionTimeout`: Seconds before night vision LED turns off (default: 10)

**Example - Increase Lock Timeout to 60 seconds**:
```json
{
  "lockTimeout": 60
}
```

**Example - Disable Alarm**:
```json
{
  "alarmArmed": 0
}
```

---

### 3. Data Topic

**Topic**: `Home/<clientId>/data`  
**Direction**: Old Phone (Publish) → Personal Phone (Subscribe)  
**QoS**: 0 (Fire and forget, real-time data)  
**Retained**: false

**Message Format** (JSON):
```json
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

**Field Definitions**:
- `doorOpen`: 0 = Locked, 1 = Unlocked
- `padlockOpen`: 0 = Closed, 1 = Opened
- `dcState`: 0 = Lock Arduino OFF, 1 = ON
- `alarmState`: 0 = Buzzer OFF, 1 = Buzzer ON
- `lightState`: 0 = Light OFF, 1 = Light ON
- `alarmArmed`: 0 = Disarmed, 1 = Armed
- `timestamp`: Unix timestamp

**Frequency**: Every 10 seconds (configurable)

**Example**:
```json
{
  "doorOpen": 1,
  "padlockOpen": 0,
  "dcState": 1,
  "alarmState": 0,
  "lightState": 1,
  "alarmArmed": 1,
  "timestamp": 1644678950
}
```
(Door unlocked, light on, alarm armed)

---

### 4. Motion Topic

**Topic**: `Home/<clientId>/motion`  
**Direction**: Old Phone (Publish) → Personal Phone (Subscribe)  
**QoS**: 1 (Important event)  
**Retained**: false

**Message Format** (JSON):
```json
{
  "detected": true,
  "timestamp": 1644678900,
  "imageUrl": "https://server.example.com/files/abc123"
}
```

**Field Definitions**:
- `detected`: Always `true` (motion occurred)
- `timestamp`: Unix timestamp of detection
- `imageUrl`: URL to captured image (if available)

**Example**:
```json
{
  "detected": true,
  "timestamp": 1644678965,
  "imageUrl": "https://your-server.com/files/20240212103005_motion.jpg"
}
```

**Trigger**: Sent when ESP32-CAM detects motion and image is captured/uploaded

---

### 5. Device Info Topic

**Topic**: `Home/<clientId>/device`  
**Direction**: Old Phone (Publish) → Personal Phone (Subscribe)  
**QoS**: 0  
**Retained**: true (last known status)

**Message Format** (JSON):
```json
{
  "batteryLevel": 85,
  "charging": false,
  "wifiConnected": true,
  "mqttConnected": true,
  "arduinoOnline": true,
  "cameraOnline": true,
  "uptime": 3600,
  "timestamp": 1644678900
}
```

**Field Definitions**:
- `batteryLevel`: 0-100 (percentage)
- `charging`: true/false
- `wifiConnected`: true if WiFi connected
- `mqttConnected`: true if MQTT connected
- `arduinoOnline`: true if receiving data from Arduino
- `cameraOnline`: true if receiving pings from camera
- `uptime`: Seconds since app started
- `timestamp`: Unix timestamp

**Frequency**: Periodically (e.g., every 60 seconds)

---

### 6. SMS Topic

**Topic**: `Home/<clientId>/sms`  
**Direction**: Old Phone (Publish) → Personal Phone (Subscribe)  
**QoS**: 1  
**Retained**: false

**Message Format** (JSON):
```json
{
  "from": "+1234567890",
  "command": "d0",
  "result": "success",
  "message": "Door unlocked via SMS",
  "timestamp": 1644678900
}
```

**Field Definitions**:
- `from`: Sender phone number
- `command`: Command extracted from SMS
- `result`: `success`, `failed`, `invalid`
- `message`: Human-readable result message
- `timestamp`: Unix timestamp

**Purpose**: Log SMS commands for audit and notification

**Example**:
```json
{
  "from": "+1234567890",
  "command": "d0",
  "result": "success",
  "message": "Door unlocked successfully",
  "timestamp": 1644678980
}
```

---

## SMS Commands

### Format
SMS commands can be sent from Personal Phone to Old Phone when data connection is unavailable.

**General Format**:
```
<command>
```

Or with parameters:
```
<command>:<param1>,<param2>
```

### Supported Commands

**Same as Arduino commands**:
- `d0` - Unlock door
- `d1` - Lock door
- `al0` - Turn off alarm
- `al1` - Turn on alarm
- `al2` - Disable alarm (disarm)
- `al3` - Enable alarm (arm)
- `dl0` - Turn off door light
- `dl1` - Turn on door light
- `rst` - Reset controller

**Example SMS Messages**:
```
d0
```
(Unlocks the door)

```
al0
```
(Turns off alarm)

```
d1
```
(Locks the door)

### SMS Processing Flow
1. Personal Phone sends SMS with command to Old Phone number
2. Old Phone `SmsReceiver` intercepts SMS
3. Command is parsed and validated
4. Command is sent to Arduino (UDP or HTTP)
5. Result is published to MQTT (SmsTopic) for logging
6. (Optional) Reply SMS sent back to Personal Phone

### Security Considerations
- **Sender Validation**: Old Phone should validate sender phone number
- **Command Whitelist**: Only allow predefined commands
- **Rate Limiting**: Implement delay between SMS commands to prevent abuse
- **Reply SMS**: Confirm command execution

---

## Digital Ocean Image Server API

### Base URL
```
https://your-server.example.com
```

### Authentication
- **Method**: Bearer Token (configurable)
- **Header**: `Authorization: Bearer <token>`

(Note: Current implementation may not have auth enabled - security improvement needed)

---

### 1. POST /upload
Upload an image file.

**Request**:
```http
POST /upload HTTP/1.1
Host: your-server.example.com
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
Authorization: Bearer <token>

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="capture.jpg"
Content-Type: image/jpeg

<binary image data>
------WebKitFormBoundary--
```

**Response** (Success):
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "abc123",
  "fileId": "20240212103005_capture",
  "fileName": "capture.jpg",
  "contentType": "image/jpeg",
  "fileSize": 87654,
  "storageLocation": "s3://bucket/path/to/file.jpg",
  "uploadedAt": "2024-02-12T10:30:05Z",
  "url": "https://your-server.example.com/files/abc123"
}
```

**Response Fields**:
- `id`: Database record ID
- `fileId`: Unique file identifier
- `fileName`: Original filename
- `contentType`: MIME type
- `fileSize`: Size in bytes
- `storageLocation`: Backend storage path (S3 key or filesystem path)
- `uploadedAt`: ISO 8601 timestamp
- `url`: Public URL to retrieve file

**Response** (Error):
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": "Invalid file format",
  "message": "Only JPEG and PNG images are allowed"
}
```

---

### 2. GET /files/{id}
Retrieve an uploaded image.

**Request**:
```http
GET /files/abc123 HTTP/1.1
Host: your-server.example.com
```

**Response** (Success):
```http
HTTP/1.1 200 OK
Content-Type: image/jpeg
Content-Disposition: inline; filename="capture.jpg"
Content-Length: 87654

<binary image data>
```

**Response** (Not Found):
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "File not found",
  "message": "No file with ID 'abc123' exists"
}
```

---

### 3. GET /files
List uploaded files (with optional filtering).

**Request**:
```http
GET /files?page=0&size=20&sort=uploadedAt,desc HTTP/1.1
Host: your-server.example.com
```

**Query Parameters**:
- `page`: Page number (0-indexed)
- `size`: Items per page (default: 20)
- `sort`: Sort field and direction (e.g., `uploadedAt,desc`)

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": "abc123",
      "fileId": "20240212103005_capture",
      "fileName": "capture.jpg",
      "contentType": "image/jpeg",
      "fileSize": 87654,
      "uploadedAt": "2024-02-12T10:30:05Z",
      "url": "https://your-server.example.com/files/abc123"
    },
    {
      "id": "abc124",
      "fileId": "20240212104512_capture",
      "fileName": "capture.jpg",
      "contentType": "image/jpeg",
      "fileSize": 92341,
      "uploadedAt": "2024-02-12T10:45:12Z",
      "url": "https://your-server.example.com/files/abc124"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

---

### 4. DELETE /files/{id}
Delete an uploaded file.

**Request**:
```http
DELETE /files/abc123 HTTP/1.1
Host: your-server.example.com
Authorization: Bearer <token>
```

**Response** (Success):
```http
HTTP/1.1 204 No Content
```

**Response** (Not Found):
```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "File not found"
}
```

---

## Configuration Parameters

### Main Arduino Configuration

| Parameter | Type | Default | Min | Max | Description |
|-----------|------|---------|-----|-----|-------------|
| `lock_timeout` | uint | 30 | 5 | 300 | Seconds before auto-lock after door closed |
| `data_send_interval` | uint | 10 | 5 | 60 | Seconds between status updates |
| `reset_interval` | ulong | 1800 | 600 | 86400 | Seconds before auto-reset (30 min default) |
| `alarm_turn_off_timeout` | int | 2 | 1 | 60 | Seconds buzzer stays on |
| `dc_power_state` | int | 0 | 0 | 1 | 0=auto, 1=keep powered |
| `alarm_armed_state` | int | 1 | 0 | 1 | 0=disarmed, 1=armed |

**Update Method**: Via HTTP response from /data endpoint (config list) or via MQTT ConfigTopic

**Config Response Format**:
```
lockTimeout,dataInterval,resetInterval,dcPowerState,alarmArmed,alarmTimeout
```

**Example**:
```
60,15,3600,0,1,3
```
(lock_timeout=60s, data_send_interval=15s, reset_interval=3600s, dc_power_state=0, alarm_armed_state=1, alarm_turn_off_timeout=3s)

---

### ESP32-CAM Configuration

| Parameter | Type | Default | Min | Max | Description |
|-----------|------|---------|-----|-----|-------------|
| `ping_interval` | uint | 10 | 5 | 300 | Seconds between heartbeat pings |
| `motion_detect_interval` | uint | 1 | 1 | 60 | Seconds between motion sensor checks |
| `night_vision_timeout` | uint | 10 | 5 | 300 | Seconds before night vision LED turns off |

**Update Method**: Via HTTP response from /cam/ping or /cam/motion endpoints

**Response Format**:
```
pingInterval,motionDetectInterval,nightVisionTimeout
```

**Example**:
```
15,2,20
```

---

### Lock Arduino Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `CLOSE_DOOR_AFTER_SECS` | const int | 30 | Auto-lock delay (hardcoded) |
| `WAIT_AFTER_OPEN_SECS` | const int | 5 | Delay before starting close timer (unused) |

**Note**: Lock Arduino configuration is currently hardcoded in firmware. Future enhancement could add runtime configuration via inter-Arduino communication.

---

## Error Codes & Responses

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful request |
| 204 | No Content | Successful delete |
| 400 | Bad Request | Invalid parameters |
| 401 | Unauthorized | Missing/invalid auth token |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server-side error |
| 503 | Service Unavailable | Server temporarily down |

### Arduino Error States

**No error codes returned directly**, but conditions:
- **WiFi connection failed**: Continues local operation, retries periodically
- **Server unreachable**: Uses cached config, continues operation
- **Lock controller not responding**: Logs error, attempts 3 retries

### MQTT Error Handling

**Connection Lost**:
- Auto-reconnect with 15-second interval
- Commands queued locally (not implemented - potential enhancement)
- Status updates resume when connection restored

**Message Delivery Failed** (QoS 1):
- MQTT library automatically retries
- Message may be delivered multiple times (idempotent commands)

---

## Rate Limits & Throttling

### Arduino Status Updates
- **Frequency**: Every 10 seconds (configurable)
- **Minimum**: 5 seconds (to prevent overload)
- **Maximum**: 60 seconds (to maintain responsiveness)

### Camera Pings
- **Frequency**: Every 10 seconds (configurable)
- **Minimum**: 5 seconds

### Motion Detection
- **Cooldown**: 1 second between detections (configurable)
- **Purpose**: Prevent multiple triggers for single event

### Command Execution
- **No built-in rate limiting** (potential security issue)
- **Recommendation**: Implement rate limiting in Android app (e.g., max 10 commands/minute)

---

## Websocket APIs (Future Enhancement)

Currently not implemented, but potential addition for real-time bidirectional communication:

**Proposed Endpoint**: `ws://192.168.43.1:8080/ws`

**Use Cases**:
- Real-time status updates (instead of polling)
- Instant command acknowledgment
- Streaming sensor data

---

## API Versioning

**Current Version**: v1 (implicit, no versioning implemented)

**Recommendation**: Add version prefix to endpoints:
```
/v1/data
/v1/cam/motion
/v1/files/upload
```

---

## Security Best Practices

### For Production Use:
1. **Enable HTTPS**: Use TLS for all HTTP communication
2. **MQTT over TLS**: Port 8883 with certificate validation
3. **Authentication**: Implement token-based auth for all endpoints
4. **Rate Limiting**: Prevent abuse of command endpoints
5. **Input Validation**: Sanitize all inputs (commands, config values)
6. **Access Control**: Restrict SMS commands to whitelisted phone numbers
7. **Audit Logging**: Log all commands and configuration changes
8. **Encrypt Images**: Encrypt images at rest and in transit

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Status**: Complete API Reference
