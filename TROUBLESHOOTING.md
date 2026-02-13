# Home Security System - Troubleshooting Guide

## Table of Contents
1. [General Troubleshooting Approach](#general-troubleshooting-approach)
2. [Power & Hardware Issues](#power--hardware-issues)
3. [Arduino Communication Issues](#arduino-communication-issues)
4. [ESP32-CAM Problems](#esp32-cam-problems)
5. [Android Application Issues](#android-application-issues)
6. [Network Connectivity](#network-connectivity)
7. [MQTT Issues](#mqtt-issues)
8. [Backend Server Problems](#backend-server-problems)
9. [Mechanical Issues](#mechanical-issues)
10. [Performance Problems](#performance-problems)
11. [Security Incidents](#security-incidents)
12. [Maintenance Procedures](#maintenance-procedures)

---

## General Troubleshooting Approach

### Step-by-Step Diagnosis

1. **Identify the Symptom**: What is not working?
2. **Check Power**: Is everything powered on?
3. **Check Connectivity**: Are devices connected to network?
4. **Check Logs**: Review serial monitors, app logs, server logs
5. **Isolate the Component**: Test each component individually
6. **Check Recent Changes**: What was modified recently?
7. **Verify Configuration**: Are settings correct?

### Essential Tools

- **Multimeter**: Measure voltages and continuity
- **Serial Monitor**: Arduino IDE, 19200/115200 baud
- **MQTT Explorer**: Test MQTT messages ([MQTT Explorer](http://mqtt-explorer.com/))
- **adb (Android Debug Bridge)**: View Android logs
- **SSH Client**: Access Digital Ocean server
- **Web Browser**: Test HTTP endpoints

### Common Quick Fixes

1. **Power cycle everything**: Turn off, wait 30s, turn on
2. **Restart old phone app**: Force stop and relaunch
3. **Reset Arduino**: Press reset button
4. **Reconnect WiFi**: Disable/enable hotspot
5. **Clear app cache**: Android Settings → Apps → Clear cache

---

## Power & Hardware Issues

### System Completely Dead

**Symptom**: Nothing powers on, no LEDs lit

**Diagnosis**:
```bash
# Check with multimeter
1. Measure main power supply output: Should be 12V DC
2. Measure after UPS: Should be 12V DC
3. Measure 5V regulator output: Should be 5.0V ± 0.2V
4. Measure 3.3V regulator output: Should be 3.3V ± 0.1V
```

**Solutions**:
- Check power supply is plugged in and switch is ON
- Test power supply with different load
- Check UPS battery charge level
- Look for blown fuses or burned components
- Verify all ground connections are secure
- Check for short circuits (resistance between power and ground)

**Replacement**: If power supply failed, replace with 12V 5A+ supply

---

### Arduino Powers On But No Activity

**Symptom**: Arduino has power LED, but no serial output or functionality

**Diagnosis**:
```bash
# Serial Monitor Test
1. Open Serial Monitor at 19200 baud (Main) or 115200 (ESP32)
2. Press Reset button on Arduino
3. Should see initialization messages
```

**Solutions**:

**No serial output at all**:
- Wrong baud rate: Try 9600, 19200, 57600, 115200
- Wrong COM port selected
- USB cable is power-only (no data lines)
- Arduino bootloader corrupted: Re-upload bootloader

**Outputs garbage characters**:
- Baud rate mismatch: Confirm 19200 for Arduino Nano
- ESP-01 is outputting debug at different rate
- Loose TX/RX connections

**Powers on then immediately off**:
- Short circuit in connected components
- Insufficient power supply current
- Faulty Arduino: Test with nothing connected

---

### Voltage Regulator Getting Hot

**Symptom**: 7805 or other regulator is very hot to touch

**Diagnosis**:
- Normal: Warm (40-60°C)
- Too hot: >70°C, cannot hold finger on it for 1 second

**Causes**:
- Excessive current draw (>1A for 7805)
- Insufficient heat sinking
- Input voltage too high (>15V for 7805)

**Solutions**:
- Add heat sink to regulator
- Use buck converter instead (more efficient)
- Reduce load by using separate regulated supplies
- Verify input voltage is within spec

---

### ESP-01 or ESP32 Not Stable

**Symptom**: Random resets, WiFi disconnects, brownouts

**Common Cause**: Insufficient or noisy power supply

**Solutions**:
- Add 10µF capacitor across ESP power pins (as close as possible)
- Add 100µF capacitor across power supply
- Use dedicated 3.3V/5V regulator, not from Arduino
- Reduce distance from power source
- Use thicker wires for power
- Ensure common ground between all components

**ESP32-CAM Specific**:
- Requires stable 5V, 500mA+ 
- Add 470µF capacitor across 5V input
- Use external 5V supply, not from FTDI

---

### Relay Not Clicking

**Symptom**: No audible click when relay should activate

**Diagnosis**:
```bash
# Measure GPIO pin voltage
1. Trigger relay via command
2. Measure voltage at relay IN pin
3. Should be 0V (LOW) or 5V (HIGH)
```

**Solutions**:
- Check Arduino pin is set correctly (OUTPUT mode)
- Verify relay module VCC and GND connections
- Test relay with direct connection to 5V/GND
- Check if relay is active HIGH or active LOW
- Relay may be burned out: Test with multimeter continuity on COM-NO contacts
- Replace relay module if faulty

---

## Arduino Communication Issues

### Main Arduino Not Connecting to WiFi

**Symptom**: Serial shows "WiFi shield not present" or "Failed to connect"

**Step 1: Check ESP-01 Connections**
```
Verify wiring:
  Arduino D6 (TX) → ESP-01 RX ✓
  Arduino D7 (RX) → ESP-01 TX ✓
  3.3V → ESP-01 VCC ✓
  3.3V → ESP-01 CH_PD ✓
  GND → ESP-01 GND ✓
```

**Step 2: Test ESP-01 Directly**
```bash
# Use USB-to-Serial adapter at 115200 baud
# Connect to ESP-01 (TX to RX, RX to TX)
# Send: AT
# Should respond: OK

# Test WiFi:
AT+CWMODE=1
AT+CWJAP="S_HOME","3030031343dc"
# Should connect
```

**Step 3: Verify Voltage**
- Measure ESP-01 VCC: Must be 3.3V (NOT 5V!)
- If 5V, ESP-01 may be damaged, replace

**Step 4: Check SoftwareSerial**
- Ensure SoftwareSerial library is installed
- Try different baud rate (9600)
- Swap TX/RX if still not working (in case wiring is reversed)

**Step 5: Update WiFiEsp Library**
- Sketch → Include Library → Manage Libraries
- Search "WiFiEsp"
- Update to latest version

---

### Main Arduino and Lock Arduino Not Communicating

**Symptom**: Unlock command does nothing, Lock Arduino doesn't respond

**Diagnosis**:
```bash
# Test with multimeter
1. Send unlock command
2. Measure Main Arduino D12 (DC_CONTROL_PIN)
   - Should pulse HIGH then LOW
3. Measure Lock Arduino D12
   - Should read same voltage as Main Arduino D12
4. Measure Lock Arduino D11 (DC_STATUS_PIN)
   - Should go LOW (ACK) then HIGH
```

**Solutions**:

**No voltage on DC_CONTROL_PIN**:
- Main Arduino not executing command
- Check UDP command reception (see Arduino logs)
- Verify code is uploaded correctly

**Voltage present but Lock Arduino doesn't respond**:
- Check wire continuity between D12 pins
- Verify Lock Arduino is powered (D11 should be HIGH)
- Check relay CH1 is supplying power to Lock Arduino

**Voltage flickers but actuator doesn't move**:
- Check L298N motor driver connections
- Verify 12V supply to L298N
- Test actuator directly with 12V battery

---

### Data Not Reaching Old Phone Server

**Symptom**: Arduino sends data but phone doesn't receive

**Step 1: Verify Server Is Running**
```bash
# From PC on same WiFi
curl http://192.168.43.1:8080/data -X POST -d "0,0,0,0,0,0"
# Should get response (may be empty: :)
```

**Step 2: Check Phone IP**
```bash
# Via ADB or in phone settings
adb shell ip addr show wlan0
# Look for inet 192.168.43.1
```

**Step 3: Check Arduino Serial**
```
Look for:
"Sending data to server: 0,0,0,0,0,0"
"connected to server"
"disconnecting from server"

If "Error connecting to server":
- Server not reachable
- Firewall on phone blocking
- Wrong IP address in Arduino code
```

**Step 4: Test Server Endpoint**
```bash
# From Arduino serial, note IP it's using
# Then test from PC:
telnet 192.168.43.1 8080
POST /data HTTP/1.1
Content-Length: 11

0,0,0,0,0,0
```

---

## ESP32-CAM Problems

### Camera Not Booting

**Symptom**: No serial output, no web server

**Solutions**:

**Check Power**:
- Measure 5V input: Should be 5.0V ± 0.25V
- ESP32-CAM draws 200-500mA, ensure supply can provide this
- Add 470µF capacitor across 5V input

**Check GPIO 0**:
- If GPIO 0 is connected to GND, ESP32 enters programming mode
- Disconnect GPIO 0 and reset

**Re-upload Firmware**:
- Enter programming mode (GPIO 0 to GND, power on)
- Upload firmware
- Disconnect GPIO 0, reset

---

### Camera Captures But Images Are Corrupted

**Symptom**: Images have artifacts, lines, colors wrong

**Solutions**:

**Power Issue**:
- Most common cause
- Add larger capacitor (1000µF) across 5V
- Use dedicated power supply

**Camera Ribbon Cable**:
- Re-seat camera ribbon cable
- Ensure it's fully inserted and latched
- Check for damage to cable

**Reduce Resolution**:
```cpp
// In code, change from SVGA to QVGA
config.frame_size = FRAMESIZE_QVGA;  // 320x240 instead of 800x600
```

**Reduce JPEG Quality**:
```cpp
config.jpeg_quality = 20;  // Increase number (lower quality, faster)
```

---

### Motion Detection Not Working

**Symptom**: PIR sensor triggers but no alert sent

**Diagnosis**:
```bash
# Serial Monitor at 115200
# Wave hand in front of sensor
# Should see: "Motion detected"
```

**Solutions**:

**No "Motion detected" message**:
- Check PIR sensor wiring (VCC, GND, OUT)
- Verify PIR sensor is powered (5V)
- Adjust PIR sensor sensitivity (turn potentiometer)
- Test PIR directly: Read GPIO 13 with multimeter

**Message appears but no HTTP POST**:
- Check WiFi connection
- Verify old phone IP is correct in code
- Test /cam/motion endpoint manually:
  ```bash
  curl -X POST http://192.168.43.1:8080/cam/motion -d "Motion detected"
  ```

**Too many false positives**:
- Adjust PIR sensor delay time (potentiometer)
- Reduce sensitivity
- Shield PIR from heat sources, direct sunlight
- Increase `motion_detect_interval` in code

---

### Image Upload to Server Fails

**Symptom**: Image captured but not uploaded to Digital Ocean

**Step 1: Check Internet Connection**
```bash
# From old phone via ADB
adb shell ping 8.8.8.8
# Should get replies
```

**Step 2: Test Server**
```bash
# From old phone
adb shell
curl http://your_server.com:8080/upload -X POST -F "file=@/sdcard/test.jpg"
# Should return JSON with file ID
```

**Step 3: Check ESP32 Serial**
```
Look for:
"Sending data to server: ..."
"Error connecting to server"
```

**Solutions**:
- Verify server URL in ESP32 code
- Check firewall allows port 8080
- Verify server is running (see Backend Server section)
- Check image is not too large (reduce quality/resolution)

---

## Android Application Issues

### App Crashes on Startup

**Check Logs**:
```bash
adb logcat | grep -i "home"
```

**Common Causes**:

**Missing Permissions**:
- Grant all required permissions in Android settings
- Phone, SMS, Location, Storage, Network

**ObjectBox Database Corruption**:
```bash
# Clear app data
adb shell pm clear com.kshem.homeclient
# Reinstall app
```

**MQTT Connection Failure**:
- App may crash if MQTT broker unreachable on startup
- Configure valid MQTT broker before starting service

---

### App Not Receiving Status Updates

**Symptom**: Status never updates, shows "No connection"

**Step 1: Check MQTT Connection**
- Open app settings
- Verify MQTT broker URL, port, credentials
- Check "MQTT Status" shows "Connected"

**Step 2: Test MQTT Broker**
```bash
# Install mosquitto-clients
sudo apt install mosquitto-clients

# Subscribe to topics
mosquitto_sub -h broker.hivemq.com -p 1883 -u username -P password -t "Home/+/data"

# Should see messages every 10 seconds
```

**Step 3: Check Old Phone**
- Verify old phone app is running (service started)
- Check old phone has internet connection
- Review old phone app logs

**Step 4: Verify Topic Names**
- Ensure both apps use same client ID format
- Check topic structure: `Home/<clientId>/data`

---

### SMS Commands Not Working

**Symptom**: Send SMS to old phone, nothing happens

**Step 1: Check SMS Permissions**
- Old phone app must have SMS permission granted
- Android Settings → Apps → Home Client → Permissions → SMS → Allow

**Step 2: Check SmsReceiver**
```bash
# View logs when SMS received
adb logcat | grep -i "sms"
```

**Step 3: Verify Phone Number**
- Some implementations validate sender phone number
- Check if your number is whitelisted in code

**Step 4: Check SMS Format**
```
Correct: d0
Incorrect: unlock door
Incorrect: D0 (case-sensitive)
```

**Step 5: Test Manually**
- Open old phone app
- Manually trigger command (if UI available)
- Verify Arduino responds

---

### High Battery Drain on Old Phone

**Symptom**: Old phone battery drains quickly

**Causes & Solutions**:

**WiFi Hotspot**:
- Hotspot uses significant power
- Keep phone plugged in (powered)
- Consider lower-power WiFi router instead

**App Wake Locks**:
- App may hold wake lock continuously
- Check battery stats: Settings → Battery
- Optimize: Allow app to use "Background restriction: Unrestricted"

**MQTT Reconnections**:
- Frequent disconnects/reconnects drain battery
- Use stable internet connection
- Increase MQTT keepalive interval

**Reduce Polling**:
- Increase `data_send_interval` from 10s to 30s
- Reduce status update frequency

---

## Network Connectivity

### Devices Can't Connect to Hotspot

**Symptom**: Arduino/ESP32 can't find or connect to hotspot

**Step 1: Verify Hotspot Is On**
- Old phone: Settings → WiFi Hotspot
- Should show "Active" with device count

**Step 2: Check Hotspot Settings**
```
SSID: S_HOME (must match code)
Password: 3030031343dc (must match code)
Security: WPA2 PSK
Band: 2.4GHz (ESP modules don't support 5GHz)
```

**Step 3: Check Device Compatibility**
- ESP-01 and ESP32 only support 2.4GHz
- If hotspot is 5GHz only, change to 2.4GHz or dual-band

**Step 4: Reduce Distance**
- Move devices closer to phone
- ESP modules have limited WiFi range (10-30m indoors)

**Step 5: Check for Interference**
- Too many WiFi networks nearby
- Microwave ovens, Bluetooth can interfere
- Try changing hotspot channel

---

### Intermittent WiFi Disconnections

**Symptom**: Devices connect then disconnect randomly

**Solutions**:

**Power Saving**:
- Old phone may turn off hotspot to save power
- Settings → WiFi → Advanced → Keep WiFi on during sleep: Always
- Keep phone plugged in

**Weak Signal**:
- Add external antenna to ESP-01 (solder to antenna pad)
- Use ESP32 with external antenna connector

**Too Many Devices**:
- Android hotspot typically supports 5-10 devices
- Reduce connected devices if at limit

**IP Address Conflicts**:
- Reboot all devices to get fresh DHCP leases
- Assign static IPs to Arduino/ESP modules

---

### Can't Access ESP32-CAM Web Interface

**Symptom**: Browser shows "Connection refused" or timeout

**Step 1: Verify ESP32 IP**
```bash
# From ESP32 serial monitor
Camera Ready! Use 'http://192.168.43.X' to connect
# Note this IP address
```

**Step 2: Ping Test**
```bash
# From PC on same network
ping 192.168.43.X
# Should get replies
```

**Step 3: Check Web Server**
- ESP32 HTTP server runs on port 80
- Try: `http://192.168.43.X` (not HTTPS)
- Try different browser

**Step 4: Power Cycle ESP32**
- May be in crashed state
- Reset button or power cycle

---

## MQTT Issues

### MQTT Connection Failed

**Symptom**: App shows "MQTT Disconnected"

**Step 1: Verify Broker Details**
```
Broker URL: broker.hivemq.com (or your broker)
Port: 1883 (unencrypted) or 8883 (TLS)
Username: (if required)
Password: (if required)
```

**Step 2: Test Broker Accessibility**
```bash
# From PC
mosquitto_pub -h broker.hivemq.com -p 1883 -t "test" -m "hello"
# Should succeed without error
```

**Step 3: Check Firewall**
- Corporate/school networks may block MQTT ports
- Try different network
- Use VPN if blocked

**Step 4: Verify Credentials**
- Free tier brokers may require sign-up
- Check username/password are correct
- Some brokers require ACL (access control list) configuration

---

### Messages Not Being Received

**Symptom**: Publishing works but subscribing doesn't receive messages

**Step 1: Check Topic Names**
```
Published topic: Home/controller_001/commands
Subscribed topic: Home/controller_001/commands
(Must match exactly, case-sensitive)
```

**Step 2: Test with MQTT Explorer**
1. Download MQTT Explorer
2. Connect to broker
3. Subscribe to `Home/#` (all topics)
4. Publish test message
5. Verify it appears

**Step 3: Check QoS**
- QoS 0: Fire and forget (may be lost)
- QoS 1: At least once (reliable)
- QoS 2: Exactly once (most reliable)
- Increase QoS if messages are lost

**Step 4: Check Retained Messages**
```bash
# Publish with retain flag
mosquitto_pub -h broker.hivemq.com -t "Home/test/config" -m "test" -r

# Subscribe to see retained message
mosquitto_sub -h broker.hivemq.com -t "Home/test/config"
# Should immediately receive "test"
```

---

### Duplicate Messages

**Symptom**: Same command executed multiple times

**Cause**: MQTT QoS 1 "at least once delivery"

**Solutions**:
- Implement idempotent commands (safe to execute multiple times)
- Add message deduplication based on timestamp
- Use unique message IDs
- QoS 0 for non-critical data (status updates)
- QoS 1 only for commands

---

## Backend Server Problems

### Server Not Responding

**Symptom**: Cannot reach Digital Ocean server

**Step 1: Check Server Is Running**
```bash
# SSH to server
ssh root@your_server_ip

# Check service status
systemctl status home-security

# If not running:
systemctl start home-security
```

**Step 2: Check Logs**
```bash
journalctl -u home-security -f
# Look for errors
```

**Step 3: Test Locally**
```bash
# On server
curl http://localhost:8080/files
# Should return JSON
```

**Step 4: Check Firewall**
```bash
ufw status
# Ensure port 8080 is allowed
ufw allow 8080/tcp
```

---

### Images Not Uploading

**Symptom**: POST /upload returns error

**Check Disk Space**:
```bash
df -h
# Ensure /var/lib/home-security/images has space
```

**Check Permissions**:
```bash
ls -la /var/lib/home-security/
# images directory should be writable
chmod 755 /var/lib/home-security/images
```

**Check PostgreSQL**:
```bash
systemctl status postgresql
# Should be running

# Test database connection
sudo -u postgres psql -d homesecurity -c "SELECT 1;"
```

**Check Logs for Errors**:
```bash
journalctl -u home-security -n 100
# Look for exceptions
```

---

### S3 Upload Failures

**Symptom**: Using S3 storage, uploads fail

**Verify AWS Credentials**:
```bash
# In application.properties
storage.s3.accessKey=YOUR_ACCESS_KEY
storage.s3.secretKey=YOUR_SECRET_KEY
storage.s3.region=us-east-1
storage.s3.bucket=your-bucket-name
```

**Test AWS CLI**:
```bash
apt install awscli
aws configure
aws s3 ls s3://your-bucket-name
# Should list contents
```

**Check IAM Permissions**:
- S3 bucket must allow PutObject, GetObject
- IAM user must have policy allowing s3:PutObject

**Check Bucket Region**:
- Must match region in config
- Verify in AWS console

---

## Mechanical Issues

### Linear Actuator Not Moving

**Symptom**: Command sent but actuator doesn't extend/retract

**Step 1: Check Power**
```bash
# Measure with multimeter
1. L298N 12V input: Should be 12V
2. L298N outputs (OUT1, OUT2) when active:
   - Forward: OUT1=12V, OUT2=0V
   - Reverse: OUT1=0V, OUT2=12V
```

**Step 2: Test Actuator Directly**
```bash
# Disconnect from L298N
# Apply 12V directly to actuator wires
# Should move
# Reverse polarity, should move opposite direction
```

**Step 3: Test L298N**
```bash
# Check enable pin (may need to be HIGH)
# Check IN1/IN2 signals from Arduino
# If L298N is hot, it may be damaged
```

**Solutions**:
- Replace L298N if burned out
- Check actuator isn't mechanically jammed
- Verify actuator current rating < L298N rating (2A)
- Add flyback diode across actuator terminals

---

### Actuator Moves But Doesn't Stop

**Symptom**: Actuator over-extends or doesn't stop at limit

**Step 1: Check Limit Switch**
```bash
# Measure voltage at Arduino D6 (OPENED_PIN)
# When actuator fully open: Should be HIGH (5V)
# When actuator not at limit: LOW (0V)
```

**Step 2: Test Limit Switch**
- Manually trigger switch
- Check LED on Arduino blinks (if code has visual feedback)
- Use multimeter continuity test

**Step 3: Check Code Logic**
```cpp
// In open() function
while(!digitalRead(OPENED_PIN)){
    delay(200);
}
// Ensure this loop waits for limit switch
```

**Solutions**:
- Adjust limit switch position
- Replace switch if faulty
- Add timeout to prevent infinite loop:
  ```cpp
  unsigned long start = millis();
  while(!digitalRead(OPENED_PIN) && millis() - start < 10000){
      delay(200);
  }
  ```

---

### Door Sensor Not Detecting

**Symptom**: Door status always shows closed/open regardless of actual state

**Test Reed Switch**:
```bash
# Measure with multimeter (resistance mode)
# Bring magnet close: Should show 0Ω (closed)
# Move magnet away: Should show infinite (open)
```

**Check Installation**:
- Reed switch and magnet must be aligned
- Maximum gap: Usually 5-20mm
- Switch may be installed backwards (swap magnet and switch)

**Solutions**:
- Adjust alignment
- Use stronger magnet
- Replace reed switch if faulty
- Check wiring: One terminal to GPIO, other to GND

---

## Performance Problems

### Slow Response Time

**Symptom**: Commands take >10 seconds to execute

**Diagnosis**:
- Normal unlock time: 5-8 seconds
- Normal lock time: 8 seconds
- If much longer, investigate:

**Check WiFi Signal**:
```bash
# Arduino serial
# Look for "Failed to connect" retries
# Weak signal causes delays
```

**Check Network Latency**:
```bash
# From personal phone
ping 192.168.43.1
# Should be <50ms
```

**Check MQTT Latency**:
- Use MQTT Explorer timestamp
- Publish message, note time to receive
- Should be <1 second

**Solutions**:
- Move phone closer to Arduinos
- Reduce `data_send_interval` (less network traffic)
- Use UDP for time-critical commands (already implemented)
- Check for network congestion

---

### High CPU Usage on Old Phone

**Symptom**: Phone gets hot, battery drains fast

**Check Running Services**:
```bash
adb shell top
# Look for com.kshem.homeclient process
# CPU usage should be <10%
```

**Common Causes**:

**MQTT Reconnect Loop**:
- App continuously tries to reconnect
- Fix MQTT broker connectivity first

**Logging**:
- Excessive logging to file/database
- Disable debug logs in release build

**Image Processing**:
- If app processes images locally
- Reduce image size/quality from camera

---

### Database Growing Too Large

**Symptom**: App slows down over time, storage full

**Check ObjectBox Size**:
```bash
adb shell du -sh /data/data/com.kshem.homeclient/files/objectbox
```

**Solutions**:
- Implement log rotation (delete old logs)
- Clear old captured images
- Purge SMS records older than X days
- Add database maintenance task:
  ```java
  // Run weekly
  box.removeAll(); // For non-critical logs
  ```

---

## Security Incidents

### Unauthorized Access Detected

**Symptom**: Door unlocked when you didn't command it

**Immediate Actions**:
1. Check MQTT SmsTopic for SMS commands (audit log)
2. Check old phone SMS inbox
3. Review app logs for commands
4. Change WiFi password immediately
5. Change MQTT broker password

**Investigation**:
1. Check all device logs for timestamps
2. Correlate with physical events (motion detection images)
3. Review who has access to personal phone

**Preventive Measures**:
- Enable SMS sender validation
- Implement rate limiting on commands
- Add command confirmation (require PIN)
- Enable audit logging
- Use TLS for MQTT

---

### Camera Hacked / Unauthorized Access

**Symptom**: Someone accessing camera feed

**Immediate Actions**:
1. Power off camera (relay or manual)
2. Change WiFi password
3. Check who is connected to hotspot:
   ```bash
   adb shell ip neigh show
   # Look for unknown devices
   ```

**Preventive Measures**:
- Add authentication to ESP32-CAM web server
- Disable web interface (comment out `startCameraServer()`)
- Use MAC address filtering on hotspot
- VPN access only
- Firewall rules limiting access to local network

---

## Maintenance Procedures

### Weekly Maintenance

**Visual Inspection**:
- Check all LED indicators are functioning
- Verify actuator moves smoothly
- Test alarm buzzer
- Check for loose wires

**Battery Check**:
- Old phone battery level (keep >20%)
- UPS battery charge level
- Replace UPS battery yearly

**Software**:
- Check for app crashes in logs
- Verify MQTT connection stable
- Test SMS command backup

---

### Monthly Maintenance

**Full System Test**:
1. Test all commands (lock, unlock, alarm, light)
2. Verify motion detection and image capture
3. Test SMS fallback
4. Check image server storage space

**Backup**:
- Export ObjectBox database
- Backup server database:
  ```bash
  pg_dump homesecurity > backup_$(date +%Y%m%d).sql
  ```
- Download all captured images

**Clean Sensors**:
- Dust PIR sensor lens
- Clean camera lens
- Check reed switch contacts

---

### Firmware Updates

**Before Updating**:
1. Document current configuration
2. Test new firmware on breadboard first
3. Have rollback plan (keep old firmware file)

**Update Procedure**:
1. Upload new firmware to test Arduino
2. Verify functionality
3. Upload to production Arduino
4. Test all features
5. Monitor for 24 hours

---

### Component Replacement

**Arduino Nano Replacement**:
```bash
1. Disconnect power
2. Photo-document all wiring
3. Carefully disconnect wires (label each)
4. Connect new Arduino with same pin configuration
5. Upload firmware
6. Test individually before reconnecting
7. Reconnect all wires
8. Power on and test
```

**ESP32-CAM Replacement**:
```bash
1. Note current IP address
2. Upload firmware to new ESP32
3. Swap hardware
4. Update IP in old phone app if changed
```

---

### Log Files

**Arduino Logs**:
- Serial output only (not saved)
- Use serial monitor for real-time debugging
- Consider adding SD card logger for long-term logs

**Android App Logs**:
```bash
# View live
adb logcat | grep -i "homeclient"

# Save to file
adb logcat > app_logs.txt
```

**Server Logs**:
```bash
# View live
journalctl -u home-security -f

# View last 100 lines
journalctl -u home-security -n 100

# View specific time range
journalctl -u home-security --since "2024-02-12 10:00" --until "2024-02-12 11:00"
```

---

## Emergency Procedures

### Complete System Failure

**If everything stops working**:
1. **Manual Override**: Use manual buttons on Lock Arduino
2. **Power Cycle**: Turn off main power, wait 60s, turn on
3. **Bypass System**: Manually operate door lock
4. **Contact**: Call for assistance if needed

### Lost Access to Old Phone

**If old phone is lost/stolen/broken**:
1. System will continue local operation (lock/unlock via manual buttons)
2. Camera will still detect motion (but not upload)
3. Get replacement phone
4. Install app
5. Configure hotspot with same SSID/password
6. System will auto-reconnect

### Fire/Emergency

**Priority**: Life safety first, property second
1. Evacuate immediately
2. Do NOT attempt to access system during emergency
3. Fire department can manually open door if needed
4. Power off main switch if safe to do so

---

## Getting Help

### Community Resources
- Arduino Forum: https://forum.arduino.cc/
- ESP32 Forum: https://esp32.com/
- Stack Overflow: Tag with `arduino`, `esp32`, `mqtt`

### Logs to Provide When Asking for Help
1. Arduino serial output (full boot sequence)
2. Android app logs (`adb logcat`)
3. Server logs (`journalctl -u home-security`)
4. Schematic or wiring photo
5. Code version/commit hash
6. Description of changes made recently

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Always prioritize safety over system operation!**
