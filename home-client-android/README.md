# Home Client App (Old Phone - Central Hub)

## Overview

The **Home Client App** runs on the old Android phone, serving as the central hub that coordinates all system components. It runs a nanoHTTPD server for local communication, an MQTT client for cloud connectivity, and manages automation via MacroDroid.

**Platform**: Android 9.0+ (API 28+)  
**Language**: Java  
**Role**: Central coordination hub

---

## Key Responsibilities

- 🌐 **HTTP Server**: nanoHTTPD server on port 8080
- 📡 **MQTT Bridge**: Connect local devices to cloud
- 📸 **Image Management**: Capture and upload images to cloud
- 📱 **SMS Gateway**: Process SMS commands when data unavailable
- 🔄 **State Coordination**: Manage system state and configuration
- 🤖 **Automation**: MacroDroid integration for scheduled tasks
- 📊 **Data Logging**: Local database for events and logs

---

## Main Features

### nanoHTTPD Server

**Port**: 8080  
**IP**: 192.168.43.1 (WiFi hotspot)

**Endpoints**:
- `POST /data` - Receive Arduino status updates
- `POST /cam/motion` - Motion detection alerts
- `POST /cam/ping` - Camera heartbeat
- `POST /cam/boot` - Camera startup notification

### MQTT Client

**Subscribes To**:
- `Home/<clientId>/commands` - Commands from personal phone
- `Home/<clientId>/config` - Configuration updates

**Publishes To**:
- `Home/<clientId>/data` - System status updates
- `Home/<clientId>/motion` - Motion detection events
- `Home/<clientId>/device` - Device health information
- `Home/<clientId>/sms` - SMS command logs

### WiFi Hotspot

**SSID**: S_HOME  
**Password**: (configured)  
**IP Address**: 192.168.43.1  
**DHCP Range**: 192.168.43.2-254

**Connected Devices**:
- Main Arduino (ESP-01)
- ESP32-CAM
- Personal Phone (when on WiFi)

### SMS Command Processing

Receives SMS from personal phone:
- Parse command (`d0`, `d1`, etc.)
- Validate sender
- Send UDP to Arduino or HTTP POST
- Publish result to MQTT (log)
- Optional: Send reply SMS

---

## Setup Instructions

### 1. Build APK

#### Using Android Studio
```bash
cd home-client-android
# Open in Android Studio
# Build → Build APK
```

#### Using Gradle
```bash
./gradlew assembleDebug
```

### 2. Install on Old Phone

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Configure WiFi Hotspot

1. Settings → Network & Internet → Hotspot
2. WiFi Hotspot Name: **S_HOME**
3. Password: (set strong password)
4. Security: WPA2 PSK
5. Band: 2.4 GHz (required for ESP modules)
6. Turn ON hotspot

### 4. Configure App

1. Open Home Client app
2. Grant permissions:
   - ✅ Location (for WiFi)
   - ✅ Phone/SMS
   - ✅ Storage
   - ✅ Network
3. Settings:
   - **MQTT Broker**: broker.hivemq.com
   - **MQTT Port**: 1883
   - **MQTT Client ID**: home_controller_001
   - **Arduino IP**: (auto-discovered or manual)
   - **Camera IP**: (auto-discovered or manual)
4. Start service (main toggle)

### 5. Install MacroDroid (Optional)

**Purpose**: Auto-enable hotspot on boot, scheduled wake

1. Install MacroDroid from Play Store
2. Create Macro: "Auto-enable Hotspot"
   - Trigger: Device Boot
   - Action: Enable WiFi Hotspot
3. Create Macro: "Scheduled Wake"
   - Trigger: Time (7:00 AM)
   - Action: Wake Device + Enable Hotspot
4. Grant MacroDroid required permissions

### 6. Power Settings

1. Settings → Battery → Battery Optimization
2. Find Home Client app
3. Select "Don't optimize"
4. Keep phone plugged in (powered)
5. Disable auto-sleep: Settings → Display → Sleep: Never

---

## Operation

### Normal Operation Flow

```
1. Boot:
   - MacroDroid auto-enables hotspot
   - App starts automatically (if configured)
   - Connect to MQTT broker
   - Start nanoHTTPD server on port 8080

2. Every 10 seconds:
   - Receive POST /data from Main Arduino
   - Parse sensor states
   - Publish to MQTT (data topic)
   - Return commands and config to Arduino

3. On motion detection:
   - Receive POST /cam/motion from ESP32-CAM
   - Send GET /capture to ESP32-CAM
   - Upload image to Digital Ocean server
   - Publish motion event to MQTT

4. On SMS received:
   - Parse command from message
   - Validate sender
   - Send UDP command to Arduino
   - Publish SMS log to MQTT
```

### HTTP Server Request Handling

**POST /data** (Arduino Status):
```
1. Receive: "0,0,1,0,0,1"
2. Parse into: doorOpen, padlockOpen, dcState, alarmState, lightState, alarmArmed
3. Update local state
4. Publish to MQTT data topic
5. Check for pending commands from MQTT
6. Format response: "commands:config"
7. Send response: "d0,al0:30,10,1800,0,1,2"
```

**POST /cam/motion** (Motion Alert):
```
1. Receive: "Motion detected"
2. Send GET http://192.168.43.X/capture
3. Receive JPEG image
4. Upload to POST https://server.com/upload
5. Get image URL from response
6. Publish to MQTT motion topic with URL
7. Send push notification
```

---

## Configuration

### nanoHTTPD Server Settings
```java
private static final int SERVER_PORT = 8080;
private static final String SERVER_IP = "192.168.43.1";
```

### MQTT Settings
```java
String mqttBroker = "tcp://broker.hivemq.com:1883";
String mqttClientId = "home_controller_001";
String mqttUsername = "";  // Optional
String mqttPassword = "";  // Optional
int mqttKeepAlive = 60;
```

### Topic Configuration
```java
String BASE_TOPIC = "Home";
String commandsTopic = BASE_TOPIC + "/" + clientId + "/commands";
String dataTopic = BASE_TOPIC + "/" + clientId + "/data";
String motionTopic = BASE_TOPIC + "/" + clientId + "/motion";
String configTopic = BASE_TOPIC + "/" + clientId + "/config";
String deviceTopic = BASE_TOPIC + "/" + clientId + "/device";
String smsTopic = BASE_TOPIC + "/" + clientId + "/sms";
```

### Digital Ocean Server
```java
String imageServerUrl = "https://your-server.com/upload";
String imageServerAuth = "Bearer YOUR_TOKEN";  // Optional
```

---

## Database

### ObjectBox Database

**Entities**:
- **StatusLog**: System status history
- **MotionEvent**: Motion detection events
- **Command**: Command execution log
- **SMSLog**: SMS command log
- **ConfigChange**: Configuration change history

**Example Query**:
```java
// Get last 50 motion events
List<MotionEvent> events = motionEventBox
    .query()
    .orderDesc(MotionEvent_.timestamp)
    .build()
    .find(0, 50);
```

---

## Troubleshooting

### nanoHTTPD Server Not Starting
- Check port 8080 not in use
- Verify network permission granted
- Check WiFi hotspot is active
- Review logs: `adb logcat | grep "nanoHTTPD"`

### Arduino Can't Connect
- Verify hotspot is enabled
- Check SSID and password in Arduino code
- Test ping: `ping 192.168.43.1`
- Check firewall on phone
- Ensure Arduino on same network

### MQTT Connection Failed
- Verify internet connection on phone
- Test broker: `mosquitto_pub -h broker.hivemq.com -t test -m test`
- Check credentials if required
- Try different broker
- Check app logs

### Images Not Uploading
- Verify Digital Ocean server is running
- Test upload with curl
- Check image server URL in code
- Verify phone has internet (not just hotspot)
- Check storage permission

### SMS Commands Not Working
- Grant SMS permission in Android settings
- Check SmsReceiver is registered
- View logs when SMS received
- Verify sender phone number

### High Battery Drain
- Keep phone plugged in (designed for powered operation)
- Reduce MQTT message frequency
- Disable unnecessary features
- Check for background wake locks
- Optimize database queries

---

## Development

### Requirements
- Android Studio 2021.1+
- Android SDK 28+
- Java JDK 8 or 11
- Gradle 7.0+

### Dependencies
```gradle
// HTTP Server
implementation 'org.nanohttpd:nanohttpd:2.3.1'

// MQTT
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

// Database
implementation 'io.objectbox:objectbox-android:3.0.0'

// HTTP Client
implementation 'com.squareup.okhttp3:okhttp:4.9.0'

// JSON
implementation 'com.google.code.gson:gson:2.8.8'
```

### Logging
```bash
# View app logs
adb logcat -s HomeClient:D

# View all logs
adb logcat | grep -i "home"

# Save logs to file
adb logcat > client_logs.txt
```

---

## Performance

### Resource Usage
- **CPU**: 5-10% (idle), 15-30% (active)
- **RAM**: 100-200 MB
- **Storage**: 50-500 MB (database grows with events)
- **Network**: 1-5 KB/s (status updates), 50-100 KB/s (image upload)
- **Battery**: Designed for plugged-in operation

### Optimization Tips
- Clear old database entries periodically
- Reduce status update frequency if needed
- Use image compression
- Limit log retention
- Disable debug logging in production

---

## Security

### Current Implementation
- No authentication on nanoHTTPD
- MQTT without TLS
- Images uploaded unencrypted
- SMS sender not validated

### Recommended Improvements
- Add API key to HTTP endpoints
- Enable MQTT over TLS
- Encrypt images before upload
- Validate SMS sender phone number
- Add rate limiting
- See [SECURITY.md](../SECURITY.md)

---

## Backup & Restore

### Export Database
```bash
adb pull /data/data/com.kshem.homeclient/files/objectbox ./backup/
```

### Export Configuration
Settings → Backup/Export → Save to file

### Restore
1. Reinstall app
2. Settings → Restore
3. Select backup file

---

## Related Documentation

- [Home Security App](../home-security-app/README.md) - Personal phone app
- [Main Arduino Controller](../main-arduino-controller/README.md) - System coordinator
- [ESP32-CAM](../esp32-camera/README.md) - Camera module
- [API Reference](../API-REFERENCE.md) - Complete API docs
- [Technical Specification](../TECHNICAL-SPECIFICATION.md) - Architecture
- [Troubleshooting Guide](../TROUBLESHOOTING.md) - Problem solving

---

**Platform**: Android 9.0+  
**Server Port**: 8080  
**Hotspot IP**: 192.168.43.1  
**Role**: Central Hub  
**Version**: 1.0  
**Status**: Production Ready
