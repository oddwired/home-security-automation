#include <SoftwareSerial.h>
#include <WiFiEsp.h>
#include <WiFiEspUdp.h>

#define DEBUG true

const int BAUD_RATE = 19200;

// PIN Assignment
const int DC_POWER_PIN = 2;
const int PAD_DOOR_PIN = 3;
const int DOOR_LIGHT_CONTROL_PIN = 4;
const int MOTION_SENSOR_PIN = 5;
const int WIFI_RX_PIN = 7; // Connect to module TX pin
const int WIFI_TX_PIN = 6; // Connect to module RX pin
const int DOOR_STATUS_PIN = 10; // LOW when the door is closed
const int DC_STATUS_PIN = 11;
const int DC_CONTROL_PIN = 12;
const int ALARM_CONTROL_PIN = 8;
const int CAMERA_CONTROL_PIN = 9;

// Commands
const String SERIAL_CMD_PREFIX = "cmd:";
const String CMD_TURN_OFF_DC = "dc0"; // dc - door controller , '0' off
const String CMD_TURN_ON_DC = "dc1"; // '1' on
const String CMD_OPEN_DOOR = "d0";
const String CMD_CLOSE_DOOR = "d1";
const String CMD_GET_DC_STATE = "dcs";
const String CMD_ACK = "OK";
const String CMD_TURN_ON_ALARM = "al1";
const String CMD_TURN_OFF_ALARM = "al0";
const String CMD_DISABLE_ALARM = "al2";
const String CMD_ENABLE_ALARM = "al3";
const String CMD_TURN_ON_DOOR_LIGHT = "dl1";
const String CMD_TURN_OFF_DOOR_LIGHT = "dl0";
const String CMD_RESET = "rst";
const String CMD_RESET_CAMERA = "crst";
const String CMD_TURN_ON_CAMERA = "c1";
const String CMD_TURN_OFF_CAMERA = "c0";

// Door lock controller state
const int DC_STATE_OPEN = 1;
const int DC_STATE_CLOSE = 2;
const int DC_STATE_NO_POWER = -1;

// WIFI Setup
const char WIFI_SSID[] = "S_HOME";
const char WIFI_PASS[] = "3030031343dc";
int wifi_status = WL_IDLE_STATUS;

const char SERVER[] = "192.168.43.1";
const int PORT = 8080;
const unsigned int udpPort = 8085;

// Config properties
unsigned int lock_timeout = 30;
unsigned int data_send_interval = 10;
unsigned long reset_interval = 60 * 30; // Every 30 minutes

// Timers
unsigned long last_sent_millis = 0;
unsigned long alarm_turned_on_millis = 0;
int alarm_turn_off_timeout = 2;

// STATES
int dc_power_state = LOW;
int alarm_state = LOW;
int door_light_state = LOW;
int alarm_armed_state = HIGH;

// Reset function
void(* controllerReset) (void) = 0;

enum DoorStatus{DS_OPEN,DS_CLOSED,DS_UNKNOWN};

SoftwareSerial esp8266(WIFI_RX_PIN, WIFI_TX_PIN); // RX, TX
WiFiEspClient client;
WiFiEspUDP Udp;

DoorStatus doorStatus;

void setup() {
    initSerial();
    initWifi();
    connectWifi();
    initUdp();

    pinMode(DC_POWER_PIN, OUTPUT);
    pinMode(ALARM_CONTROL_PIN, OUTPUT);
    pinMode(DOOR_LIGHT_CONTROL_PIN, OUTPUT);
    pinMode(MOTION_SENSOR_PIN, INPUT);
    pinMode(PAD_DOOR_PIN, INPUT_PULLUP);
    pinMode(DOOR_STATUS_PIN, INPUT);
    pinMode(DC_STATUS_PIN, INPUT);
    pinMode(DC_CONTROL_PIN, OUTPUT);
    pinMode(CAMERA_CONTROL_PIN, OUTPUT);

    turnOnCamera();
    turnOffAlarm();
    //turnOnDoorControllerPower();
    turnOffDoorControllerPower();
    //getDoorStatus();
    // By default it is closed
    doorStatus = DS_CLOSED;
}

void loop() {

    // Listen for emergency commands
    udpListen();

    if(alarm_armed_state){
      checkDoorPad();
     }

    if ((millis() - last_sent_millis) >= (data_send_interval * 1000)) {
        sendData();

        last_sent_millis = millis();
    }

    // Reset before the next loop if reset interval has been reached
    if(millis() >= (reset_interval * 1000)){
        log("Resetting the controller");
        delay(2000);
        controllerReset();
    }

    if(getDCState() && getDoorStatus() == DS_CLOSED && dc_power_state == 0){
      turnOffDoorControllerPower();
    }else if(dc_power_state == 1){
      turnOnDoorControllerPower();
    }
}

void sendData() {
    int doorPadState = digitalRead(PAD_DOOR_PIN);
    int motionSensorState = digitalRead(MOTION_SENSOR_PIN);

    char delimiter = ',';

    String data = String("") + (doorStatus == DS_OPEN ? 1 : 0);
    data += delimiter;
    data += doorPadState;
    data += delimiter;
    data += getDCState();
    data += delimiter;
    data += alarm_state;
    data += delimiter;
    data += door_light_state;
    data += delimiter;
    data += alarm_armed_state;
    //data += delimiter;

    String response = sendToServer(data.c_str());
    log("Response: " + response);

    String commandList = getValueAtIndex(response, ':', 0);
    String configList = getValueAtIndex(response, ':', 1);

    executeCommands(commandList);
    updateConfig(configList);
}

void executeCommand(String command){
    log("Executing Command: "+ command);
    if(command.equals(CMD_TURN_OFF_DC)){
        dc_power_state = 0;
        turnOffDoorControllerPower();
    }else if(command.equals(CMD_TURN_ON_DC)){
      dc_power_state = 1;
        turnOnDoorControllerPower();
    }else if(command.equals(CMD_OPEN_DOOR)){
        unlockDoor();
    }else if(command.equals(CMD_CLOSE_DOOR)){
        lockDoor();
    }else if(command.equals(CMD_TURN_ON_ALARM)){
        turnOnAlarm();
    }else if(command.equals(CMD_TURN_OFF_ALARM)){
        turnOffAlarm();
    }else if(command.equals(CMD_TURN_ON_DOOR_LIGHT)){
        turnOnDoorLight();
    }else if(command.equals(CMD_TURN_OFF_DOOR_LIGHT)){
        turnOffDoorLight();
    }else if(command.equals(CMD_RESET)){
        controllerReset();
    }else if(command.equals(CMD_RESET_CAMERA)){
        resetCamera();
    }else if(command.equals(CMD_TURN_ON_CAMERA)){
        turnOnCamera();
    }else if(command.equals(CMD_TURN_OFF_CAMERA)){
        turnOffCamera();
    }else if(command.equals(CMD_DISABLE_ALARM)){
      turnOffAlarm();
      alarm_armed_state = LOW;
    }else if(command.equals(CMD_ENABLE_ALARM)){
      alarm_armed_state = HIGH;
    }
}

void executeCommands(String commandList){
    int index = 0;
    String valueAtIndex = getValueAtIndex(commandList, ',', index);
    while(!valueAtIndex.equals("")){
        executeCommand(valueAtIndex);
        ++index;

        valueAtIndex = getValueAtIndex(commandList, ',', index);
    }
}

void updateConfig(String configList){
    String lock = getValueAtIndex(configList, ',', 0);
    if(!lock.equals("")){
        log("Update lock_timeout: "+ lock);
        lock_timeout = lock.toInt();
    }

    String data_interval = getValueAtIndex(configList, ',', 1);
    if(!data_interval.equals("")){
        log("Update data_send_interval: "+ data_interval);
        data_send_interval = data_interval.toInt();
    }

    String resetInterval = getValueAtIndex(configList, ',', 2);
    if(!resetInterval.equals("")){
        log("Update reset_interval: "+ resetInterval);
        reset_interval = resetInterval.toInt();
    }

    String power_state = getValueAtIndex(configList, ',', 3);
    if(!power_state.equals("")){
        log("Power state: "+ power_state);
        dc_power_state = power_state.toInt();
    }

    String alarm_armed = getValueAtIndex(configList, ',', 4);
    if(!alarm_armed.equals("")){
        alarm_armed_state = alarm_armed.toInt();
    }

    String alarm_timeout = getValueAtIndex(configList, ',', 5);
    if(!alarm_timeout.equals("")){
        alarm_turn_off_timeout = alarm_timeout.toInt();
      }

}

void errorAlert(String message) {
    log("Error:  " + message);
}

/******************************************************
***********CAMERA CONTROL ****************************
*****************************************************/

void turnOnCamera(){
  digitalWrite(CAMERA_CONTROL_PIN, LOW);
}

void turnOffCamera(){
  digitalWrite(CAMERA_CONTROL_PIN, HIGH);
}

void resetCamera(){
  turnOffCamera();
  delay(3000);
  turnOnCamera();
}

/*****************************************************
 * ******** DOOR LIGHT FUNCTIONS *********************
 * ***************************************************/

void turnOnDoorLight(){
    digitalWrite(DOOR_LIGHT_CONTROL_PIN, HIGH);
    door_light_state = HIGH;
}

void turnOffDoorLight(){
    digitalWrite(DOOR_LIGHT_CONTROL_PIN, LOW);
    door_light_state = LOW;
}
/******************************************************
 * *********** ALARM FUNCTIONS*************************
 * ***************************************************/

void turnOnAlarm(){
    digitalWrite(ALARM_CONTROL_PIN, LOW);
    alarm_state = HIGH;
}

void turnOffAlarm(){
    digitalWrite(ALARM_CONTROL_PIN, HIGH);
    alarm_state = LOW;
}
/******************************************************
************ SERIAL COMMS FUNCTIONS *******************
********************************************************/

void initSerial() {
    Serial.begin(BAUD_RATE);
}

/******************************************************
************** DOOR CONTROLLER FUNCTIONS **************
*******************************************************/

/* Unlock the door */
void unlockDoor() {
    if (turnOnDoorControllerPower() && getDoorStatus() == DS_CLOSED) {
        sendOpenOrCloseCommand();
    }
}

/* Lock the door*/
void lockDoor() {
    if (turnOnDoorControllerPower() && getDoorStatus() == DS_OPEN) {
        sendOpenOrCloseCommand();
    }
}

void sendOpenOrCloseCommand(){
    digitalWrite(DC_CONTROL_PIN, HIGH);
    while (digitalRead(DC_STATUS_PIN)){ // Wait for pin to turn LOW

    }
    digitalWrite(DC_CONTROL_PIN, LOW); // Finish the command

  DoorStatus previousStatus = doorStatus;

  while(getDoorStatus() == previousStatus){ // Wait for the operation to complete
  }
}

/* Turn off power to door controller */
void turnOffDoorControllerPower() {
    digitalWrite(DC_POWER_PIN, HIGH);
}

/* Turn on power to door controller */
bool turnOnDoorControllerPower() {

    int tries = 0;
    while (!getDCState()) {
        digitalWrite(DC_POWER_PIN, LOW);
        delay(2000);

        if (tries == 3) {
            errorAlert("Could not power DC");
            return false;
        }

        tries++;
    }

    return true;
}

/* Get the current state of the door controller */
int getDCState() {
    return digitalRead(DC_STATUS_PIN);
}

DoorStatus getDoorStatus(bool powerDc){

  if(!powerDc){
    if(digitalRead(DOOR_STATUS_PIN)){
          doorStatus = DS_OPEN;
     }else{
          doorStatus = DS_CLOSED;
    }

    return doorStatus;
  }
  
  if(turnOnDoorControllerPower()){
        if(digitalRead(DOOR_STATUS_PIN)){
            doorStatus = DS_OPEN;
        }else{
            doorStatus = DS_CLOSED;
        }
    }else{
        doorStatus = DS_UNKNOWN;
    }

    return doorStatus;
}

DoorStatus getDoorStatus(){
    return getDoorStatus(true);
}

void checkDoorPad(){
  if(digitalRead(PAD_DOOR_PIN)){
    turnOnAlarm();
    alarm_turned_on_millis = millis();
  }else if(millis() - alarm_turned_on_millis >= (alarm_turn_off_timeout * 1000) ){
    turnOffAlarm();
  }
}

/******************************************************
********* WIFI FUNCTIONS ******************************
*******************************************************/
void initWifi() {
    log("Initializing WiFi");
    esp8266.begin(BAUD_RATE);

    WiFi.init(&esp8266);

    long start_millis = millis();
    int timeout = 20000; // 20 seconds

    while (WiFi.status() == WL_NO_SHIELD && millis() - start_millis < timeout) {
        log("WiFi shield not present");
        delay(1000);
    }

    if (WiFi.status() == WL_NO_SHIELD) {
        log("Wifi initialization failed");
        // Incase we have no other way to unlock the door. (The module is dead)
    }
}

/**
* Connect to wifi
*/
bool connectWifi() {
    log("Connecting to Wifi");

    if (wifi_status == WL_CONNECTED) {
        log("Already connected");
        return true;
    }

    if (WiFi.status() == WL_NO_SHIELD) {
        initWifi();

        if (WiFi.status() == WL_NO_SHIELD) {
            return false;
        }
    }

    wifi_status = WiFi.status();

    long start_millis = millis();
    int timeout = 20000; // 20 seconds

    while (wifi_status != WL_CONNECTED && millis() - start_millis < timeout) {

        log(String("Attempting to connect to SSID: ") + String(WIFI_SSID));

        // Connect to WPA/WPA2 network. Change this line if using open or WEP network:

        wifi_status = WiFi.begin(WIFI_SSID, WIFI_PASS);

        // wait 10 seconds for connection:

        delay(10000);

    }

    if (wifi_status != WL_CONNECTED) {
        log("Failed to connect");
        // Wifi is probably down.
        return false;
    }

    log("Connected successfully");

    return true;
}

String IpAddress2String(const IPAddress& ipAddress)
{
  return String(ipAddress[0]) + String(".") +\
  String(ipAddress[1]) + String(".") +\
  String(ipAddress[2]) + String(".") +\
  String(ipAddress[3])  ; 
}

void initUdp(){
    Udp.begin(udpPort);
}

void udpListen(){
    int packetSize = Udp.parsePacket();

    char packetBuffer[10];
    if (packetSize) {
        delay(10);
        int len = Udp.read(packetBuffer, 10);

        if (len > 0) {
            packetBuffer[len] = 0;
        }

        //log("Contents:" + String(packetBuffer));

        // send a reply, to the IP address and port that sent us the packet we received
        Udp.beginPacket(Udp.remoteIP(), Udp.remotePort());
        Udp.write(CMD_ACK.c_str());
        Udp.endPacket();

        executeCommand(String(packetBuffer));
    }
}

boolean find(String string, String value) {
    return string.indexOf(value) >= 0;
}

String sendToServer(char data[]) {
    if (!connectWifi()) {
        return "";
    }

    log("Sending data to server: " + String(data), false);
    if (client.connect(SERVER, PORT)) {
        log("connected to server", false);

        // Make a HTTP request:
        client.println("POST /data HTTP/1.1");
        String contentLength = String("Content-Length: ") + String(data).length();
        client.println(contentLength.c_str());
        client.println("Content-Type: text/plain");
        client.println();
        client.println(data);
    } else {
        log("Error connecting to server", false);
        // Server connection fails. 
        return "";
    }

    String response = "";
    while (client.available()) {
        char c = client.read();
        response += c;
    }

    log("disconnecting from server.", false);

    client.stop();

    return response;
}

void logToServer(String data) {
    if (!connectWifi()) {
        return;
    }

    log("Sending logs to server: " + String(data), false);
    if (client.connect(SERVER, PORT)) {
        log("connected to server", false);

        // Make a HTTP request:
        client.println("POST /logs HTTP/1.1");
        String contentLength = String("Content-Length: ") + String(data).length();
        client.println(contentLength.c_str());
        client.println("Content-Type: text/plain");
        client.println();
        client.println(data);
    } else {
        log("Error connecting to server", false);
    }

    log("disconnecting from server.", false);

    client.stop();

}

/*Utils*/
void log(String data) {
    log(data, true);
}

void log(String data, bool sendToServer) {
    if (DEBUG) {
        Serial.println(data);

        /*if(sendToServer){
            logToServer(data);
         }*/
    }
}

String getValueAtIndex(String data, char separator, int index)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}
