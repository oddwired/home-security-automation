# Home Security System - Setup and Installation Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Hardware Assembly](#hardware-assembly)
3. [Arduino Firmware Setup](#arduino-firmware-setup)
4. [ESP32-CAM Setup](#esp32-cam-setup)
5. [Android Applications Setup](#android-applications-setup)
6. [Backend Service Deployment](#backend-service-deployment)
7. [System Configuration](#system-configuration)
8. [Testing and Validation](#testing-and-validation)
9. [Troubleshooting First Setup](#troubleshooting-first-setup)

---

## Prerequisites

### Hardware Tools Required
- Soldering iron and solder
- Wire strippers
- Multimeter
- Screwdrivers (Phillips and flathead)
- Heat shrink tubing or electrical tape
- Breadboard (for testing)
- Jumper wires (male-to-male, male-to-female)
- FTDI USB-to-Serial adapter (for ESP32-CAM programming)
- USB cables (Mini USB for Arduino Nano)

### Software Tools Required
- **Arduino IDE** 1.8.19 or later ([Download](https://www.arduino.cc/en/software))
- **Android Studio** 2021.1+ ([Download](https://developer.android.com/studio))
- **Java JDK** 8 or 11 (for backend)
- **Maven** 3.6+ (for backend)
- **Git** (for version control)
- Text editor (VS Code, Sublime, etc.)

### Required Libraries

#### Arduino IDE Libraries
Install via Library Manager (Sketch → Include Library → Manage Libraries):
- `WiFiEsp` by bportaluri (for ESP-01)
- `SoftwareSerial` (built-in)

#### ESP32 Board Support
1. File → Preferences → Additional Board Manager URLs:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
2. Tools → Board → Board Manager → Search "ESP32" → Install

### Accounts & Services
- **MQTT Broker**: Sign up for free tier at:
  - HiveMQ Cloud ([https://www.hivemq.com/mqtt-cloud-broker/](https://www.hivemq.com/mqtt-cloud-broker/))
  - CloudMQTT ([https://www.cloudmqtt.com/](https://www.cloudmqtt.com/))
  - Or self-host Mosquitto
- **Digital Ocean** (or AWS/GCP): For image storage server
  - Droplet: 1 GB RAM minimum ($5/month)
  - Or use local server

### Component Purchase Links (Reference)
- Arduino Nano: Search "Arduino Nano ATmega328P"
- ESP-01: Search "ESP8266 ESP-01 WiFi Module"
- ESP32-CAM: Search "ESP32-CAM OV2640 AI-Thinker"
- Linear Actuator: Search "12V DC Linear Actuator 50mm"
- L298N Driver: Search "L298N H-Bridge Motor Driver"
- Relay Module: Search "4 Channel 5V Relay Module"
- PIR Sensor: Search "HC-SR501 PIR Motion Sensor"
- Reed Switches: Search "Magnetic Reed Switch NO"

---

## Hardware Assembly

### Step 1: Power System Setup

#### 1.1 Connect Power Supply to UPS
```
[12V 5A Power Supply]
         │
         ├── Positive (+) ──► [UPS Input +]
         └── Ground (-)   ──► [UPS Input -]

[UPS Output]
         │
         ├── +12V ──► [Main Power Bus]
         └── GND  ──► [Common Ground]
```

#### 1.2 Create Voltage Regulators
**12V to 5V** (for Arduino boards):
```
Option A: 7805 Linear Regulator
  12V ──►[7805]──► 5V (Max 1A)
              │
             GND

Option B: Buck Converter Module (Recommended)
  12V+ ──► IN+
  GND  ──► IN-
  5V   ◄── OUT+ (Adjust trim pot to 5.0V)
  GND  ◄── OUT-
```

**12V to 3.3V** (for ESP-01):
```
Use AMS1117-3.3 or similar:
  5V ──►[AMS1117-3.3]──► 3.3V (Max 800mA)
             │
            GND
```

⚠️ **IMPORTANT**: Verify voltages with multimeter before connecting modules!

---

### Step 2: Main Arduino Controller Assembly

#### 2.1 Pin Connections

**Arduino Nano Pin Layout Reference**:
```
              [USB]
         ┌─────────────┐
      D13│●           ●│VIN (12V via regulator)
      D12│●           ●│GND
      D11│●           ●│RESET
      D10│●           ●│5V
       D9│●           ●│A7
       D8│●           ●│A6
       D7│●           ●│A5
       D6│●           ●│A4
       D5│●           ●│A3
       D4│●           ●│A2
       D3│●           ●│A1
       D2│●           ●│A0
      GND│●           ●│AREF
     RESET│●          ●│3.3V
      RX0│●           ●│D13
      TX1│●           ●│D12
         └─────────────┘
```

#### 2.2 ESP-01 WiFi Module Connection
```
Arduino Nano          ESP-01
    D6 (TX) ──────► RX
    D7 (RX) ◄────── TX
    3.3V    ──────► VCC & CH_PD (enable)
    GND     ──────► GND

Note: ESP-01 requires 3.3V! Do NOT connect to 5V.
```

Add 10µF capacitor between ESP-01 VCC and GND for stability.

#### 2.3 Relay Module Connection
```
Arduino Nano       4-Channel Relay
    D2  ──────────► IN1 (DC Power)
    D4  ──────────► IN2 (Door Light)
    D8  ──────────► IN3 (Alarm/Buzzer)
    D9  ──────────► IN4 (Camera Power)
    5V  ──────────► VCC
    GND ──────────► GND
```

#### 2.4 Sensor Connections
```
Padlock Reed Switch:
    One terminal ──► D3 (PAD_DOOR_PIN)
    Other terminal ─► GND
    (Arduino internal pull-up used)

Door Status from Lock Arduino:
    Lock Arduino D10 ──► Main Arduino D10

DC Status from Lock Arduino:
    Lock Arduino D11 ──► Main Arduino D11

DC Control to Lock Arduino:
    Main Arduino D12 ──► Lock Arduino D12
```

#### 2.5 Relay Output Connections
```
Relay CH1 (DC Power):
    COM ──► 12V+
    NO  ──► Lock Arduino VIN

Relay CH2 (Door Light):
    COM ──► 110V/220V Hot
    NO  ──► Light Bulb
    Light Bulb ──► Neutral

Relay CH3 (Buzzer):
    COM ──► 5V/12V+ (buzzer positive)
    NC  ──► Buzzer negative (active when relay OFF)
    
Relay CH4 (Camera):
    COM ──► 5V+
    NO  ──► ESP32-CAM 5V
```

⚠️ **WARNING**: Relay CH2 handles mains voltage (110V/220V). Use proper wiring and insulation. If unfamiliar, consult an electrician.

---

### Step 3: Lock Controller Arduino Assembly

#### 3.1 Pin Connections
```
Arduino Nano          Connection
    D2  ──────────► L298N IN1
    D3  ──────────► L298N IN2
    D4  ◄────────── Close Button (other side to GND)
    D5  ◄────────── Open Button (other side to GND)
    D6  ◄────────── Limit Switch (HIGH when open)
    D7  ◄────────── Door Sensor Reed Switch
    D10 ──────────► Main Arduino D10 (Door Status)
    D11 ──────────► Main Arduino D11 (DC Status)
    D12 ◄────────── Main Arduino D12 (Control)
    VIN ◄────────── Relay CH1 NO (12V switched)
    GND ──────────► Common Ground
```

#### 3.2 L298N H-Bridge Connection
```
Arduino          L298N           Linear Actuator
   D2  ─────────► IN1
   D3  ─────────► IN2
   5V  ─────────► 5V (Logic)
   GND ─────────► GND
                  12V ◄──── 12V+ (motor power)
                  GND ◄──── GND
                  OUT1 ───► Actuator Wire 1
                  OUT2 ───► Actuator Wire 2
```

**Direction Control**:
- IN1=HIGH, IN2=LOW → Actuator extends (lock closes)
- IN1=LOW, IN2=HIGH → Actuator retracts (lock opens)
- IN1=LOW, IN2=LOW → Stop

#### 3.3 Limit Switch Installation
```
Physical Installation:
- Mount switch where actuator triggers it when fully retracted
- Switch closes (outputs HIGH) when limit reached
- Provides feedback to stop motor

Wiring:
    Switch NO terminal ──► Arduino D6
    Switch COM terminal ──► 5V
    (When triggered: D6 reads HIGH)
```

#### 3.4 Door Sensor (Reed Switch)
```
Mount magnet on door, reed switch on frame:
    Reed Switch terminal 1 ──► Arduino D7
    Reed Switch terminal 2 ──► GND
    
    When door closed: Magnet closes reed switch, D7 = LOW
    When door open: Reed switch open, D7 = HIGH (pull-up)
```

---

### Step 4: ESP32-CAM Module Assembly

#### 4.1 Pin Connections
```
ESP32-CAM             Connection
   GPIO 13  ◄─────── PIR Sensor OUT
   GPIO 12  ──────► Night Vision LED (+ via 220Ω resistor)
   5V       ──────► From Relay CH4 or dedicated 5V supply
   GND      ──────► Common Ground
   
PIR Sensor:
   VCC ──► 5V
   GND ──► GND
   OUT ──► GPIO 13

Night Vision LED:
   GPIO 12 ──► [220Ω Resistor] ──► LED Anode (+)
   LED Cathode (-) ──► GND
```

#### 4.2 Programming Connections (FTDI Adapter)
```
FTDI 3.3V Mode       ESP32-CAM
   3.3V  ──────────► 5V (Yes, this works for programming)
   GND   ──────────► GND
   TX    ──────────► U0R (RX)
   RX    ──────────► U0T (TX)

For Programming Mode:
   GPIO 0 ──► GND (connect before power on)
   
After uploading:
   Disconnect GPIO 0 from GND and reset
```

⚠️ **NOTE**: Some ESP32-CAM boards need external 5V supply during programming (FTDI may not provide enough current).

---

### Step 5: Final Assembly & Wiring

#### 5.1 Create Common Ground Bus
```
All components must share a common ground:
- Power supply ground
- Both Arduino grounds
- ESP-01 ground
- ESP32-CAM ground
- Relay module ground
- All sensor grounds
```

Use a terminal block or breadboard for ground bus.

#### 5.2 Power Distribution
```
12V Bus:
  ├─► Buck converter to 5V (Arduino power)
  ├─► L298N motor power
  ├─► Relay VCC
  └─► Buzzer (if 12V version)

5V Bus:
  ├─► Main Arduino VIN
  ├─► Lock Arduino VIN (via relay)
  ├─► ESP32-CAM (via relay)
  ├─► PIR Sensor
  └─► Linear regulator to 3.3V

3.3V Bus:
  └─► ESP-01 VCC
```

#### 5.3 Cable Management
- Use different colored wires for power (+red), ground (black), signals
- Label all connections with tape/labels
- Secure cables with zip ties
- Keep high-voltage AC wiring separate from low-voltage DC

#### 5.4 Enclosure Mounting
- Mount Arduino boards on standoffs/risers
- Ensure adequate ventilation for voltage regulators
- Protect electronics from moisture
- Provide access to USB ports for debugging

---

## Arduino Firmware Setup

### Step 1: Install Arduino IDE & Libraries

#### 1.1 Download and Install Arduino IDE
```bash
# Linux
sudo apt install arduino

# Or download from arduino.cc
wget https://downloads.arduino.cc/arduino-1.8.19-linux64.tar.xz
tar -xf arduino-1.8.19-linux64.tar.xz
cd arduino-1.8.19
sudo ./install.sh
```

#### 1.2 Install Required Libraries
1. Open Arduino IDE
2. Sketch → Include Library → Manage Libraries
3. Search and install:
   - "WiFiEsp" by bportaluri

### Step 2: Configure Main Arduino Controller

#### 2.1 Open Firmware
```bash
cd home-automation-project1/main-arduino-controller
# Open main_controller.ino in Arduino IDE
```

#### 2.2 Update Configuration
Edit these lines in the code:
```cpp
// WiFi credentials (match your old phone's hotspot)
const char WIFI_SSID[] = "S_HOME";           // Change if using different SSID
const char WIFI_PASS[] = "3030031343dc";     // Change to your hotspot password

// Server (old phone) configuration
const char SERVER[] = "192.168.43.1";        // Old phone hotspot IP
const int PORT = 8080;                       // nanoHTTPD port
const unsigned int udpPort = 8085;           // UDP listen port
```

#### 2.3 Upload Firmware
1. Connect Arduino Nano via USB
2. Tools → Board → Arduino Nano
3. Tools → Processor → ATmega328P (Old Bootloader)
   - Try "ATmega328P" first, if upload fails, try "Old Bootloader"
4. Tools → Port → Select correct COM/tty port
5. Click Upload button (→)

**Expected Output**:
```
Sketch uses 24568 bytes (79%) of program storage space.
Global variables use 1432 bytes (69%) of dynamic memory.
avrdude: done.  Thank you.
```

#### 2.4 Test Serial Output
1. Tools → Serial Monitor (Ctrl+Shift+M)
2. Set baud rate to 19200
3. Press Reset button on Arduino
4. You should see:
```
Initializing WiFi
Connecting to Wifi
Attempting to connect to SSID: S_HOME
...
```

### Step 3: Configure Lock Controller Arduino

#### 3.1 Open Firmware
```bash
cd home-automation-project1/arduino-lock-controller
# Open door_controller.ino in Arduino IDE
```

#### 3.2 Adjust Timing Constants (Optional)
```cpp
const int CLOSE_DOOR_AFTER_SECS = 30;  // Auto-lock delay
// Increase if you need more time after closing door
// Decrease for faster locking
```

#### 3.3 Upload Firmware
1. Connect Lock Controller Arduino via USB
2. Same upload process as Main Controller
3. Verify serial output at 19200 baud

**Expected Output**:
```
(System boots)
OPEN Actuator  or  CLOSE Actuator  (depending on limit switch)
```

---

## ESP32-CAM Setup

### Step 1: Install ESP32 Board Support

#### 1.1 Add Board Manager URL
1. File → Preferences
2. Additional Board Manager URLs:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
3. Click OK

#### 1.2 Install ESP32 Boards
1. Tools → Board → Board Manager
2. Search "ESP32"
3. Install "ESP32 by Espressif Systems" (latest version)

### Step 2: Configure ESP32-CAM Firmware

#### 2.1 Open Firmware
```bash
cd home-automation-project1/esp32-camera
# Open CameraWebServer.ino in Arduino IDE
```

#### 2.2 Update Configuration
```cpp
const char* ssid = "S_HOME";                  // Your hotspot SSID
const char* password = "3030031343dc";        // Your hotspot password
const char SERVER[] = "192.168.43.1";         // Old phone IP
const int PORT = 8080;                        // nanoHTTPD port

// Timing configuration (optional)
unsigned int ping_interval = 10;              // Heartbeat frequency
unsigned int motion_detect_interval = 1;      // Motion check frequency
unsigned int night_vision_timeout = 10;       // Night vision LED timeout
```

#### 2.3 Select Camera Model
Ensure this line is uncommented:
```cpp
#define CAMERA_MODEL_AI_THINKER // Has PSRAM
```

### Step 3: Upload Firmware to ESP32-CAM

#### 3.1 Connect FTDI Programmer
```
FTDI (3.3V mode)    ESP32-CAM
    VCC (or 3.3V) ─► 5V (needs external 5V supply)
    GND           ─► GND
    TX            ─► U0R
    RX            ─► U0T

Programming mode:
    GPIO 0 ─► GND (connect before powering on)
```

#### 3.2 Configure Arduino IDE
1. Tools → Board → ESP32 Arduino → AI Thinker ESP32-CAM
2. Tools → Upload Speed → 115200
3. Tools → Port → Select FTDI port

#### 3.3 Enter Programming Mode
1. Connect GPIO 0 to GND
2. Press Reset button on ESP32-CAM (or power on)
3. LED should flash (indicates boot mode)

#### 3.4 Upload
1. Click Upload
2. Wait for compilation and upload (1-2 minutes)
3. You'll see: "Connecting........____......." (may take 10-20 seconds)

**Expected Output**:
```
Leaving...
Hard resetting via RTS pin...
```

#### 3.5 Exit Programming Mode
1. Disconnect GPIO 0 from GND
2. Press Reset or power cycle
3. Open Serial Monitor (115200 baud)

**Expected Serial Output**:
```
Camera init success
Attempting to connect to SSID: S_HOME
...
WiFi connected
Camera Ready! Use 'http://192.168.43.X' to connect
```

**Note the IP address displayed!**

---

## Android Applications Setup

### Step 1: Setup Development Environment

#### 1.1 Install Android Studio
```bash
# Download from developer.android.com
# Install following the installer instructions
```

#### 1.2 Install Required SDKs
1. Open Android Studio
2. Tools → SDK Manager
3. Install:
   - Android SDK Platform 28 (Android 9.0)
   - Android SDK Build-Tools 28.0.3
   - Android SDK Platform-Tools

### Step 2: Build Home Client App (Old Phone Server)

#### 2.1 Open Project
```bash
cd home-automation-project1/home-client-android
# Open this folder in Android Studio
```

#### 2.2 Sync Gradle
Wait for Gradle sync to complete (may take several minutes on first run)

#### 2.3 Update Configuration
Edit `app/src/main/res/values/strings.xml` or use in-app settings:
```xml
<string name="default_mqtt_server_ip">broker.hivemq.com</string>
<string name="default_mqtt_server_port">1883</string>
<string name="default_mqtt_client_id">home_controller_001</string>
```

Or configure these settings in the app after installation.

#### 2.4 Build APK
```bash
# Via command line
./gradlew assembleDebug

# APK location:
app/build/outputs/apk/debug/app-debug.apk
```

Or use Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)

#### 2.5 Install on Old Phone
```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer APK to phone and install manually
```

### Step 3: Build Home Security App (Personal Phone Client)

#### 3.1 Open Project
```bash
cd home-automation-project1/home-security-app
# Open in Android Studio
```

#### 3.2 Configure MQTT Settings
Same as Home Client app - update MQTT broker details.

#### 3.3 Build and Install
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Configure Old Phone Settings

#### 4.1 Enable WiFi Hotspot
1. Settings → Network & Internet → Hotspot & Tethering
2. WiFi Hotspot settings:
   - SSID: `S_HOME`
   - Password: `3030031343dc`
   - Security: WPA2 PSK
3. Turn on hotspot

#### 4.2 Find Hotspot IP Address
```bash
# Via ADB
adb shell ip addr show wlan0

# Look for:
inet 192.168.43.1/24
```

Usually defaults to 192.168.43.1 for Android hotspot.

#### 4.3 Install MacroDroid (Optional but Recommended)
1. Install MacroDroid from Play Store
2. Create Macro 1: Auto-enable hotspot on boot
   - Trigger: Device Boot
   - Action: Enable WiFi Hotspot
3. Create Macro 2: Scheduled power-on
   - Trigger: Alarm Clock (7:00 AM)
   - Action: Wake Device

#### 4.4 Configure Home Client App
1. Open app
2. Grant required permissions:
   - Location (for WiFi)
   - Phone/SMS (for SMS commands)
   - Storage (for logs)
3. Navigate to Settings
4. Configure:
   - MQTT Broker: your_broker.com
   - MQTT Port: 1883
   - MQTT Client ID: home_controller_001
   - Arduino IP: 192.168.43.x (will auto-discover)
   - Camera IP: 192.168.43.x (will auto-discover)
5. Start service (toggle main switch)

#### 4.5 Configure Personal Phone App
1. Open Home Security app
2. Configure same MQTT settings
3. Test connection (should see "Connected" indicator)

---

## Backend Service Deployment

### Step 1: Setup Digital Ocean Droplet

#### 1.1 Create Droplet
1. Log in to Digital Ocean
2. Create → Droplets
3. Choose:
   - Distribution: Ubuntu 20.04 LTS
   - Plan: Basic ($5/month, 1GB RAM)
   - Datacenter: Closest to your location
   - Authentication: SSH key (recommended) or password
4. Create Droplet

#### 1.2 Connect to Droplet
```bash
ssh root@your_droplet_ip
```

### Step 2: Install Dependencies

#### 2.1 Update System
```bash
apt update && apt upgrade -y
```

#### 2.2 Install Java
```bash
apt install openjdk-11-jdk -y
java -version  # Verify installation
```

#### 2.3 Install PostgreSQL
```bash
apt install postgresql postgresql-contrib -y
systemctl start postgresql
systemctl enable postgresql
```

#### 2.4 Setup Database
```bash
sudo -u postgres psql

# In PostgreSQL prompt:
CREATE DATABASE homesecurity;
CREATE USER securityuser WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE homesecurity TO securityuser;
\q
```

### Step 3: Deploy Application

#### 3.1 Transfer Application
```bash
# On local machine
cd home-automation-project1/home-security-service
./mvnw clean package
scp target/home-security-service-0.0.1-SNAPSHOT.jar root@your_droplet_ip:/opt/
```

#### 3.2 Configure Application
```bash
# On droplet
nano /opt/application.properties
```

Add:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/homesecurity
spring.datasource.username=securityuser
spring.datasource.password=your_secure_password

# Storage configuration
storage.type=local
storage.local.path=/var/lib/home-security/images

# Or for AWS S3:
# storage.type=s3
# storage.s3.bucket=your-bucket-name
# storage.s3.region=us-east-1
# storage.s3.accessKey=YOUR_ACCESS_KEY
# storage.s3.secretKey=YOUR_SECRET_KEY

server.port=8080
```

#### 3.3 Create Storage Directory
```bash
mkdir -p /var/lib/home-security/images
chmod 755 /var/lib/home-security/images
```

#### 3.4 Create Systemd Service
```bash
nano /etc/systemd/system/home-security.service
```

Content:
```ini
[Unit]
Description=Home Security Service
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt
ExecStart=/usr/bin/java -jar /opt/home-security-service-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/application.properties
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

#### 3.5 Start Service
```bash
systemctl daemon-reload
systemctl start home-security
systemctl enable home-security
systemctl status home-security
```

#### 3.6 Verify
```bash
curl http://localhost:8080/files
# Should return empty list or existing files
```

### Step 4: Configure Firewall

```bash
# Allow SSH, HTTP, HTTPS
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 8080/tcp
ufw enable

# Verify
ufw status
```

### Step 5: Setup Nginx Reverse Proxy (Optional but Recommended)

```bash
apt install nginx -y

nano /etc/nginx/sites-available/home-security
```

Content:
```nginx
server {
    listen 80;
    server_name your_domain.com;

    client_max_body_size 10M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```bash
ln -s /etc/nginx/sites-available/home-security /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx
```

---

## System Configuration

### Step 1: Network Configuration

#### 1.1 Verify WiFi Hotspot
- Old phone hotspot should be active
- SSID: S_HOME
- All devices should connect to this network

#### 1.2 Check IP Assignments
```bash
# Old phone (should be)
192.168.43.1

# Check Arduino IPs (from serial monitor)
Main Arduino: 192.168.43.x
ESP32-CAM: 192.168.43.y

# Update in Android app settings if different
```

### Step 2: MQTT Broker Configuration

#### 2.1 Create MQTT Account
For HiveMQ Cloud:
1. Sign up at console.hivemq.cloud
2. Create cluster (free tier)
3. Note:
   - Broker URL: xxxxxxxxx.s1.eu.hivemq.cloud
   - Port: 8883 (TLS) or 1883 (TCP)
   - Username: your_username
   - Password: your_password

#### 2.2 Configure Apps
Update both Android apps with:
- MQTT Server IP/URL
- Port
- Username/Password
- Client ID (unique per app)

### Step 3: Configure Timeouts & Intervals

#### 3.1 Via Personal Phone App
1. Open Home Security app
2. Settings → Configuration
3. Adjust:
   - Lock timeout: 30s (default)
   - Data send interval: 10s
   - Alarm timeout: 2s
   - Reset interval: 1800s (30 minutes)

#### 3.2 Send Configuration
Configuration will be sent via MQTT ConfigTopic and applied on next status update.

---

## Testing and Validation

### Step 1: Component Testing

#### 1.1 Test Main Arduino
```bash
# Serial monitor at 19200 baud
# Should see:
Initializing WiFi
Connecting to Wifi
Connected successfully
Sending data to server: 0,0,0,0,0,1
```

#### 1.2 Test Lock Arduino
```bash
# Power on Lock Arduino
# Serial should show:
OPEN Actuator (or CLOSE Actuator)

# Press manual buttons - actuator should move
```

#### 1.3 Test ESP32-CAM
```bash
# Serial monitor at 115200 baud
WiFi connected
Camera Ready! Use 'http://192.168.43.X' to connect

# Open browser to http://192.168.43.X
# You should see camera web interface
```

### Step 2: Integration Testing

#### 2.1 Test Door Lock/Unlock
1. Open Personal Phone app
2. Tap "Unlock Door"
3. Verify:
   - Command sent via MQTT
   - Old phone receives command
   - UDP sent to Arduino
   - Lock Arduino powers on
   - Actuator retracts
   - Status updates in app

**Expected Duration**: 5-8 seconds

#### 2.2 Test Motion Detection
1. Wave hand in front of PIR sensor
2. Verify:
   - ESP32 sends /cam/motion to old phone
   - Old phone triggers image capture
   - Image uploaded to Digital Ocean
   - Notification on personal phone

**Expected Duration**: 3-5 seconds

#### 2.3 Test Alarm System
1. Enable alarm in app (if disarmed)
2. Open padlock cover (simulate tamper)
3. Verify:
   - Buzzer activates
   - Alarm state updates in app
   - Buzzer turns off after timeout (2s default)

### Step 3: End-to-End Testing

#### 3.1 Simulate Full Scenario
1. Close door (with door unlocked)
2. Verify auto-lock after 30 seconds
3. Unlock door remotely via app
4. Open door
5. Close door
6. Wait for auto-lock
7. Test motion detection
8. Test alarm tamper

#### 3.2 Test SMS Fallback
1. Disable WiFi/mobile data on personal phone
2. Send SMS to old phone: `d0`
3. Verify door unlocks
4. Re-enable data, check SMS log in app

---

## Troubleshooting First Setup

### Arduino Won't Connect to WiFi

**Problem**: "WiFi shield not present" or "Failed to connect"

**Solutions**:
1. Check ESP-01 wiring (especially RX/TX swap)
2. Verify 3.3V power supply (not 5V!)
3. Ensure WiFi hotspot is enabled
4. Check SSID/password spelling
5. Move Arduino closer to phone
6. Add capacitor (10µF) to ESP-01 power pins
7. Try slower baud rate (9600 instead of 19200)

### Lock Actuator Not Moving

**Problem**: No response when sending unlock command

**Solutions**:
1. Check L298N connections
2. Verify 12V power to motor driver
3. Test actuator directly (connect to 12V briefly)
4. Check relay is clicking (DC power relay)
5. Verify Lock Arduino is powered (DC_STATUS_PIN)
6. Test with manual buttons on Lock Arduino
7. Check inter-Arduino communication wires

### ESP32-CAM Not Capturing Images

**Problem**: Motion detected but no image

**Solutions**:
1. Check camera ribbon cable connection
2. Verify sufficient power (500mA+)
3. Test /capture endpoint directly in browser
4. Check Digital Ocean server is reachable
5. View ESP32 serial monitor for error messages
6. Reduce image quality/size in code
7. Verify PSRAM is detected (check serial output)

### Android App Not Receiving Status

**Problem**: No updates in personal phone app

**Solutions**:
1. Verify MQTT broker is accessible
2. Check MQTT credentials
3. Ensure both apps use same Client ID prefix
4. Check topic names match
5. Verify old phone has internet connection
6. Test MQTT with tool like MQTT Explorer
7. Check app permissions (network, background)

### Image Upload Fails

**Problem**: ESP32 captures but doesn't upload

**Solutions**:
1. Check Digital Ocean server is running
2. Verify firewall allows port 8080
3. Test upload with curl:
   ```bash
   curl -X POST -F "file=@test.jpg" http://your_server:8080/upload
   ```
4. Check PostgreSQL is running
5. Verify storage path exists and is writable
6. Check application logs on server:
   ```bash
   journalctl -u home-security -f
   ```

---

**Setup Complete!**

Your home security system should now be fully operational. Proceed to normal usage and refer to the Troubleshooting Guide for any issues that arise during operation.

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Estimated Setup Time**: 8-12 hours (hardware + software)
