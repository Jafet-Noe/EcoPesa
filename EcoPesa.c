#include <WiFi.h>
#include <WiFiUdp.h>
#include <WebServer.h>
#include <Preferences.h>
#include "HX711.h"

#define DT 4   // Connect to HX711 DT
#define SCK 5  // Connect to HX711 SCK
#define BOOT_BUTTON 0

Preferences preferences;
WebServer apServer(80);     // Server used during AP mode
WebServer server(8080);     // Server used in normal mode
WiFiUDP udp;
HX711 scale;

bool accessPointMode = false;
String localIpStr;

const char* multicastAddress = "239.0.0.1";
const int multicastPort = 12345;

unsigned long lastSend = 0;

void sendMulticast() {
  String message = "EcoPesa20 - " + localIpStr;
  udp.beginPacket(IPAddress(239, 0, 0, 1), multicastPort);
  udp.print(message);
  udp.endPacket();
  Serial.println("Sent: " + message);
}

void sendWeight() {
  Serial.print("Weight: ");
  float number = scale.get_units();
  Serial.println(number, 2);
  server.send(200, "text/plain", String(number));
  delay(10);
}

void startAccessPoint() {
  WiFi.mode(WIFI_AP);
  WiFi.softAP("EcoPesa20");
  Serial.println("Access Point started. Connect to 'ESP32-Setup'.");

  IPAddress IP = WiFi.softAPIP();
  Serial.print("AP IP: ");
  Serial.println(IP);

  apServer.on("/", HTTP_GET, []() {
  apServer.send(200, "text/html", R"rawliteral(
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>EcoPesa Wi-Fi</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          background-color: #f2f2f2;
          display: flex;
          justify-content: center;
          align-items: center;
          height: 100vh;
          margin: 0;
        }

        .card {
          background-color: white;
          padding: 20px 30px;
          border-radius: 10px;
          box-shadow: 0 4px 8px rgba(0,0,0,0.1);
          width: 90%;
          max-width: 400px;
        }

        h2 {
          text-align: center;
          color: #333;
        }

        input[type=text], input[type=password] {
          width: 100%;
          padding: 12px;
          margin: 10px 0;
          border: 1px solid #ccc;
          border-radius: 5px;
          box-sizing: border-box;
        }

        input[type=submit] {
          width: 100%;
          background-color: #4CAF50;
          color: white;
          padding: 14px;
          border: none;
          border-radius: 5px;
          cursor: pointer;
          font-size: 16px;
        }

        input[type=submit]:hover {
          background-color: #45a049;
        }
      </style>
    </head>
    <body>
      <div class="card">
        <h2>EcoPesa Wi-Fi</h2>
        <form action="/save" method="post">
          <label for="ssid">SSID:</label>
          <input type="text" id="ssid" name="ssid" required>
          <label for="password">Password:</label>
          <input type="password" id="password" name="password" required>
          <input type="submit" value="Guarda & Connecta">
        </form>
      </div>
    </body>
    </html>
  )rawliteral");
});

  apServer.on("/save", HTTP_POST, []() {
    String ssid = apServer.arg("ssid");
    String password = apServer.arg("password");

    if (ssid != "") {
      preferences.putString("ssid", ssid);
      preferences.putString("password", password);
      apServer.send(200, "text/html", "Saved! Rebooting...");
      delay(500);
      preferences.end();
      ESP.restart();
    } else {
      apServer.send(400, "text/html", "SSID required!");
    }
  });

  apServer.begin();
}

void connectToWiFi() {
  String ssid = preferences.getString("ssid", "");
  String password = preferences.getString("password", "");

  if (ssid == "") {
    Serial.println("No saved credentials. Switching to AP mode.");
    accessPointMode = true;
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid.c_str(), password.c_str());

  Serial.printf("Connecting to %s", ssid.c_str());
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 10000) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nConnection failed. Switching to AP mode.");
    accessPointMode = true;
    return;
  }

  localIpStr = WiFi.localIP().toString();
  Serial.println("\nConnected to WiFi. IP: " + localIpStr);
}

void setup() {
  Serial.begin(115200);
  pinMode(BOOT_BUTTON, INPUT_PULLUP);

  if (digitalRead(BOOT_BUTTON) == LOW) {
    accessPointMode = true;
  }

  preferences.begin("wifi", false);

  if (!accessPointMode) {
    setupSesor();
    connectToWiFi();
  }

  if (accessPointMode) {
    startAccessPoint();
    return;
  }

  // Connected mode
  udp.beginMulticast(IPAddress(239, 0, 0, 1), multicastPort);
  server.on("/", sendWeight);
  server.begin();
  Serial.println("Web server started on port 8080.");
}

void setupSesor() {
    Serial.begin(115200);
    scale.begin(DT, SCK);

    // Step 1: Reset the scale to 0
    scale.tare();
    delay(1000); // Give time to stabilize

/*
    delay(5000); // Wait 5 seconds for you to place the 800g

    // Step 2: Get the average reading with known weight
    
    long reading = scale.get_units(20);  // average of 20 reads
    Serial.print("Raw reading with 1000g: ");
    Serial.println(reading);

    // Step 3: Calculate and set calibration factor
    float weight_cover_grams = 1000.0;
    float calibration_factor = (float)reading / weight_cover;  // divide by 1 kg
    //float calibration_factor = 0.006872454162414f;
    Serial.print("Calibration factor: ");
    Serial.println(calibration_factor, 2);

    //scale.set_scale(calibration_factor);  // apply calibration move to app
    */
    scale.set_scale();  
    //Serial.println("Calibration done!");      // Reset the scale to 0
}

void loop() {
  if (accessPointMode) {
    apServer.handleClient();
  } else {
    server.handleClient();

    unsigned long now = millis();
    if (now - lastSend > 5000) {
      sendMulticast();
      lastSend = now;
    }

    if (digitalRead(BOOT_BUTTON) == LOW) {
      Serial.println("BOOT button pressed. Switching to AP mode.");
      delay(500); // Debounce
      preferences.end();
      accessPointMode = true;
      ESP.restart();
    }
  }
}