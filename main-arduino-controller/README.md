# Main Arduino Controller

## Overview

The **Main Arduino Controller** is the central coordination hub of the home security system. It manages sensor inputs, controls relays, coordinates with the Lock Controller Arduino, and communicates with the Old Phone (central hub) via WiFi.

**Board**: Arduino Nano (ATmega328P)  
**Connectivity**: ESP-01 WiFi Module  
**Role**: System coordinator and sensor hub

---

## Key Responsibilities

- 🔌 **Sensor Monitoring**: Door status, padlock cover, motion (future)
- 🔄 **Coordination**: Control Lock Controller Arduino via pin protocol
- 📡 **Communication**: HTTP/UDP with Old Phone server
- 🚨 **Alarm Management**: Trigger buzzer when padlock tampered
- 💡 **Device Control**: Manage relays for lights, camera, Lock Arduino power
- ⏰ **Auto-Reset**: Periodic reboot (30 minutes) for stability

---

## Hardware Connections

### ESP-01 WiFi Module
```
Arduino D6 (TX) → ESP-01 RX
Arduino D7 (RX) → ESP-01 TX
3.3V → ESP-01 VCC & CH_PD
GND → ESP-01 GND
```

### Relay Module (4-Channel)
```
D2  → Relay IN1 (Lock Arduino Power)
D4  → Relay IN2 (Outdoor Light)
D8  → Relay IN3 (Buzzer)
D9  → Relay IN4 (Camera Power)
5V  → Relay VCC
GND → Relay GND
```

### Sensors
```
D3  → Padlock Reed Switch (pull-up)
D10 → Lock Arduino D10 (Door Status)
D11 → Lock Arduino D11 (DC Status)
```

### Outputs
```
D12 → Lock Arduino D12 (Control Signal)
```

---

## Pin Assignments

| Pin | Mode | Connection | Purpose |
|-----|------|------------|---------|
| D2 | OUTPUT | Relay IN1 | Lock Arduino power control |
| D3 | INPUT_PULLUP | Reed switch | Padlock cover sensor |
| D4 | OUTPUT | Relay IN2 | Outdoor light control |
| D6 | OUTPUT | ESP-01 RX | WiFi TX (SoftwareSerial) |
| D7 | INPUT | ESP-01 TX | WiFi RX (SoftwareSerial) |
| D8 | OUTPUT | Relay IN3 | Buzzer control (active LOW) |
| D9 | OUTPUT | Relay IN4 | Camera power control |
| D10 | INPUT | Lock Arduino | Door lock status |
| D11 | INPUT | Lock Arduino | Lock Arduino powered status |
| D12 | OUTPUT | Lock Arduino | Lock/unlock command |

---

## Supported Commands

| Command | Action | Response Time |
|---------|--------|---------------|
| `d0` | Unlock door | 5-8 seconds |
| `d1` | Lock door | 8 seconds |
| `dc0` | Power off Lock Arduino | Immediate |
| `dc1` | Power on Lock Arduino | 2 seconds |
| `al0` | Turn off buzzer | Immediate |
| `al1` | Turn on buzzer | Immediate |
| `al2` | Disable alarm system | Immediate |
| `al3` | Enable alarm system | Immediate |
| `dl0` | Turn off outdoor light | Immediate |
| `dl1` | Turn on outdoor light | Immediate |
| `rst` | Reset controller | 3-5 seconds |
| `crst` | Reset camera | 5-8 seconds |
| `c0` | Turn off camera | Immediate |
| `c1` | Turn on camera | 3-5 seconds |

---

## Configuration

### WiFi Settings
```cpp
const char WIFI_SSID[] = "S_HOME";           // Change to your hotspot SSID
const char WIFI_PASS[] = "your_password";    // Change to your password
```

### Server Settings
```cpp
const char SERVER[] = "192.168.43.1";        // Old phone IP
const int PORT = 8080;                       // nanoHTTPD port
const unsigned int udpPort = 8085;           // UDP listen port
```

### Timing Parameters (Configurable via Server)
- **Lock Timeout**: 30 seconds (auto-lock delay)
- **Data Send Interval**: 10 seconds (status updates)
- **Reset Interval**: 1800 seconds (30 minutes)
- **Alarm Timeout**: 2 seconds (buzzer duration)

---

## Setup Instructions

### 1. Install Arduino IDE
Download from [arduino.cc](https://www.arduino.cc/en/software)

### 2. Install Libraries
```
Sketch → Include Library → Manage Libraries
Search and install:
- WiFiEsp (by bportaluri)
```

### 3. Configure Code
Edit in `main_controller.ino`:
- WiFi SSID and password
- Server IP address

### 4. Upload Firmware
1. Connect Arduino Nano via USB
2. Select: Tools → Board → Arduino Nano
3. Select: Tools → Processor → ATmega328P (Old Bootloader)
4. Select: Tools → Port → (your COM port)
5. Click Upload (→)

### 5. Test Serial Output
1. Open Serial Monitor (Ctrl+Shift+M)
2. Set baud rate: **19200**
3. Press Reset button
4. Verify WiFi connection

---

## Communication Protocols

### HTTP (Status Updates)
**Endpoint**: `POST http://192.168.43.1:8080/data`  
**Frequency**: Every 10 seconds  
**Format**: `doorOpen,padlockOpen,dcState,alarmState,lightState,alarmArmed`  
**Response**: `commands:config`

### UDP (Fast Commands)
**Port**: 8085  
**Test**: `echo -n "d0" | nc -u 192.168.43.10 8085`

---

## Troubleshooting

### WiFi Not Connecting
- Check ESP-01 wiring (RX/TX correct)
- Verify 3.3V power (NOT 5V!)
- Ensure hotspot is active
- Add 10µF capacitor to ESP-01 power

### Commands Not Executing
- Check serial monitor for errors
- Verify UDP port 8085 listening
- Check server response format

### Door Not Locking/Unlocking
- Verify Lock Arduino powered (D11 HIGH)
- Check relay clicking
- Test inter-Arduino pins (D12)

---

## Related Documentation

- [API Reference](../API-REFERENCE.md) - Complete command reference
- [Technical Specification](../TECHNICAL-SPECIFICATION.md) - Detailed architecture
- [Troubleshooting Guide](../TROUBLESHOOTING.md) - Problem solving
- [Setup Guide](../SETUP-GUIDE.md) - Installation instructions
- [Code Documentation](../CODE-DOCUMENTATION.md) - Function reference

---

**Serial Baud Rate**: 19200  
**Default IP Range**: 192.168.43.x  
**UDP Port**: 8085  
**Version**: 1.0  
**Status**: Production Ready
