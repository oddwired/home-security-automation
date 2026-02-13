# Arduino Lock Controller

## Overview

The **Lock Controller Arduino** is dedicated to controlling the linear actuator that physically locks and unlocks the door. It operates on-demand (powered by Main Arduino via relay) and uses a limit switch to detect when the lock is fully open.

**Board**: Arduino Nano (ATmega328P)  
**Power**: Via relay from Main Arduino (12V → 5V regulator)  
**Role**: Linear actuator control and door position monitoring

---

## Key Responsibilities

- 🔐 **Actuator Control**: Extend/retract linear actuator via L298N H-bridge
- 📍 **Position Detection**: Use limit switch to detect fully open position
- 🚪 **Door Sensor**: Monitor reed switch to detect door closed/open
- 🔄 **Inter-Arduino Communication**: Receive commands from Main Arduino
- ⏰ **Auto-Lock**: Automatically lock door 30 seconds after closing
- 🔘 **Manual Control**: Support manual open/close buttons

---

## Hardware Connections

### L298N H-Bridge Motor Driver
```
Arduino D2  → L298N IN1 (Forward)
Arduino D3  → L298N IN2 (Reverse)
Arduino 5V  → L298N 5V (Logic)
Arduino GND → L298N GND

12V Supply → L298N 12V (Motor power)
L298N OUT1  → Actuator Wire 1
L298N OUT2  → Actuator Wire 2
```

### Sensors & Buttons
```
D4 → Close Button (pull-up, other side to GND)
D5 → Open Button (pull-up, other side to GND)
D6 → Limit Switch (HIGH when fully open)
D7 → Door Reed Switch (LOW when door closed)
```

### Inter-Arduino Communication
```
D10 → Main Arduino D10 (Door Status: HIGH=open, LOW=closed)
D11 → Main Arduino D11 (Power Status: HIGH=powered)
D12 ← Main Arduino D12 (Control Signal: commands)
```

---

## Pin Assignments

| Pin | Mode | Connection | Purpose |
|-----|------|------------|---------|
| D2 | OUTPUT | L298N IN1 | Actuator forward (lock) |
| D3 | OUTPUT | L298N IN2 | Actuator reverse (unlock) |
| D4 | INPUT_PULLUP | Button | Manual close button |
| D5 | INPUT_PULLUP | Button | Manual open button |
| D6 | INPUT | Limit switch | Fully open detection |
| D7 | INPUT_PULLUP | Reed switch | Door closed detection |
| D10 | OUTPUT | Main Arduino | Door lock status |
| D11 | OUTPUT | Main Arduino | Power status indicator |
| D12 | INPUT | Main Arduino | Command signal |

---

## Actuator Control Logic

### Direction Control
```cpp
// Lock (extend actuator)
IN1 = HIGH, IN2 = LOW

// Unlock (retract actuator)
IN1 = LOW, IN2 = HIGH

// Stop
IN1 = LOW, IN2 = LOW
```

### Open Operation (Unlock)
1. Check if already open (limit switch HIGH)
2. If not, set IN1=LOW, IN2=HIGH
3. Poll limit switch every 200ms
4. When limit switch goes HIGH, stop motor
5. Update door status pin (D10 = HIGH)

**Timing**: 5-8 seconds (variable, until limit switch)

### Close Operation (Lock)
1. Set IN1=HIGH, IN2=LOW
2. Wait fixed 8 seconds (timeout-based)
3. Stop motor
4. Update door status pin (D10 = LOW)

**Timing**: Fixed 8 seconds

⚠️ **Note**: Close operation uses timeout instead of limit switch. Ensure actuator reaches full extension within 8 seconds.

---

## Auto-Lock Feature

### Logic
```cpp
if (actuator_open && door_closed && timer_not_started) {
    start_timer = millis();
}

if (door_opened) {
    reset_timer = 0;
}

if (timer >= CLOSE_DOOR_AFTER_SECS) {
    close();
}
```

**Default Timeout**: 30 seconds (configurable via `CLOSE_DOOR_AFTER_SECS`)

**Behavior**:
- When door is unlocked and then closed, starts 30-second timer
- If door reopened before timeout, resets timer
- After 30 seconds, automatically locks door
- Provides time to close door without immediately locking

---

## Inter-Arduino Communication Protocol

### Pin-Based Handshake

**Lock/Unlock Command Flow**:
1. Main Arduino sets D12 HIGH (signal command)
2. Lock Arduino detects HIGH on D12
3. Lock Arduino sets D11 LOW (acknowledge)
4. Main Arduino sees LOW on D11
5. Main Arduino sets D12 LOW (complete handshake)
6. Lock Arduino sets D11 HIGH (ready)
7. Lock Arduino executes action:
   - If currently CLOSED → open()
   - If currently OPEN → close()

**Advantages**:
- No data lines needed
- Reliable handshake
- Simple protocol
- No timing issues

---

## States

### Actuator State
```cpp
enum ActuatorState {
    OPEN,      // Unlocked, actuator retracted
    CLOSED     // Locked, actuator extended
};
```

### Door Status Pin Output
- `D10 = HIGH`: Door unlocked (actuator open)
- `D10 = LOW`: Door locked (actuator closed)

### Power Status Pin Output
- `D11 = HIGH`: Arduino powered and ready
- `D11 = LOW`: Arduino acknowledging command

---

## Configuration

### Timing Constants
```cpp
const int CLOSE_DOOR_AFTER_SECS = 30;    // Auto-lock delay
const int WAIT_AFTER_OPEN_SECS = 5;      // Unused (future)
```

### Pin Constants
```cpp
const int BAUD_RATE = 19200;             // Serial speed
const int ACTUATOR_IN1 = 2;              // Forward
const int ACTUATOR_IN2 = 3;              // Reverse
const int BTN_CLOSE = 4;                 // Manual close
const int BTN_OPEN = 5;                  // Manual open
const int OPENED_PIN = 6;                // Limit switch
const int DOOR_SENSOR_PIN = 7;           // Door sensor
const int DOOR_STATUS_PIN = 10;          // Status output
const int DC_STATUS_PIN = 11;            // Power status
const int DC_CONTROL_PIN = 12;           // Command input
```

---

## Setup Instructions

### 1. Install Arduino IDE
Download from [arduino.cc](https://www.arduino.cc/en/software)

### 2. Upload Firmware
1. Connect Lock Controller Arduino via USB
2. Select: Tools → Board → Arduino Nano
3. Select: Tools → Processor → ATmega328P (Old Bootloader)
4. Select: Tools → Port → (your COM port)
5. Click Upload (→)

### 3. Test Serial Output
1. Open Serial Monitor (19200 baud)
2. Press Reset
3. Should see: "OPEN Actuator" or "CLOSE Actuator"
4. Test manual buttons

### 4. Test Limit Switch
1. Monitor serial output
2. Manually trigger limit switch
3. Should see state change

### 5. Test Inter-Arduino Communication
1. Connect to Main Arduino
2. Send unlock command from Main
3. Verify actuator moves
4. Check status pin changes

---

## Operation

### Main Loop
```
loop() {
    runLinearActuator();    // Check buttons, sensors, timers
    readControlPins();      // Check for commands from Main Arduino
}
```

### Manual Operation
- Press **Open Button**: Unlocks door (if locked)
- Press **Close Button**: Locks door (if unlocked)

### Remote Operation
- Receive command via D12 pin from Main Arduino
- Execute open() or close() based on current state

### Automatic Operation
- Door closed + unlocked → Start 30-second timer
- After 30 seconds → Automatically lock
- Door opened → Reset timer

---

## Troubleshooting

### Actuator Not Moving
- Check L298N connections (IN1, IN2, power)
- Verify 12V supply to L298N
- Test actuator directly with 12V
- Check if L298N overheating (damaged)

### Actuator Doesn't Stop
- Check limit switch wiring
- Verify limit switch triggers (test manually)
- Adjust limit switch position
- Check code logic in open() function

### Auto-Lock Not Working
- Verify door sensor (D7) working
- Check timer logic in serial output
- Adjust CLOSE_DOOR_AFTER_SECS if needed
- Ensure door sensor shows closed state

### Inter-Arduino Communication Fails
- Check wire connections (D10, D11, D12)
- Verify common ground between Arduinos
- Test with multimeter (voltage changes)
- Check handshake sequence in serial

---

## Serial Output Examples

### Normal Boot
```
(System boots)
OPEN Actuator     (if limit switch is HIGH)
or
CLOSE Actuator    (if limit switch is LOW)
```

### Manual Button Press
```
Close button pressed
Actuator CLOSE
```

### Command from Main Arduino
```
Control pin HIGH detected
Sending ACK
Received command
Opening...
Actuator OPEN
```

### Auto-Lock Trigger
```
Door closed detected
Auto-lock timer started
Timer: 30 seconds elapsed
Auto-locking...
Actuator CLOSE
```

---

## Safety Features

### Limit Switch
- Prevents over-extension when opening
- Stops motor immediately when triggered
- Protects actuator from damage

### Timeout on Close
- Fixed 8-second timeout prevents infinite run
- Ensures motor stops even if no feedback

### Manual Override
- Physical buttons always work
- Independent of Main Arduino
- Emergency access if communication fails

### Power Status
- D11 pin indicates Arduino is powered
- Main Arduino monitors this for safety

---

## Performance Characteristics

- **Open Time**: 5-8 seconds (until limit switch)
- **Close Time**: 8 seconds (fixed timeout)
- **Auto-Lock Delay**: 30 seconds (configurable)
- **Power Consumption**: ~500mA (with actuator running)
- **Standby Power**: ~50mA
- **Boot Time**: <1 second

---

## Hardware Specifications

### Linear Actuator
- **Voltage**: 12V DC
- **Current**: 2A max (under load)
- **Stroke Length**: 50mm (typical)
- **Speed**: ~7mm/second
- **Force**: Varies by model

### L298N H-Bridge
- **Logic Voltage**: 5V
- **Motor Voltage**: 7-12V
- **Max Current**: 2A per channel
- **Protection**: Thermal shutdown

---

## Future Enhancements

### Potential Improvements
- Add second limit switch for closed position
- Implement soft-start/stop for smoother operation
- Add current sensing for jam detection
- Variable auto-lock timeout (configurable via Main Arduino)
- Position encoder for precise control
- Add timeout protection for all operations

---

## Related Documentation

- [Main Arduino Controller](../main-arduino-controller/README.md) - Coordinator
- [Technical Specification](../TECHNICAL-SPECIFICATION.md) - Architecture details
- [Code Documentation](../CODE-DOCUMENTATION.md) - Function reference
- [Setup Guide](../SETUP-GUIDE.md) - Hardware assembly
- [Troubleshooting Guide](../TROUBLESHOOTING.md) - Problem solving

---

**Serial Baud Rate**: 19200  
**Auto-Lock Delay**: 30 seconds  
**Close Timeout**: 8 seconds  
**Version**: 1.0  
**Status**: Production Ready
