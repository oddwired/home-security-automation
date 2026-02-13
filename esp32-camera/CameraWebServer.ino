#include "esp_camera.h"
#include <WiFi.h>

//
// WARNING!!! PSRAM IC required for UXGA resolution and high JPEG quality
//            Ensure ESP32 Wrover Module or other board with PSRAM is selected
//            Partial images will be transmitted if image exceeds buffer size
//

// Select camera model
//#define CAMERA_MODEL_WROVER_KIT // Has PSRAM
//#define CAMERA_MODEL_ESP_EYE // Has PSRAM
//#define CAMERA_MODEL_M5STACK_PSRAM // Has PSRAM
//#define CAMERA_MODEL_M5STACK_V2_PSRAM // M5Camera version B Has PSRAM
//#define CAMERA_MODEL_M5STACK_WIDE // Has PSRAM
//#define CAMERA_MODEL_M5STACK_ESP32CAM // No PSRAM
#define CAMERA_MODEL_AI_THINKER // Has PSRAM
//#define CAMERA_MODEL_TTGO_T_JOURNAL // No PSRAM

#include "camera_pins.h"

const char* ssid = "S_HOME";
const char* password = "3030031343dc";
const char SERVER[] = "192.168.43.1";
const int PORT = 8080;

const int MOTION_PIN = 13;
const int NIGHT_VISION_PIN = 12;

unsigned int ping_interval = 10;
unsigned long last_ping_millis = 0;

unsigned int motion_detect_interval = 1;
unsigned long last_motion_detect = 0;

unsigned int night_vision_timeout = 10;
unsigned long nv_turned_on_millis = 0;

void startCameraServer();
WiFiClient client;

void setup() {
  Serial.begin(115200);
  Serial.setDebugOutput(true);
  Serial.println();

  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  
  // if PSRAM IC present, init with UXGA resolution and higher JPEG quality
  //                      for larger pre-allocated frame buffer.

  config.frame_size = FRAMESIZE_SVGA;
  config.jpeg_quality = 12;
  config.fb_count = 1;
  /*if(psramFound()){
    config.frame_size = FRAMESIZE_UXGA;
    config.jpeg_quality = 10;
    config.fb_count = 2;
  } else {
    config.frame_size = FRAMESIZE_SVGA;
    config.jpeg_quality = 12;
    config.fb_count = 1;
  }*/

//#if defined(CAMERA_MODEL_ESP_EYE)
//  pinMode(13, INPUT_PULLUP);
//  pinMode(14, INPUT_PULLUP);
//#endif

  // camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return;
  }

  sensor_t * s = esp_camera_sensor_get();
  // initial sensors are flipped vertically and colors are a bit saturated
  if (s->id.PID == OV3660_PID) {
    s->set_vflip(s, 1); // flip it back
    s->set_brightness(s, 1); // up the brightness just a bit
    s->set_saturation(s, -2); // lower the saturation
  }
  // drop down frame size for higher initial frame rate
  s->set_framesize(s, FRAMESIZE_QVGA);

#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
  s->set_vflip(s, 1);
  s->set_hmirror(s, 1);
#endif

  connectWifi();
  Serial.println("");
  Serial.println("WiFi connected");

  startCameraServer();

  Serial.print("Camera Ready! Use 'http://");
  Serial.print(WiFi.localIP());
  Serial.println("' to connect");

  pinMode(MOTION_PIN, INPUT);
  pinMode(NIGHT_VISION_PIN, OUTPUT);

  sendToServer("/cam/boot", "Boot completed");
}

bool connectWifi(){
  if(WiFi.status() != WL_CONNECTED){
      long start_millis = millis();
      int timeout = 20000; // 20 seconds

      while (WiFi.status() != WL_CONNECTED && millis() - start_millis < timeout) {
        Serial.println(String("Attempting to connect to SSID: ") + String(ssid));

        WiFi.begin(ssid, password);
        delay(10000);
      }

      if(WiFi.status() != WL_CONNECTED){
        Serial.println("Failed to connect");

        return false;
      }

      Serial.println("Connected");
    }
    return true;
}

String sendToServer(String endpoint, char data[]) {
    if (!connectWifi()) {
        return "";
    }

    log("Sending data to server: " + String(data), false);
    if (client.connect(SERVER, PORT)) {
        log("connected to server", false);

        // Make a HTTP request:
        String docDefinition = "POST "+ endpoint + " HTTP/1.1";
        client.println(docDefinition.c_str());
        String contentLength = String("Content-Length: ") + String(data).length();
        client.println(contentLength.c_str());
        client.println("Content-Type: text/plain");
        client.println();
        client.println(data);
    } else {
        log("Error connecting to server", false);
        return "";
    }

    while(!client.available()){}
    
    String response = "";
    while (client.available()) {
        //char c = client.read();
        response += client.readStringUntil('\r');
        //response += c;
    }

    log("disconnecting from server.", false);

    client.stop();

    return response;
}

void turnOnNightVision(){
  digitalWrite(NIGHT_VISION_PIN, HIGH);
}

void turnOffNightVision(){
  digitalWrite(NIGHT_VISION_PIN, LOW);
}

void loop() {
  // put your main code here, to run repeatedly:
  //delay(1000);

  connectWifi();

  if((millis() - last_motion_detect >= motion_detect_interval * 1000) && digitalRead(MOTION_PIN)){
      last_motion_detect = millis();
      sendToServer("/cam/motion", "Motion detected");
      turnOnNightVision();
      nv_turned_on_millis = millis();
   }

   if(millis() - nv_turned_on_millis >= night_vision_timeout * 1000){
      turnOffNightVision();
    }

  if(millis() - last_ping_millis >= (ping_interval * 1000)){
    last_ping_millis = millis();
    String response = sendToServer("/cam/ping", "Ping");
    log("Response: "+ response);
  }
}
void log(String data) {
    log(data, true);
}
void log(String data, bool sendToServer){
  Serial.println(data);
}
