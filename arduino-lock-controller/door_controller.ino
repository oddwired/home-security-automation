#include <SoftwareSerial.h>
#define DEBUG true

const int BAUD_RATE = 19200;

const int ACTUATOR_IN1 = 2;
const int ACTUATOR_IN2 = 3;
const int BTN_CLOSE = 4;
const int BTN_OPEN= 5;
const int OPENED_PIN = 6;
const int DOOR_SENSOR_PIN = 7;
const int DOOR_STATUS_PIN = 10; // LOW when the door is closed
const int DC_STATUS_PIN = 11;
const int DC_CONTROL_PIN = 12;

const String SERIAL_CMD_PREFIX = "cmd:";
const String CMD_TURN_OFF_DC = "dc0"; // dc - door controller , '0' off
const String CMD_TURN_ON_DC = "dc1"; // '1' on
const String CMD_OPEN_DOOR = "d0";
const String CMD_CLOSE_DOOR = "d1";
const String CMD_GET_DC_STATE = "dcs";
const String CMD_ACK = "OK";

const int DC_STATE_OPEN = 1;
const int DC_STATE_CLOSE = 2;

const int CLOSE_DOOR_AFTER_SECS = 30; // Wait time before activating actuator to lock
const int WAIT_AFTER_OPEN_SECS = 5; // Wait before starting close timer
unsigned long door_closed_millis = 0; // time in millis when the door was closed. '0' if timer has not been started

enum ActuatorState{OPEN, CLOSED};

ActuatorState actuatorState;

void setup() {
    initSerial();

    pinMode(ACTUATOR_IN1, OUTPUT);
    pinMode(ACTUATOR_IN2, OUTPUT);

    pinMode(BTN_CLOSE, INPUT);
    pinMode(BTN_OPEN, INPUT);

    pinMode(OPENED_PIN, INPUT);
    pinMode(DOOR_SENSOR_PIN, INPUT);

    pinMode(DOOR_STATUS_PIN, OUTPUT);
    pinMode(DC_STATUS_PIN, OUTPUT);
    pinMode(DC_CONTROL_PIN, INPUT);

    digitalWrite(DC_STATUS_PIN, HIGH);

    if(digitalRead(OPENED_PIN)){
        setState(OPEN);
    }else{
        setState(CLOSED);
    }
}

void loop() {
    runLinearActuator();
    readControlPins();
}

void readControlPins(){
    if(digitalRead(DC_CONTROL_PIN)){
        // The PIN now HIGH.
        // First acknowledge the status change
        digitalWrite(DC_STATUS_PIN, LOW);
        while(digitalRead(DC_CONTROL_PIN)){ // Wait for the control pin to go low

        }
        digitalWrite(DC_STATUS_PIN, HIGH); // Bring status PIN back up to complete the command
        // Check the right action to take
        if(actuatorState == CLOSED){
            open();
        }else{
            close();
        }
    }
}

/*******************************************************
 * *********** LINEAR ACTUATOR FUNCTIONS ***************
 * *******************************************************/

void setState(ActuatorState state){
    if(state == OPEN){
        actuatorState = OPEN;
        digitalWrite(DOOR_STATUS_PIN, HIGH);
    }else{
        actuatorState = CLOSED;
        digitalWrite(DOOR_STATUS_PIN, LOW);
    }
}

void runLinearActuator(){
    int btn1Value = digitalRead(BTN_CLOSE);
    int btn2Value = digitalRead(BTN_OPEN);

    int doorClosed = digitalRead(DOOR_SENSOR_PIN);

    /* Button to close is pressed and the actuator is open */
    if(btn1Value && actuatorState == OPEN){
        close();
    }

    /* Button to open is pressed the actuator is closed */
    if(btn2Value && !digitalRead(OPENED_PIN)){
        open();
        //delay(WAIT_AFTER_OPEN_SECS * 1000);
    }

    /* Actuator is open and the door has been closed */
    if(actuatorState == OPEN && doorClosed && door_closed_millis == 0){
        door_closed_millis = millis();
    }

    if(!doorClosed){
        door_closed_millis = 0;
    }

    if(actuatorState == OPEN // actuator is still open
       && doorClosed // the door has been closed
       && door_closed_millis != 0 // Timer has been started
       && millis() - door_closed_millis >= CLOSE_DOOR_AFTER_SECS * 1000){ // Timer has reached the wait time
        close();
    }

    turnOff();
}

void turnOff(){
    digitalWrite(ACTUATOR_IN1, LOW);
    digitalWrite(ACTUATOR_IN2, LOW);

    //if(DEBUG){
    //    Serial.println("TURNOFF Actuator");
    //}
}

void close(){
    if(DEBUG){
        Serial.println("CLOSE Actuator");
    }

    digitalWrite(ACTUATOR_IN1, HIGH);
    digitalWrite(ACTUATOR_IN2, LOW);

    delay(8000);

    setState(CLOSED);

    turnOff();

    door_closed_millis = 0; // Reset timer
}

void  open(){

    if(DEBUG){
        Serial.println("OPEN Actuator");
    }

    if(!digitalRead(OPENED_PIN)){
        digitalWrite(ACTUATOR_IN1, LOW);
        digitalWrite(ACTUATOR_IN2, HIGH);

        while(!digitalRead(OPENED_PIN)){
            delay(200);
        }
    }

    setState(OPEN);

    turnOff();
}

/******************************************************
************ SERIAL COMMS FUNCTIONS *******************
********************************************************/

void initSerial() {
    Serial.begin(BAUD_RATE);
}
