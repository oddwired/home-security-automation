# Home Security System - Complete Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Hardware Components](#hardware-components)
4. [Software Stack](#software-stack)
5. [Repository Structure](#repository-structure)
6. [Quick Start Guide](#quick-start-guide)
7. [Related Documentation](#related-documentation)

---

## Project Overview

### Motivation
This home security system was designed to prevent intrusion through intelligent door monitoring and automated locking mechanisms. The system provides real-time surveillance, automated door locking, motion detection with image capture, and remote control capabilities via MQTT and SMS fallback.

### Key Features
- **Automated Door Locking**: Linear actuator-based lock with automatic closure after configurable delay
- **Motion-Triggered Camera**: ESP32-CAM captures and uploads images when motion is detected
- **Dual Communication**: Primary MQTT communication with SMS fallback
- **Real-time Monitoring**: Sensor data streaming to personal phone via MQTT
- **Intrusion Alerts**: Buzzer activation when padlock cover is opened (if armed)
- **Remote Control**: Lock/unlock door, control lights, arm/disarm alarm via MQTT or SMS
- **Power Backup**: UPS with 12V battery ensures continuous operation
- **Auto-Recovery**: Scheduled phone boot and WiFi hotspot auto-enable

### System Capabilities
- Monitor door status (open/closed)
- Detect padlock cover tampering
- Capture images on motion detection
- Upload images to cloud storage (Digital Ocean)
- Remote door lock/unlock
- Remote alarm arm/disarm/silence
- Control outdoor light
- Configure timeouts and intervals remotely
- SMS command fallback when data unavailable

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PERSONAL PHONE                                  │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Home Security App (Remote Control)                              │   │
│  │  - MQTT Client (Primary)                                         │   │
│  │  - SMS Commands (Fallback)                                       │   │
│  │  - View Images from Server                                       │   │
│  │  - Control: Lock/Unlock, Alarm, Lights                           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────┬───────────────────────────┬───────────────────────┘
                      │ MQTT (Internet)           │ SMS
                      │                           │
                      ▼                           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    OLD ANDROID PHONE (Central Hub)                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  Home Client App (Server)                                        │   │
│  │  ├─ nanoHTTPD Server (192.168.43.1:8080)                         │   │
│  │  │  └─ Endpoints: /data, /cam/motion, /cam/ping, /cam/boot       │   │
│  │  ├─ MQTT Client (Bridge to Internet)                             │   │
│  │  └─ SMS Receiver (Fallback Commands)                             │   │
│  │  ├─ MacroDroid Automation                                        │   │
│  │  │  └─ Auto WiFi Hotspot (SSID: S_HOME)                          │   │
│  │  │  └─ Scheduled Boot (7:00 AM)                                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────┬──────────────────────────┬──────────────────────────────┬─────┘
          │ WiFi Hotspot             │ HTTP/UDP                     │ HTTP
          │ (192.168.43.x)           │                              │
          ▼                          ▼                              ▼
┌──────────────────────┐  ┌─────────────────────────┐  ┌───────────────────┐
│   ESP32-CAM          │  │  MAIN ARDUINO NANO      │  │ DIGITAL OCEAN     │
│   (Camera Module)    │  │  (Always-On Controller) │  │ DROPLET           │
│                      │  │                         │  │                   │
│ ├─ PIR Motion        │  │ ├─ ESP-01 WiFi Module   │  │ ├─ Spring Boot    │
│ │  Sensor (GPIO 13)  │  │ │  └─ HTTP (TCP) for    │  │ │   Application   │
│ │                    │  │ │     status            │  │ │                 │
│ ├─ Camera OV2640     │  │ │  └─ UDP for commands  │  │ ├─ Image Storage  │
│ │                    │  │ │                       │  │ │   Service       │
│ ├─ Night Vision LED  │  │ ├─ Door Reed Switch     │  │ │                 │
│ │  (GPIO 12)         │  │ │  (GPIO 10)            │  │ ├─ PostgreSQL DB  │
│ │                    │  │ │                       │  │ │   (Metadata)    │
│ └─ Web Server        │  │ ├─ Padlock Reed Switch  │  │ │                 │
│    - /capture        │  │ │  (GPIO 3, pull-up)    │  │ └─ AWS S3 or      │
│    - /stream         │  │ │                       │  │    Local Storage  │
│                      │  │ ├─ Relay Control:       │  │                   │
│ Sends to:            │  │ │  • DC Power (GPIO 2)  │  │ API:              │
│ /cam/motion          │  │ │  • Buzzer (GPIO 8)    │  │ POST /upload      │
│ /cam/ping            │  │ │  • Light (GPIO 4)     │  │ GET /files/{id}   │
└──────────────────────┘  │ │  • Camera (GPIO 9)    │  └───────────────────┘
                          │ │                       │
                          │ └─ Inter-Arduino Comm:  │
                          │    • Control (GPIO 12)  │
                          │    • Status (GPIO 11)   │
                          │    • Door Status (GPIO  │
                          │      10 - from DC)      │
                          └─────────┬───────────────┘
                                    │ Pin Communication
                                    │ (Control/Status)
                                    ▼
                           ┌─────────────────────────┐
                           │  ARDUINO NANO           │
                           │  (Lock Controller)      │
                           │                         │
                           │ ├─ Linear Actuator      │
                           │ │  Driver (GPIO 2,3)    │
                           │ │                       │
                           │ ├─ Push Button (Limit   │
                           │ │  Switch, GPIO 6)      │
                           │ │                       │
                           │ ├─ Close Button         │
                           │ │  (GPIO 4)             │
                           │ │                       │
                           │ ├─ Open Button          │
                           │ │  (GPIO 5)             │
                           │ │                       │
                           │ ├─ Door Sensor          │
                           │ │  (GPIO 7)             │
                           │ │                       │
                           │ └─ Communication Pins:  │
                           │    • Control In (12)    │
                           │    • Status Out (11)    │
                           │    • Door Status (10)   │
                           │    • DC Status (11)     │
                           └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         POWER SYSTEM                                    │
│  ├─ 12V 5A Power Supply                                                 │
│  ├─ UPS with 12V Battery Backup                                         │
│  └─ Powers: Both Arduinos, ESP modules, Linear Actuator, Relays         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

#### 1. Normal Monitoring Flow
```
[Sensors] → [Main Arduino] → [ESP-01 WiFi] → [HTTP POST to Phone] 
→ [Phone MQTT Client] → [MQTT Broker] → [Personal Phone]
```

#### 2. Lock/Unlock Command Flow
```
[Personal Phone] → [MQTT/SMS] → [Old Phone] → [UDP to ESP-01] 
→ [Main Arduino] → [Pin Control] → [Lock Controller Arduino] 
→ [Linear Actuator]
```

#### 3. Motion Detection & Image Capture Flow
```
[PIR Sensor] → [ESP32-CAM] → [HTTP POST /cam/motion to Phone] 
→ [Phone sends capture command] → [ESP32-CAM /capture endpoint] 
→ [ESP32-CAM uploads to Digital Ocean] → [Image stored]
```

---

## Hardware Components

### Complete Bill of Materials

| Component | Quantity | Specifications | Purpose |
|-----------|----------|----------------|---------|
| **Arduino Nano** | 2 | ATmega328P, 5V, 16MHz | Main controller & Lock controller |
| **ESP-01 WiFi Module** | 1 | ESP8266, 2.4GHz WiFi | Wireless communication for main controller |
| **ESP32-CAM** | 1 | ESP32-S with OV2640 camera | Motion detection & image capture |
| **Linear Actuator** | 1 | 12V DC, stroke length ~50mm | Door lock mechanism |
| **Linear Actuator Driver** | 1 | L298N or similar H-bridge | Control actuator direction |
| **PIR Motion Sensor** | 1 | HC-SR501 or similar | Motion detection |
| **Reed Switch** | 2 | Magnetic, NO (Normally Open) | Door & padlock monitoring |
| **Push Button** | 1 | Momentary switch | Lock position limit switch |
| **Relay Module** | 1 | 4-channel, 5V trigger | Control DC power, buzzer, light, camera |
| **Buzzer** | 1 | 5V/12V active | Intrusion alarm |
| **Power Supply** | 1 | 12V 5A | Main power |
| **UPS** | 1 | 12V battery backup | Power continuity |
| **Old Android Phone** | 1 | Android 6.0+ | Central hub server |
| **Personal Android Phone** | 1 | Android 6.0+ | Remote control |
| **Outdoor Light Bulb** | 1 | 110V/220V AC (via relay) | Deterrence/visibility |

### Pin Assignments

#### Main Arduino Controller (Always-On)
```
Digital Pins:
  GPIO 2  : DC_POWER_PIN (Output) - Relay to power Lock Controller
  GPIO 3  : PAD_DOOR_PIN (Input, Pull-up) - Padlock cover reed switch
  GPIO 4  : DOOR_LIGHT_CONTROL_PIN (Output) - Outdoor light relay
  GPIO 5  : MOTION_SENSOR_PIN (Input) - Motion sensor (unused, handled by ESP32)
  GPIO 6  : WIFI_TX_PIN (TX to ESP-01 RX)
  GPIO 7  : WIFI_RX_PIN (RX from ESP-01 TX)
  GPIO 8  : ALARM_CONTROL_PIN (Output) - Buzzer relay
  GPIO 9  : CAMERA_CONTROL_PIN (Output) - Camera power relay
  GPIO 10 : DOOR_STATUS_PIN (Input) - Door lock status from Lock Controller
  GPIO 11 : DC_STATUS_PIN (Input) - Lock Controller powered status
  GPIO 12 : DC_CONTROL_PIN (Output) - Send lock/unlock command to Lock Controller
```

#### Lock Controller Arduino
```
Digital Pins:
  GPIO 2  : ACTUATOR_IN1 (Output) - H-bridge input 1
  GPIO 3  : ACTUATOR_IN2 (Output) - H-bridge input 2
  GPIO 4  : BTN_CLOSE (Input) - Manual close button
  GPIO 5  : BTN_OPEN (Input) - Manual open button
  GPIO 6  : OPENED_PIN (Input) - Limit switch (HIGH when fully open)
  GPIO 7  : DOOR_SENSOR_PIN (Input) - Door closed sensor
  GPIO 10 : DOOR_STATUS_PIN (Output) - Current lock state (HIGH=open, LOW=closed)
  GPIO 11 : DC_STATUS_PIN (Output) - Power status (HIGH=powered)
  GPIO 12 : DC_CONTROL_PIN (Input) - Receive command from Main Controller
```

#### ESP32-CAM Module
```
GPIO 13 : MOTION_PIN (Input) - PIR motion sensor
GPIO 12 : NIGHT_VISION_PIN (Output) - Night vision LED control

Camera Pins (AI-Thinker module):
  Standard OV2640 pin configuration (defined in camera_pins.h)
```

---

## Software Stack

### Arduino Firmware

#### Main Controller (main-arduino-controller)
- **Platform**: Arduino (ATmega328P)
- **Baud Rate**: 19200
- **Libraries**:
  - `SoftwareSerial` - ESP-01 communication
  - `WiFiEsp` - WiFi functionality via ESP-01
  - `WiFiEspUdp` - UDP command reception
- **Key Functions**:
  - Sensor monitoring (door, padlock, motion)
  - ESP-01 WiFi management
  - HTTP status reporting (POST /data)
  - UDP command listening (port 8085)
  - Relay control (DC power, alarm, light, camera)
  - Inter-Arduino communication protocol
  - Auto-reset every 30 minutes

#### Lock Controller (arduino-lock-controller)
- **Platform**: Arduino (ATmega328P)
- **Baud Rate**: 19200
- **Libraries**: None (bare metal)
- **Key Functions**:
  - Linear actuator H-bridge control
  - Limit switch monitoring
  - Auto-close timer (30 seconds after door closed)
  - Manual button handling
  - Pin-based communication with Main Controller

#### ESP32-CAM (esp32-camera)
- **Platform**: ESP32 (ESP32-S)
- **Framework**: Arduino Core for ESP32
- **Libraries**:
  - `esp_camera` - Camera functions
  - `WiFi` - Network connectivity
  - `esp_http_server` - Web server
- **Key Features**:
  - Motion detection with configurable intervals
  - Image capture (JPEG, SVGA resolution)
  - Video streaming (/stream endpoint)
  - Night vision LED control
  - HTTP endpoints: /, /capture, /stream, /control, /status
  - Sends alerts to nanoHTTPD server on motion

### Android Applications

#### Home Client App (Old Phone - Server)
- **Platform**: Android 6.0+
- **Language**: Java
- **Key Libraries**:
  - **nanoHTTPD** - Lightweight HTTP server
  - **Eclipse Paho MQTT** - MQTT client
  - **ObjectBox** - Local database
  - **Retrofit** (likely for REST API)
- **Server Endpoints**:
  - `POST /data` - Receive sensor data from Main Arduino
  - `POST /cam/motion` - Motion alert from ESP32-CAM
  - `POST /cam/ping` - Heartbeat from camera
  - `POST /cam/boot` - Camera boot notification
- **MQTT Topics** (Subscribe):
  - Commands topic - Receive control commands
  - Config topic - Receive configuration updates
- **MQTT Topics** (Publish):
  - Data topic - Sensor data forwarding
  - Motion topic - Motion events
  - Device info topic - Status information
  - SMS topic - SMS command results
- **Services**:
  - `MainControlService` - Main background service
  - `AsyncService` - Async task execution
- **MacroDroid Automation**:
  - Auto-enable WiFi hotspot on boot
  - Scheduled power-on at 7:00 AM

#### Home Security App (Personal Phone - Client)
- **Platform**: Android 6.0+
- **Language**: Java
- **Key Libraries**:
  - **Eclipse Paho MQTT** - MQTT client
  - **ObjectBox** - Local database
- **Features**:
  - MQTT command publishing
  - SMS command fallback
  - Real-time status monitoring
  - Image viewing from server
  - Control interface (lock/unlock, alarm, lights)
  - Notification handling
- **SMS Commands**:
  - Send commands when data connection lost
  - Parsed and executed by Old Phone app

### Backend Service (Digital Ocean)

#### Image Storage Service (home-security-service)
- **Platform**: Spring Boot 2.1.5
- **Language**: Java 8
- **Framework**: Spring Boot
- **Database**: PostgreSQL
- **Storage Options**:
  - AWS S3 (cloud storage)
  - Google Cloud Storage (alternative)
  - Local filesystem (fallback)
- **API Endpoints**:
  - `POST /upload` - Upload image from ESP32-CAM
  - `GET /files/{id}` - Retrieve image
  - `GET /files` - List images
- **Features**:
  - Image metadata storage in PostgreSQL
  - Configurable storage backend
  - REST API for image management

### Communication Protocols

#### HTTP (Main Arduino ↔ Old Phone)
```
POST /data HTTP/1.1
Host: 192.168.43.1:8080
Content-Type: text/plain
Content-Length: <length>

<doorOpen>,<padlockOpen>,<dcState>,<alarmState>,<lightState>,<alarmArmed>

Response: <commands>:<config>
  commands: cmd1,cmd2,cmd3,...
  config: lockTimeout,dataInterval,resetInterval,dcPowerState,alarmArmed,alarmTimeout
```

#### UDP (Old Phone ↔ Main Arduino)
```
Packet to: 192.168.43.x:8085
Data: <command> (e.g., "d0", "d1", "al1", "dl1")

Response: "OK"
```

#### MQTT Topics Structure
```
Home/<clientId>/commands   (Subscribe) - Receive commands from personal phone
Home/<clientId>/config     (Subscribe) - Receive config updates
Home/<clientId>/data       (Publish)   - Send sensor data
Home/<clientId>/motion     (Publish)   - Motion detection events
Home/<clientId>/device     (Publish)   - Device status/info
Home/<clientId>/sms        (Publish)   - SMS command results
```

### Network Configuration
```
WiFi Hotspot (Old Phone):
  SSID: S_HOME
  Password: 3030031343dc
  IP: 192.168.43.1
  
nanoHTTPD Server:
  Address: 192.168.43.1
  Port: 8080
  
UDP Listener:
  Port: 8085
  
MQTT Broker:
  External (Internet-based)
  Configurable in app
```

---

## Repository Structure

```
home-automation-project1/
│
├── arduino-lock-controller/          # Lock Controller Arduino code
│   ├── door_controller.ino          # Main firmware
│   └── README.md
│
├── main-arduino-controller/          # Main Controller Arduino code
│   ├── main_controller.ino          # Main firmware
│   └── README.md
│
├── esp32-camera/                     # ESP32-CAM firmware
│   ├── CameraWebServer.ino          # Main firmware
│   ├── app_httpd.cpp                # HTTP server & camera handlers
│   ├── camera_pins.h                # Pin definitions
│   ├── camera_index.h               # Web interface HTML
│   └── README.md
│
├── home-client-android/              # Old Phone Server App
│   ├── app/src/main/java/com/kshem/homeclient/
│   │   ├── services/                # Background services
│   │   │   └── MainControlService.java
│   │   ├── mqtt/                    # MQTT client implementation
│   │   │   ├── MqttService.java
│   │   │   ├── CommandsTopic.java
│   │   │   ├── DataTopic.java
│   │   │   ├── ConfigTopic.java
│   │   │   ├── MotionTopic.java
│   │   │   └── SmsTopic.java
│   │   ├── controller/              # Arduino controller interface
│   │   ├── camera/                  # Camera interface
│   │   └── models/                  # Data models
│   ├── build.gradle
│   └── README.MD
│
├── home-security-app/                # Personal Phone Control App
│   ├── app/src/main/java/com/kshem/homesecurity/
│   │   ├── controller/              # Controller command interface
│   │   ├── camera/                  # Camera control
│   │   ├── mqtt/                    # MQTT client
│   │   └── broadcastreceivers/      # SMS receiver, boot receiver
│   ├── build.gradle
│   └── README.md
│
├── home-security-service/            # Digital Ocean Image Server
│   ├── src/main/java/com/kshem/services/homesecurity/
│   │   ├── controllers/             # REST API controllers
│   │   │   └── FileController.java
│   │   ├── services/                # Business logic
│   │   │   ├── StorageService.java
│   │   │   └── implementation/
│   │   │       ├── AwsS3DriverImpl.java
│   │   │       ├── GcpDriverImpl.java
│   │   │       └── LocalStorageDriverImpl.java
│   │   ├── repositories/            # Database repositories
│   │   └── entities/                # JPA entities
│   ├── pom.xml
│   ├── Dockerfile
│   └── README.md
│
├── MASTER-README.md                  # This file
├── TECHNICAL-SPECIFICATION.md        # Detailed technical specs
├── SETUP-GUIDE.md                    # Installation & setup instructions
├── API-REFERENCE.md                  # API documentation
└── TROUBLESHOOTING.md                # Maintenance & troubleshooting
```

---

## Quick Start Guide

### Prerequisites
- Arduino IDE 1.8+ (for Arduino programming)
- ESP32 board support in Arduino IDE
- Android Studio (for Android app development)
- Java 8+ and Maven (for backend service)
- MQTT broker (e.g., Mosquitto, CloudMQTT, HiveMQ)
- Digital Ocean account (or alternative cloud provider)

### Hardware Assembly (Brief)
1. **Power System**:
   - Connect 12V 5A supply to UPS
   - Wire UPS output to relay module VCC
   - Connect Arduino Nano boards to 5V (via voltage regulator from 12V)

2. **Main Controller Setup**:
   - Connect ESP-01 to Arduino (TX→RX, RX→TX, VCC, GND)
   - Wire sensors to assigned GPIO pins
   - Connect relay module control pins

3. **Lock Controller Setup**:
   - Connect H-bridge to actuator
   - Wire limit switches and door sensor
   - Connect communication pins to Main Controller

4. **ESP32-CAM Setup**:
   - Connect PIR sensor to GPIO 13
   - Connect night vision LED to GPIO 12
   - Ensure power supply is stable (3.3V)

### Software Setup (Brief)

#### 1. Arduino Controllers
```bash
# Install Arduino IDE
# Add ESP8266/ESP32 board support via Board Manager
# Install libraries: WiFiEsp, SoftwareSerial

# Flash Main Controller
1. Open main-arduino-controller/main_controller.ino
2. Update WiFi credentials (WIFI_SSID, WIFI_PASS)
3. Update server IP if different from 192.168.43.1
4. Upload to Main Arduino

# Flash Lock Controller
1. Open arduino-lock-controller/door_controller.ino
2. Upload to Lock Arduino
```

#### 2. ESP32-CAM
```bash
# Install ESP32 board support
# Install required libraries (usually bundled)

1. Open esp32-camera/CameraWebServer.ino
2. Update WiFi credentials
3. Select "AI Thinker ESP32-CAM" board
4. Upload using FTDI programmer (GPIO 0 to GND for programming mode)
```

#### 3. Android Apps
```bash
# Build Home Client App (Old Phone)
cd home-client-android
./gradlew assembleDebug
# Install app/build/outputs/apk/debug/app-debug.apk on old phone

# Configure MQTT settings in app
# Setup MacroDroid automation for hotspot and scheduled boot

# Build Home Security App (Personal Phone)
cd home-security-app
./gradlew assembleDebug
# Install on personal phone
```

#### 4. Backend Service
```bash
cd home-security-service

# Configure application.properties
# Set database URL, storage type (AWS S3, GCP, or local)

# Build and run
./mvnw clean package
java -jar target/home-security-service-0.0.1-SNAPSHOT.jar

# Or deploy to Digital Ocean using Docker
docker build -t home-security-service .
docker run -p 8080:8080 home-security-service
```

### Initial Configuration
1. **Power on old Android phone** - Ensure hotspot is enabled
2. **Connect all devices to S_HOME WiFi**
3. **Configure MQTT broker** in both Android apps
4. **Test communication** - Send test commands via personal phone app
5. **Verify image capture** - Trigger motion sensor and check Digital Ocean server

For detailed setup instructions, see [SETUP-GUIDE.md](SETUP-GUIDE.md).

---

## Related Documentation

- **[Technical Specification](TECHNICAL-SPECIFICATION.md)** - Detailed architecture, protocols, and wiring diagrams
- **[Setup and Installation Guide](SETUP-GUIDE.md)** - Step-by-step assembly and configuration
- **[API Reference](API-REFERENCE.md)** - Complete API documentation for all endpoints
- **[Troubleshooting Guide](TROUBLESHOOTING.md)** - Common issues and solutions
- **[Security Considerations](SECURITY.md)** - Security analysis and recommendations

---

## System Status & Monitoring

### Operational Indicators
- **Main Arduino**: Sends status every 10 seconds (configurable)
- **ESP32-CAM**: Pings every 10 seconds (configurable)
- **Lock Controller**: Status available when powered on
- **Old Phone**: Battery level, network status
- **MQTT Connection**: Connection status visible in app

### Data Flow Monitoring
```
Every 10 seconds:
  Main Arduino → Old Phone → MQTT → Personal Phone
  ESP32-CAM → Old Phone (ping)

On Event:
  Motion Detection → Image Capture → Upload to Server
  Door State Change → Immediate notification
  Padlock Tamper → Buzzer activation + Alert
```

---

## Future Improvements
See individual component documentation for specific enhancement suggestions. Key areas:
- **Security**: Add encryption for WiFi and MQTT, implement authentication
- **Reliability**: Add watchdog timers, improve error handling
- **Scalability**: Support multiple cameras and controllers
- **Features**: Add face recognition, time-based automation rules
- **Power**: Implement sleep modes for ESP modules

---

## License
Private project - All rights reserved

## Contact
For questions or issues, please refer to the troubleshooting guide or create an issue in the repository.

---

**Last Updated**: February 2026  
**Version**: 1.0  
**Status**: Production (Working System)
