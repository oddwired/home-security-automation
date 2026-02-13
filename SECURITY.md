# Home Security System - Security Considerations

## Table of Contents
1. [Security Overview](#security-overview)
2. [Current Security Posture](#current-security-posture)
3. [Vulnerabilities & Risks](#vulnerabilities--risks)
4. [Network Security](#network-security)
5. [Authentication & Authorization](#authentication--authorization)
6. [Data Security](#data-security)
7. [Physical Security](#physical-security)
8. [Recommended Improvements](#recommended-improvements)
9. [Security Best Practices](#security-best-practices)
10. [Incident Response](#incident-response)

---

## Security Overview

This home security system was designed primarily for functional operation with basic security. This document analyzes the current security posture, identifies vulnerabilities, and provides recommendations for hardening the system against various threats.

### Threat Model

**Assets to Protect**:
- Physical access to home (door lock control)
- Privacy (camera images, activity logs)
- System availability (preventing DoS)
- Configuration data

**Threat Actors**:
1. **Casual Attacker**: Neighbor, visitor with casual access
2. **Determined Attacker**: Someone specifically targeting your system
3. **Remote Attacker**: Internet-based attacker
4. **Insider Threat**: Someone with legitimate access

**Attack Vectors**:
- WiFi network compromise
- MQTT message interception/injection
- Physical tampering
- Android app compromise
- Server breach

---

## Current Security Posture

### ✅ Existing Security Measures

1. **WPA2-PSK WiFi Encryption**
   - WiFi hotspot uses WPA2 encryption
   - Prevents casual eavesdropping

2. **Network Isolation**
   - Local hotspot network separate from main home WiFi
   - Limits attack surface

3. **SMS Fallback**
   - Allows control even if data connection compromised
   - Phone number provides some authentication

4. **Physical Isolation**
   - Components can be placed in secure enclosure
   - Reed switches and sensors distributed

5. **Auto-Reset Feature**
   - Arduino resets every 30 minutes
   - Clears any stuck states

### ❌ Security Weaknesses

1. **No MQTT Encryption**
   - Messages transmitted in plain text
   - Anyone on network can read/inject messages

2. **No Authentication**
   - No password/token on HTTP endpoints
   - No authentication on MQTT
   - No ESP32-CAM web interface authentication

3. **Hardcoded Credentials**
   - WiFi password in Arduino code
   - Server IPs hardcoded

4. **No Message Integrity**
   - No signatures on MQTT messages
   - Commands can be spoofed

5. **No Rate Limiting**
   - Commands can be sent in rapid succession
   - Vulnerable to DoS attacks

6. **No Audit Logging**
   - Limited logging of who did what
   - Difficult to investigate incidents

7. **Unencrypted Image Storage**
   - Images stored without encryption
   - Vulnerable to server breach

---

## Vulnerabilities & Risks

### Critical Vulnerabilities (High Risk)

#### 1. MQTT Message Injection
**Vulnerability**: MQTT messages are unencrypted and unauthenticated

**Attack Scenario**:
```python
# Attacker on same WiFi or with MQTT credentials
import paho.mqtt.client as mqtt

client = mqtt.Client()
client.connect("broker.hivemq.com", 1883)
client.publish("Home/controller_001/commands", '{"commands":["d0"]}')
# Door unlocks!
```

**Risk**: Unauthorized door unlock, alarm disable, system manipulation

**Likelihood**: Medium (requires network access or MQTT credentials)

**Impact**: Critical (physical access to home)

**Mitigation**: 
- Use MQTT with TLS (port 8883)
- Implement username/password authentication
- Add message signatures (HMAC)
- Whitelist allowed MQTT client IDs

---

#### 2. WiFi Hotspot Compromise
**Vulnerability**: If WiFi password is obtained, attacker has full network access

**Attack Scenario**:
1. Attacker obtains WiFi password (shoulder surfing, guessing, etc.)
2. Connects to hotspot
3. Scans network: `nmap 192.168.43.0/24`
4. Finds Arduino (port 8085 UDP), old phone (port 8080), ESP32 (port 80)
5. Sends UDP commands to Arduino
6. Accesses camera feed

**Risk**: Complete system compromise

**Likelihood**: Medium (if physical access to view hotspot settings)

**Impact**: Critical

**Mitigation**:
- Use strong, unique WiFi password (not the current one)
- MAC address filtering on hotspot
- Change password regularly
- Use WPA3 if supported
- Implement application-layer authentication

---

#### 3. Unprotected HTTP Endpoints
**Vulnerability**: No authentication on nanoHTTPD server or ESP32-CAM

**Attack Scenario**:
```bash
# Once on WiFi
curl http://192.168.43.1:8080/data -X POST -d "0,0,0,0,0,0"
# Can send fake sensor data

curl http://192.168.43.X/capture -o image.jpg
# Can view camera without authorization
```

**Risk**: Fake data injection, privacy violation

**Likelihood**: High (if on WiFi)

**Impact**: High

**Mitigation**:
- Add API key/token authentication
- Implement request signing
- Use HTTPS
- Rate limiting

---

### High Vulnerabilities (Medium-High Risk)

#### 4. ESP32-CAM Web Interface
**Vulnerability**: Open web interface on port 80

**Attack Scenario**:
- Attacker on WiFi browses to `http://192.168.43.X`
- Full camera control and live streaming
- Can change camera settings

**Risk**: Privacy violation, surveillance

**Likelihood**: High (if on WiFi)

**Impact**: High (privacy)

**Mitigation**:
- Disable web interface (comment out `startCameraServer()`)
- Add basic auth (username/password)
- Use VPN for remote access

---

#### 5. SMS Command Injection
**Vulnerability**: SMS commands have minimal validation

**Attack Scenario**:
- Attacker spoofs sender phone number (SS7 exploit)
- Sends SMS: "d0"
- Door unlocks

**Risk**: Unauthorized access via SMS

**Likelihood**: Low (requires SS7 access or SIM swap)

**Impact**: Critical

**Mitigation**:
- Implement SMS sender whitelist
- Add PIN requirement (e.g., "d0:1234")
- Use two-factor authentication
- Rate limit SMS commands (max 1 per minute)

---

#### 6. Physical Tampering
**Vulnerability**: Arduino, sensors, and actuators are accessible

**Attack Scenario**:
- Attacker cuts power
- Attacker disconnects door sensor
- Attacker shorts relay pins
- Attacker removes reed switch magnet

**Risk**: System bypass

**Likelihood**: Medium (requires physical access)

**Impact**: High

**Mitigation**:
- Tamper-evident seals
- Secure enclosures with locks
- Tamper detection sensors
- Battery backup (already implemented)
- Alert on sensor disconnection

---

### Medium Vulnerabilities (Low-Medium Risk)

#### 7. Denial of Service
**Vulnerability**: No rate limiting on commands

**Attack Scenario**:
```python
# Flood commands
while True:
    mqtt_client.publish("commands", '{"commands":["d0","d1"]}')
```

**Risk**: System becomes unresponsive, battery drain, component wear

**Likelihood**: Low (requires network access)

**Impact**: Medium (availability)

**Mitigation**:
- Rate limiting (max 10 commands/minute)
- Command queue with deduplication
- Auto-block flooding clients

---

#### 8. Image Data Exposure
**Vulnerability**: Images stored unencrypted on server

**Attack Scenario**:
- Server is compromised
- Attacker downloads all images
- Privacy violation

**Risk**: Privacy breach

**Likelihood**: Low (requires server access)

**Impact**: Medium-High

**Mitigation**:
- Encrypt images at rest (AES-256)
- Use encrypted S3 bucket
- Implement access control lists
- Auto-delete old images (retention policy)

---

#### 9. Hardcoded Secrets
**Vulnerability**: Credentials in source code

**Attack Scenario**:
- Code is posted to GitHub
- WiFi password exposed: `3030031343dc`
- Attacker connects to network

**Risk**: Credential exposure

**Likelihood**: Medium (if code is shared)

**Impact**: Medium-High

**Mitigation**:
- Use configuration files (not in git)
- Environment variables
- Secure credential storage (Android KeyStore)
- Rotate secrets regularly

---

## Network Security

### Current Configuration
```
[Internet]
    │
    ├─ MQTT Broker (1883, unencrypted)
    └─ Digital Ocean (8080, HTTP)
    
[Old Phone Hotspot]
192.168.43.1
WPA2-PSK: "3030031343dc"
    │
    ├─ Main Arduino (192.168.43.x:8085 UDP, no auth)
    ├─ ESP32-CAM (192.168.43.y:80, no auth)
    └─ Devices (any can connect)
```

### Recommended Configuration
```
[Internet]
    │
    ├─ MQTT Broker (8883, TLS + auth)
    └─ Digital Ocean (443, HTTPS + auth)
    
[Old Phone Hotspot]
192.168.43.1
WPA3 or WPA2-Enterprise
    │
    ├─ Main Arduino (API token required)
    ├─ ESP32-CAM (Basic auth, HTTPS)
    └─ MAC Address Filtering
```

### WiFi Security Improvements

#### Upgrade to WPA3
If phone and ESP modules support:
```
Security: WPA3-Personal
Password: Strong 16+ character passphrase
```

#### MAC Address Filtering
```bash
# On Android hotspot settings
Allowed MAC Addresses:
  - XX:XX:XX:XX:XX:XX (Main Arduino ESP-01)
  - YY:YY:YY:YY:YY:YY (ESP32-CAM)
  - ZZ:ZZ:ZZ:ZZ:ZZ:ZZ (Personal Phone)
```

#### Hide SSID
```
SSID Broadcast: Disabled
Hidden Network: Enabled
```

Note: Security through obscurity, not true security

---

### MQTT Security

#### Enable TLS
```cpp
// Arduino code
WiFiSSLClient sslClient;
MqttClient mqttClient(sslClient);
mqttClient.connect("broker.hivemq.com", 8883);
```

**Certificate Validation**: Ensure client validates server certificate

#### Username/Password Authentication
```cpp
mqttClient.setUsernamePassword("username", "strong_password");
```

#### Client Certificate Authentication
For strongest security:
1. Generate client certificate
2. Install on Arduino/ESP32
3. Configure broker to require client certs
4. Only authorized clients can connect

---

### HTTP Security

#### HTTPS for nanoHTTPD
Current limitation: nanoHTTPD on Android doesn't easily support HTTPS

**Workaround**:
- Use VPN for remote access
- Or implement reverse proxy with HTTPS

#### API Key Authentication
```java
// nanoHTTPD server
public Response serve(IHTTPSession session) {
    String apiKey = session.getHeaders().get("x-api-key");
    if (!"your_secret_key_here".equals(apiKey)) {
        return newFixedLengthResponse(Response.Status.UNAUTHORIZED, 
            "text/plain", "Unauthorized");
    }
    // Process request
}
```

```cpp
// Arduino code
client.println("POST /data HTTP/1.1");
client.println("X-API-Key: your_secret_key_here");
```

---

## Authentication & Authorization

### Current State
- **No authentication** on any component
- **No authorization** (if you can connect, you can control)

### Recommended Implementation

#### 1. API Token Authentication
```
Request Header: Authorization: Bearer <token>
Token: SHA-256 hash of secret + timestamp
```

**Advantages**:
- Lightweight
- Easy to implement on Arduino
- Can be revoked

**Implementation**:
```cpp
// Arduino
String generateToken(String secret, unsigned long timestamp) {
    String data = secret + String(timestamp);
    return sha256(data);
}

// Include in HTTP header
client.println("Authorization: Bearer " + generateToken("secret", millis()));
```

#### 2. HMAC Message Signing
```
Message: {"commands":["d0"]}
Signature: HMAC-SHA256(message, secret_key)
```

**Advantages**:
- Ensures message integrity
- Prevents tampering
- Replay protection with nonce

**Implementation**:
```cpp
#include <mbedtls/md.h>

String hmacSHA256(String message, String key) {
    byte hmacResult[32];
    mbedtls_md_context_t ctx;
    mbedtls_md_init(&ctx);
    mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(MBEDTLS_MD_SHA256), 1);
    mbedtls_md_hmac_starts(&ctx, (const unsigned char*)key.c_str(), key.length());
    mbedtls_md_hmac_update(&ctx, (const unsigned char*)message.c_str(), message.length());
    mbedtls_md_hmac_finish(&ctx, hmacResult);
    mbedtls_md_free(&ctx);
    
    return bytesToHex(hmacResult, 32);
}
```

#### 3. ESP32-CAM Basic Authentication
```cpp
// ESP32 HTTP server
static esp_err_t auth_handler(httpd_req_t *req) {
    char *buf;
    size_t buf_len;
    
    buf_len = httpd_req_get_hdr_value_len(req, "Authorization") + 1;
    if (buf_len > 1) {
        buf = malloc(buf_len);
        httpd_req_get_hdr_value_str(req, "Authorization", buf, buf_len);
        
        // Check: "Basic <base64(username:password)>"
        if (strcmp(buf, "Basic YWRtaW46cGFzc3dvcmQ=") == 0) {
            free(buf);
            return ESP_OK;
        }
        free(buf);
    }
    
    httpd_resp_set_status(req, "401 Unauthorized");
    httpd_resp_set_hdr(req, "WWW-Authenticate", "Basic realm=\"ESP32-CAM\"");
    httpd_resp_send(req, NULL, 0);
    return ESP_FAIL;
}
```

---

## Data Security

### Data in Transit

#### Current
- HTTP: Unencrypted
- MQTT: Unencrypted
- UDP: Unencrypted

**Risk**: Eavesdropping, man-in-the-middle

#### Recommended
- HTTPS everywhere
- MQTT over TLS
- Encrypted UDP (custom protocol)

### Data at Rest

#### Images on Server
**Current**: Unencrypted files on disk

**Recommended**:
```java
// Encrypt before storing
public void storeImage(byte[] imageData, String fileId) {
    SecretKey key = getEncryptionKey();
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    
    byte[] encrypted = cipher.doFinal(imageData);
    Files.write(Paths.get(storagePath, fileId), encrypted);
}
```

#### Android Database
**Current**: ObjectBox database unencrypted

**Recommended**:
- Use Android Keystore for keys
- Encrypt database with SQLCipher or ObjectBox encryption

#### Logs
**Current**: Plain text logs may contain sensitive info

**Recommended**:
- Redact sensitive data (passwords, tokens)
- Encrypt log files
- Implement log rotation and deletion

---

## Physical Security

### Enclosure Recommendations

**Main Controller Enclosure**:
- Lockable box (metal or heavy plastic)
- Ventilation holes (but too small for probing)
- Tamper-evident seals
- Mount in hidden location

**Lock Controller**:
- Must be near door mechanism (higher risk)
- Tamper-resistant screws
- Alert on power disconnect

**Sensors**:
- Reed switches can be defeated with external magnet
- Consider accelerometer for tamper detection
- Redundant sensors

### Tamper Detection

**Implement Tamper Alerts**:
```cpp
// Check for suspicious patterns
if (sensorDisconnected || powerGlitch || rapidCommands) {
    sendTamperAlert();
    lockSystem();  // Failsafe mode
}
```

**Power Tamper**:
- Alert if main power is cut (run on battery)
- Alert if battery voltage drops rapidly

**Enclosure Tamper**:
- Add magnetic/mechanical switch on enclosure
- Trigger alarm if opened

---

## Recommended Improvements

### Priority 1: Critical (Implement Immediately)

#### 1.1 Strong WiFi Password
```
Current: 3030031343dc (weak)
Recommended: Use 16+ character random passphrase
Example: correct-horse-battery-staple-7821
```

#### 1.2 MQTT Authentication
```
Broker: Configure with username/password
Clients: Update to use credentials
Consider: Client certificates for strongest auth
```

#### 1.3 API Key for HTTP Endpoints
```java
// Generate strong API key
String apiKey = UUID.randomUUID().toString();
// Store securely, validate on each request
```

### Priority 2: High (Implement Soon)

#### 2.1 MQTT over TLS
- Port 8883 instead of 1883
- Validates broker certificate
- Encrypts all messages

#### 2.2 Rate Limiting
```cpp
// Arduino/Server
unsigned long lastCommandTime = 0;
const int COMMAND_DELAY_MS = 5000;  // 5 seconds between commands

if (millis() - lastCommandTime < COMMAND_DELAY_MS) {
    return;  // Ignore command
}
```

#### 2.3 SMS PIN Requirement
```
Format: <command>:<PIN>
Example: d0:1234
```

### Priority 3: Medium (Implement When Possible)

#### 3.1 Image Encryption
- Encrypt images before upload
- Server stores encrypted blobs
- Decrypt only when viewing

#### 3.2 Audit Logging
```java
void logCommand(String command, String source, String result) {
    AuditLog log = new AuditLog();
    log.timestamp = System.currentTimeMillis();
    log.command = command;
    log.source = source;  // "MQTT", "SMS", "Manual"
    log.result = result;  // "Success", "Failed"
    box.put(log);
}
```

#### 3.3 Geofencing
- Only allow commands when personal phone is within certain distance
- Use GPS on old phone to verify

### Priority 4: Low (Nice to Have)

#### 4.1 Two-Factor Authentication
- Require confirmation for critical commands
- Push notification with "Confirm unlock? Yes/No"

#### 4.2 Video Retention Policy
- Auto-delete images older than 30 days
- Reduce storage and privacy risk

#### 4.3 Intrusion Detection
- Monitor for unusual patterns
- Alert on rapid commands, off-hours access

---

## Security Best Practices

### Operational Security

1. **Change Default Passwords**
   - WiFi password
   - MQTT credentials
   - Any default settings

2. **Regular Updates**
   - Arduino firmware
   - Android apps
   - Server software
   - Dependencies

3. **Backup & Recovery**
   - Regular backups of configuration
   - Test recovery procedures
   - Document emergency access

4. **Access Control**
   - Limit who has credentials
   - Revoke access when no longer needed
   - Change passwords after personnel changes

5. **Monitoring**
   - Review logs weekly
   - Check for anomalies
   - Monitor failed access attempts

### Development Security

1. **Secure Coding**
   - Validate all inputs
   - Avoid hardcoded secrets
   - Use parameterized queries (SQL injection prevention)
   - Handle errors securely (don't leak info)

2. **Code Review**
   - Review security-critical code
   - Check for common vulnerabilities
   - Use static analysis tools

3. **Dependency Management**
   - Keep libraries updated
   - Monitor for known vulnerabilities
   - Use trusted sources only

4. **Testing**
   - Test authentication/authorization
   - Test with invalid inputs
   - Penetration testing

---

## Incident Response

### Suspected Compromise

**Indicators**:
- Door unlocks unexpectedly
- Unusual commands in logs
- Unknown devices on WiFi
- MQTT messages from unknown source
- Camera accessed at odd hours

**Immediate Actions**:
1. **Isolate**: Disable WiFi hotspot, disconnect from internet
2. **Change Credentials**: All passwords, tokens, keys
3. **Review Logs**: Determine scope and timeline
4. **Notify**: Anyone who should know
5. **Assess Damage**: What was accessed? Changed?

### Response Procedure

**Step 1: Containment (First 15 minutes)**
- Disable system or put in safe mode
- Block attacker if identified
- Document everything

**Step 2: Investigation (1-4 hours)**
- Collect logs from all components
- Identify attack vector
- Determine what data was accessed
- Check for backdoors or persistence

**Step 3: Eradication (4-24 hours)**
- Remove attacker access
- Fix vulnerability exploited
- Change all credentials
- Update firmware if compromised

**Step 4: Recovery (1-3 days)**
- Restore system to known good state
- Re-upload clean firmware
- Reinstall apps if needed
- Test thoroughly before going live

**Step 5: Post-Incident (1 week)**
- Document incident fully
- Implement preventive measures
- Update security procedures
- Review and improve monitoring

### Evidence Collection

**Preserve**:
- Arduino serial logs (if captured)
- Android app logs: `adb logcat > incident_log.txt`
- Server logs: `journalctl -u home-security > server_log.txt`
- MQTT message logs (if broker provides)
- Network traffic capture (if available)
- Physical evidence (tampering)

**Analyze**:
- Timeline of events
- Source of attack
- Commands executed
- Data accessed

---

## Security Checklist

### Deployment Checklist

- [ ] Strong WiFi password (16+ characters)
- [ ] MQTT authentication enabled
- [ ] MQTT over TLS (port 8883)
- [ ] API key for HTTP endpoints
- [ ] ESP32-CAM web interface disabled or protected
- [ ] SMS sender whitelist
- [ ] Rate limiting implemented
- [ ] Audit logging enabled
- [ ] Enclosures locked
- [ ] Tamper seals applied
- [ ] Default passwords changed
- [ ] Regular backups scheduled

### Periodic Review (Monthly)

- [ ] Review access logs for anomalies
- [ ] Check for firmware updates
- [ ] Test SMS fallback
- [ ] Verify MQTT connection secure
- [ ] Check storage space (logs, images)
- [ ] Test emergency procedures
- [ ] Review and rotate credentials (quarterly)

---

## Conclusion

The current system prioritizes functionality over security. While adequate for low-threat environments, significant improvements are needed for high-security applications.

**Minimum Security**:
- Strong WiFi password
- MQTT authentication
- API keys

**Recommended Security**:
- All minimum items +
- TLS encryption
- Rate limiting
- Audit logging

**High Security**:
- All recommended items +
- Image encryption
- Tamper detection
- Two-factor authentication
- Regular security audits

**Remember**: Security is a process, not a product. Continuous monitoring and improvement are essential.

---

**Document Version**: 1.0  
**Last Updated**: February 2026  
**Classification**: Internal Use Only  
**Review Security Posture Quarterly**
