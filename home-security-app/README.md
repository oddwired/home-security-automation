# Home Security App (Personal Phone)

## Overview

The **Home Security App** is the remote control interface installed on your personal phone. It provides real-time monitoring, remote control, and notifications for the home security system via MQTT. This app communicates with the Old Phone (central hub) through an MQTT broker.

**Platform**: Android 9.0+ (API 28+)  
**Language**: Java  
**Role**: Remote monitoring and control

---

## Key Features

- 📊 **Real-Time Monitoring**: View door status, alarm state, light status
- 🎮 **Remote Control**: Lock/unlock door, control lights, manage alarm
- 🔔 **Push Notifications**: Instant alerts for motion, door events, alarms
- �� **Image Viewing**: View captured motion detection images
- ⚙️ **Configuration**: Adjust system parameters remotely
- 📱 **SMS Fallback**: Send commands via SMS when data unavailable
- 🔐 **Secure**: MQTT with optional TLS support

---

## Main Features

### Dashboard
- **Door Status**: Locked/Unlocked indicator
- **Alarm Status**: Armed/Disarmed state
- **Light Status**: On/Off indicator
- **Camera Status**: Online/Offline
- **Last Update**: Timestamp of last status

### Control Panel
- **Unlock Door**: Button to unlock remotely
- **Lock Door**: Button to lock remotely
- **Toggle Alarm**: Arm/disarm alarm system
- **Toggle Light**: Turn outdoor light on/off
- **Camera Control**: Power cycle camera

### Notifications
- **Motion Detected**: With image preview
- **Door Unlocked**: Alert when door opens
- **Alarm Triggered**: When padlock tampered
- **System Offline**: When connection lost

### Settings
- **MQTT Configuration**: Broker URL, port, credentials
- **Topics**: Configure MQTT topic structure
- **Notifications**: Enable/disable specific alerts
- **Auto-connect**: Connect on app start
- **SMS Fallback**: Configure SMS commands

---

## MQTT Topics

### Subscribe To (Receive Updates)

**`Home/<clientId>/data`** - System status  
Format:
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

**`Home/<clientId>/motion`** - Motion events  
Format:
```json
{
  "detected": true,
  "timestamp": 1644678900,
  "imageUrl": "https://server.com/files/abc123"
}
```

**`Home/<clientId>/device`** - Device health  
Format:
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

### Publish To (Send Commands)

**`Home/<clientId>/commands`** - Control commands  
Format:
```json
{
  "commands": ["d0", "al0", "dl1"],
  "timestamp": 1644678900
}
```

**`Home/<clientId>/config`** - Configuration updates  
Format:
```json
{
  "lockTimeout": 30,
  "dataInterval": 10,
  "alarmArmed": 1
}
```

---

## Setup Instructions

### 1. Build APK

#### Using Android Studio
1. Open project in Android Studio
2. Wait for Gradle sync
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Using Command Line
```bash
cd home-security-app
./gradlew assembleDebug
```

### 2. Install on Personal Phone

#### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Manual Installation
1. Transfer APK to phone
2. Enable "Install from Unknown Sources"
3. Open APK file
4. Click Install

### 3. Configure MQTT

1. Open app
2. Go to Settings
3. Enter MQTT details:
   - **Broker**: broker.hivemq.com (or your broker)
   - **Port**: 1883 (or 8883 for TLS)
   - **Username**: (if required)
   - **Password**: (if required)
   - **Client ID**: home_controller_001
4. Save settings

### 4. Grant Permissions

Required permissions:
- ✅ **Internet**: For MQTT connection
- ✅ **Network State**: Check connectivity
- ✅ **Notifications**: Display alerts
- ✅ **SMS** (Optional): For SMS fallback commands
- ✅ **Vibrate**: Notification feedback

### 5. Test Connection

1. Start MQTT service (toggle in app)
2. Check "Connected" indicator
3. Verify status updates appear
4. Test command (unlock door)
5. Confirm command executed

---

## Usage

### Unlock Door
1. Open app
2. Tap "Unlock Door" button
3. Wait 5-8 seconds
4. Status updates to "Unlocked"

### Lock Door
1. Tap "Lock Door" button
2. Wait 8 seconds
3. Status updates to "Locked"

### Toggle Alarm
1. Tap "Alarm" button
2. Confirms armed/disarmed state
3. Buzzer will trigger if tampered (when armed)

### View Motion Images
1. Receive motion notification
2. Tap notification
3. View captured image
4. Image displayed from cloud URL

### SMS Fallback
When data connection unavailable:
1. Open SMS app
2. Send command to old phone number:
   - `d0` - Unlock
   - `d1` - Lock
   - `al0` - Turn off alarm
3. Command executed via SMS

---

## Configuration Options

### MQTT Settings
- **Broker URL**: MQTT broker address
- **Port**: 1883 (plain) or 8883 (TLS)
- **Use TLS**: Enable encrypted connection
- **Username**: Broker authentication
- **Password**: Broker authentication
- **Client ID**: Unique identifier
- **Keep Alive**: Connection timeout (seconds)

### Topic Configuration
- **Base Topic**: `Home`
- **Client ID**: `home_controller_001`
- **Commands Topic**: `Home/<clientId>/commands`
- **Data Topic**: `Home/<clientId>/data`
- **Motion Topic**: `Home/<clientId>/motion`

### Notification Settings
- **Enable Motion Alerts**: Yes/No
- **Enable Door Alerts**: Yes/No
- **Enable Alarm Alerts**: Yes/No
- **Sound**: Enable notification sound
- **Vibrate**: Enable vibration
- **LED**: Enable notification LED

### Display Settings
- **Auto-Refresh**: Update interval (seconds)
- **Show Timestamp**: Display last update time
- **Dark Mode**: Enable dark theme
- **Keep Screen On**: Prevent sleep when viewing

---

## Troubleshooting

### App Won't Connect to MQTT
- Check broker URL and port
- Verify internet connection
- Test broker with MQTT Explorer
- Check username/password
- Try different network
- Enable TLS if broker requires

### Not Receiving Status Updates
- Verify old phone app is running
- Check old phone has internet
- Verify topic names match
- Check MQTT broker logs
- Restart both apps

### Commands Not Executing
- Check "Connected" indicator is green
- Verify command sent (check logs)
- Ensure old phone receives command
- Check Arduino is responding
- Test with UDP command directly

### Notifications Not Appearing
- Grant notification permission
- Check notification settings in app
- Verify Android notification settings
- Check Do Not Disturb mode
- Restart app

### High Battery Drain
- Reduce MQTT keep-alive interval
- Disable auto-refresh when not in use
- Check for background wake locks
- Use battery optimization settings
- Update to latest version

---

## Development

### Requirements
- Android Studio 2021.1+
- Android SDK 28+
- Java JDK 8 or 11
- Gradle 7.0+

### Dependencies
```gradle
// MQTT Client
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

// UI
implementation 'com.google.android.material:material:1.4.0'
implementation 'androidx.appcompat:appcompat:1.3.1'

// Image Loading
implementation 'com.squareup.picasso:picasso:2.8'

// HTTP Client
implementation 'com.squareup.okhttp3:okhttp:4.9.0'
```

### Build Variants
- **Debug**: Development build with logging
- **Release**: Production build, ProGuard enabled

### Logging
View logs:
```bash
adb logcat | grep "HomeSecurity"
```

---

## Security Considerations

### Current Implementation
- MQTT over plain TCP (port 1883)
- No command authentication
- No message encryption

### Recommended Improvements
- ✅ Enable MQTT over TLS (port 8883)
- ✅ Add certificate pinning
- ✅ Implement command signing
- ✅ Add PIN/biometric authentication
- ✅ Rate limiting on commands
- See [SECURITY.md](../SECURITY.md)

---

## Features in Development

### Planned Features
- [ ] Biometric authentication
- [ ] Two-factor authentication
- [ ] Command history log
- [ ] Scheduled actions (unlock at 7am)
- [ ] Geofencing (auto-unlock when home)
- [ ] Widget support
- [ ] Wear OS app
- [ ] Voice commands (Google Assistant)

---

## Related Documentation

- [Home Client App](../home-client-android/README.md) - Old phone server
- [API Reference](../API-REFERENCE.md) - MQTT topics and messages
- [Technical Specification](../TECHNICAL-SPECIFICATION.md) - Architecture
- [Security Guide](../SECURITY.md) - Security improvements
- [Troubleshooting Guide](../TROUBLESHOOTING.md) - Problem solving

---

**Platform**: Android 9.0+  
**Min SDK**: 28  
**Target SDK**: 30  
**Version**: 1.0  
**Status**: Production Ready
