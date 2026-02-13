# ESP32-CAM Security Camera

## Overview

The **ESP32-CAM** module serves as the intelligent security camera with motion detection capabilities. It monitors a PIR sensor, captures images on motion detection, and uploads them to the cloud storage server. It also provides a web interface for live streaming and camera control.

**Board**: ESP32-CAM (AI-Thinker)  
**Camera**: OV2640 (2MP)  
**Connectivity**: WiFi (2.4GHz)  
**Role**: Motion-triggered photography and surveillance

---

## Key Features

- 📹 **Live Streaming**: MJPEG video stream via HTTP
- 📸 **Image Capture**: High-quality JPEG images
- 🚶 **Motion Detection**: PIR sensor integration
- 🌙 **Night Vision**: IR LED control
- 🌐 **Web Interface**: Browser-based camera control
- ☁️ **Cloud Upload**: Images uploaded to Digital Ocean server
- ⚙️ **Face Detection**: Optional face recognition (AI-Thinker model)
- 🔄 **Remote Configuration**: Settings via HTTP responses

---

## Hardware Connections

### PIR Motion Sensor
```
ESP32 GPIO 13 ← PIR OUT
PIR VCC → 5V
PIR GND → GND
```

### Night Vision LED
```
ESP32 GPIO 12 → [220Ω Resistor] → LED Anode (+)
LED Cathode (-) → GND
```

### Power Supply
```
5V (500mA minimum) → ESP32-CAM 5V
GND → ESP32-CAM GND
```

⚠️ **Important**: ESP32-CAM requires stable 5V with at least 500mA capacity. Add 470µF capacitor across power pins for stability.

---

## Pin Assignments

| Pin | Mode | Connection | Purpose |
|-----|------|------------|---------|
| GPIO 13 | INPUT | PIR sensor OUT | Motion detection |
| GPIO 12 | OUTPUT | LED (via resistor) | Night vision illumination |
| GPIO 0 | INPUT | GND (programming) | Programming mode enable |
| 5V | POWER | External 5V | Main power supply |
| GND | POWER | Common ground | Ground reference |

### Camera Pins (Internal - OV2640)
Handled by ESP32-CAM board, no external connections needed.

---

## Camera Specifications

### OV2640 Camera Module

**Resolution Options**:
- UXGA: 1600x1200
- SVGA: 800x600 (default)
- VGA: 640x480
- QVGA: 320x240
- Many more...

**Current Configuration**:
- **Frame Size**: SVGA (800x600)
- **JPEG Quality**: 12 (0-63 scale, lower = better)
- **Format**: YUV422
- **Typical Image Size**: 50-100 KB

**Adjustable Settings**:
- Brightness (-2 to +2)
- Contrast (-2 to +2)
- Saturation (-2 to +2)
- Auto White Balance (on/off)
- Auto Exposure Control (on/off)
- Face Detection (on/off)

---

## Configuration

### WiFi Settings
```cpp
const char* ssid = "S_HOME";                  // Hotspot SSID
const char* password = "your_password";       // Hotspot password
```

### Server Settings
```cpp
const char SERVER[] = "192.168.43.1";         // Old phone IP
const int PORT = 8080;                        // nanoHTTPD port
```

### Timing Parameters (Configurable via HTTP)
```cpp
unsigned int ping_interval = 10;              // Heartbeat (seconds)
unsigned int motion_detect_interval = 1;      // Motion check (seconds)
unsigned int night_vision_timeout = 10;       // LED timeout (seconds)
```

---

## HTTP API Endpoints

### Camera Web Server (Port 80)

#### `GET /`
Web interface for camera control and live view.

**Response**: HTML page with controls and stream embed

#### `GET /capture`
Capture single JPEG image.

**Response**: JPEG image file  
**Headers**: `Content-Type: image/jpeg`  
**Typical Size**: 50-100 KB

**Usage**:
```bash
curl http://192.168.43.X/capture -o image.jpg
```

#### `GET /stream`
MJPEG video stream (continuous).

**Response**: Multipart MJPEG stream  
**Frame Rate**: 10-15 FPS (variable)

**Usage**: Open in browser or VLC player

#### `GET /status`
Get current camera settings as JSON.

**Response**:
```json
{
  "framesize": 7,
  "quality": 12,
  "brightness": 0,
  "face_detect": 0,
  ...
}
```

#### `GET /control?var=<setting>&val=<value>`
Adjust camera settings.

**Examples**:
```
/control?var=framesize&val=7     # Set SVGA
/control?var=quality&val=10      # Better quality
/control?var=brightness&val=1    # Increase brightness
/control?var=face_detect&val=1   # Enable face detection
```

---

## Communication with Old Phone

### `POST /cam/motion`
Sent when motion detected.

**Endpoint**: `http://192.168.43.1:8080/cam/motion`  
**Body**: "Motion detected"  
**Frequency**: On motion event

**Server Actions**:
1. Parse motion alert
2. Request image capture (GET /capture)
3. Upload image to cloud
4. Publish MQTT notification

### `POST /cam/ping`
Heartbeat to indicate camera is online.

**Endpoint**: `http://192.168.43.1:8080/cam/ping`  
**Body**: "Ping"  
**Frequency**: Every 10 seconds (configurable)

**Response**: May contain configuration updates

### `POST /cam/boot`
Sent once on startup.

**Endpoint**: `http://192.168.43.1:8080/cam/boot`  
**Body**: "Boot completed"  
**Frequency**: Once on power-on/reset

**Purpose**: Notify server of camera restart

---

## Motion Detection Flow

```
1. Check motion_detect_interval elapsed
2. Read PIR sensor (GPIO 13)
3. If motion detected (HIGH):
   a. Turn on night vision LED
   b. Send POST /cam/motion to old phone
   c. Old phone captures image (GET /capture)
   d. Old phone uploads to cloud
   e. MQTT notification sent
4. After night_vision_timeout:
   a. Turn off night vision LED
```

**Timing**:
- Motion check: Every 1 second
- Night vision: 10 seconds after motion
- Image capture: 2-3 seconds
- Total alert time: 3-5 seconds

---

## Setup Instructions

### 1. Install ESP32 Board Support

**Arduino IDE**:
1. File → Preferences
2. Additional Board Manager URLs:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
3. Tools → Board → Board Manager
4. Search "ESP32" → Install "ESP32 by Espressif"

### 2. Configure Code

Edit in `CameraWebServer.ino`:
```cpp
const char* ssid = "S_HOME";                  // Your SSID
const char* password = "your_password";       // Your password
const char SERVER[] = "192.168.43.1";         // Server IP
```

Ensure camera model is set:
```cpp
#define CAMERA_MODEL_AI_THINKER
```

### 3. Programming Mode

**Hardware Setup**:
```
FTDI (3.3V mode)    ESP32-CAM
    VCC           →  5V (needs external 5V)
    GND           →  GND
    TX            →  U0R (RX)
    RX            →  U0T (TX)
    
Programming Mode:
    GPIO 0 → GND (before powering on)
```

### 4. Upload Firmware

1. Connect FTDI programmer
2. Connect GPIO 0 to GND
3. Power on ESP32-CAM
4. Tools → Board → AI Thinker ESP32-CAM
5. Tools → Port → (FTDI port)
6. Click Upload
7. Wait for "Connecting........____......."
8. Upload completes (~1-2 minutes)
9. Disconnect GPIO 0 from GND
10. Reset ESP32-CAM

### 5. Verify Operation

1. Open Serial Monitor (115200 baud)
2. Press Reset
3. Should see:
   ```
   Camera init success
   WiFi connected
   Camera Ready! Use 'http://192.168.43.X' to connect
   ```
4. Note the IP address
5. Open browser to that IP

---

## Operation

### Normal Operation Flow
```
1. Boot → Initialize camera → Connect WiFi
2. Start HTTP server (port 80)
3. Send boot notification to old phone
4. Main loop:
   a. Check WiFi (reconnect if needed)
   b. Check motion sensor (every 1 second)
   c. Send ping to old phone (every 10 seconds)
   d. Manage night vision LED
```

### Motion Detection Behavior
- **Sensitivity**: Adjustable on PIR sensor (potentiometer)
- **Cooldown**: 1 second between checks
- **Night Vision**: Turns on with motion, off after 10 seconds
- **False Positives**: Common with heat sources, direct sunlight

### Web Interface Usage
1. Open browser to `http://192.168.43.X`
2. View live stream
3. Adjust settings (brightness, resolution, etc.)
4. Enable face detection (if needed)
5. Capture single images

---

## Troubleshooting

### Camera Not Booting
- Check 5V power (use multimeter)
- Verify sufficient current (500mA+)
- Add 470µF capacitor across power
- Check GPIO 0 not connected to GND
- Re-upload firmware

### WiFi Connection Failed
- Verify hotspot is active
- Check SSID and password
- ESP32 only supports 2.4GHz WiFi
- Move closer to hotspot
- Check serial output for errors

### Images Corrupted/Artifacts
- **Most common**: Power supply issue
- Add larger capacitor (1000µF)
- Use dedicated 5V supply
- Check camera ribbon cable
- Reduce resolution/quality

### Motion Detection Not Working
- Check PIR sensor wiring (VCC, GND, OUT)
- Adjust PIR sensitivity (turn potentiometer)
- Test PIR directly: read GPIO 13
- Check serial output for "Motion detected"

### Web Interface Not Accessible
- Verify IP address from serial monitor
- Ping ESP32-CAM: `ping 192.168.43.X`
- Check firewall on accessing device
- Try different browser
- Power cycle ESP32-CAM

### Can't Upload Firmware
- Verify GPIO 0 connected to GND during power-on
- Check FTDI TX/RX connections (swap if needed)
- Try slower upload speed (115200 → 57600)
- Use external 5V supply (FTDI may not provide enough)
- Press and hold RESET while starting upload

---

## Serial Output Examples

### Successful Boot
```
Camera init success
Attempting to connect to SSID: S_HOME
....
WiFi connected
Camera Ready! Use 'http://192.168.43.15' to connect
Stream server started on port 81
Sending boot notification...
Boot notification sent
```

### Motion Detection
```
Motion detected on GPIO 13
Turning on night vision LED
Sending motion alert to server...
Motion alert sent
Night vision timeout: 10 seconds
Night vision LED OFF
```

### Ping/Heartbeat
```
Sending ping to server...
Ping sent successfully
Response: (empty or config data)
```

### WiFi Reconnection
```
WiFi connection lost
Attempting reconnection...
....
WiFi reconnected
IP address: 192.168.43.15
```

---

## Performance Characteristics

- **Boot Time**: 5-10 seconds
- **Image Capture**: 100-300ms
- **Motion Detection Latency**: 1-2 seconds
- **Stream Frame Rate**: 10-15 FPS
- **WiFi Range**: 10-30 meters (indoor)
- **Power Consumption**: 200-500mA (streaming), 80-150mA (idle)
- **Image Quality**: Good (SVGA, quality 12)

---

## Camera Settings Reference

### Frame Sizes
- 0: 96x96
- 1: QQVGA 160x120
- 2: QCIF 176x144
- 3: HQVGA 240x176
- 4: QVGA 320x240
- 5: CIF 400x296
- 6: VGA 640x480
- **7: SVGA 800x600** (default)
- 8: XGA 1024x768
- 9: SXGA 1280x1024
- 10: UXGA 1600x1200

### JPEG Quality
- Range: 0-63
- Lower = Better quality, larger file
- Higher = Lower quality, smaller file
- **Default: 12** (good balance)

### Image Adjustments
- **Brightness**: -2 to +2
- **Contrast**: -2 to +2
- **Saturation**: -2 to +2
- **Special Effects**: Various filters available

---

## Security Considerations

⚠️ **Current Weaknesses**:
- No authentication on web interface
- No HTTPS (unencrypted stream)
- Open to anyone on WiFi
- Default web interface accessible

🔒 **Recommendations**:
- Disable web interface in production
- Add basic authentication
- Use VPN for remote access
- MAC address filtering on hotspot
- See [SECURITY.md](../SECURITY.md)

---

## Advanced Features

### Face Detection
Enable via web interface or:
```
GET /control?var=face_detect&val=1
```

**Features**:
- Real-time face detection
- Bounding boxes drawn on images
- Recognition possible (requires enrollment)

**Performance Impact**: Reduces frame rate

### Custom Resolution
For slower networks or storage:
```cpp
config.frame_size = FRAMESIZE_QVGA;  // 320x240
config.jpeg_quality = 20;            // Lower quality
```

### Flip/Mirror Image
```
/control?var=vflip&val=1    # Vertical flip
/control?var=hmirror&val=1  # Horizontal mirror
```

---

## Code Structure

### Main Files

**`CameraWebServer.ino`**:
- Main setup and loop
- WiFi management
- Motion detection
- Communication with old phone
- Night vision control

**`app_httpd.cpp`**:
- HTTP server implementation
- Stream handler
- Capture handler
- Control handler
- Web interface

### Key Functions

**`void setup()`**: Initialize camera and WiFi  
**`void loop()`**: Main execution loop  
**`bool connectWifi()`**: WiFi connection management  
**`String sendToServer()`**: HTTP POST to old phone  
**`void startCameraServer()`**: Initialize HTTP server  

---

## Future Enhancements

### Planned Improvements
- Two-way audio support
- Motion detection zones
- Scheduled recording
- Time-lapse photography
- Pan/tilt servo control
- SD card local storage

---

## Related Documentation

- [Main Arduino Controller](../main-arduino-controller/README.md) - System coordinator
- [Technical Specification](../TECHNICAL-SPECIFICATION.md) - Architecture
- [API Reference](../API-REFERENCE.md) - Complete API docs
- [Setup Guide](../SETUP-GUIDE.md) - Hardware setup
- [Troubleshooting Guide](../TROUBLESHOOTING.md) - Problem solving
- [Security Guide](../SECURITY.md) - Security improvements

---

**Serial Baud Rate**: 115200  
**WiFi**: 2.4GHz only  
**Default Resolution**: SVGA (800x600)  
**Default Quality**: 12  
**Version**: 1.0  
**Status**: Production Ready
