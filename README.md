# AQ-Application (Project)
This project is about air quality API. It fetches air quality data from __[OpenAQ](https://openaq.org/)__ for specific locations in Thailand(6) and worldwide(8), processes the data, and stores it in both InfluxDB and MySQL databases.

The data retrieved are from these following sensor locations: <br>
__...Thailand data__ _(PM2.5, PM10, NO₂, CO, SO₂, and O₃)_
| Bangkok  | North | Northeast | East | South | West | 
| -------- | ----- | --------- | ---- | ----- | ---- |
| Nonthaburi | Chiangrai | Sakon Nakhon | Chiangrai | Chanthaburi | Trang | Prachuap Khiri Khan |
| 225629 | 225567 | 228544 | 353740 | 228627 | 354124 |

__...Worldwide data__ _(PM2.5)_
| NA  | SA | EU | AF | ASIA(India) | ASIA(Thailand) | ASIA(Japan) | AUS | 
| --- | -- | -- | ---| ----------- | -------------- | ----------- | --- |
| Florida, US | Sao Paulo, Brazil | Berlin, Germany | Cape Town, South Africa | New Delhi | Bangkok | Tokyo | Canberra |
| 1324 | 589967 | 2162178 | 1590755 | 8118 | 225617 | 1214722 | 9466 |

# About the code
<details>
  <summary>Click to see explaination on the code</summary>
  
  ### Main code:
  #### 1. Initialisation <br>
  The _App_ class contains the main logic and _i_ tracks the number of time the code executes.
  ```
  public class App {
    public static void main(String[] args) {
        int i = 1; // Execution count
        String apiKey = "";
        // ------ HTTP Client Initialisation ------
        HttpClient client = HttpClient.newHttpClient();
  ```
  #### 2. Location parameters <br>
  The arrays contain the location ids and names according to OpenAQ's data (mentioned in the section above).
  ```
  int[] THlocationIds = {225629, 225567, 228544, 353740, 228627, 354124}; // Sensor IDs
  String[] THlocationNames = {"Bangkok", "North", "Northeast", "East", "South", "West"};
  
  int[] locationIds = {1324, 589967, 2162178, 1590755, 8118, 225617, 1214722, 9466};
  String[] locationNames = {"North America", "South America", "Europe", "Africa", "Asia(India)", "Asia(Thailand)", "Asia(Japan)", "Australia"};
  ```
  #### 3. Fetching data <br>
  The _while_ loop continuously fetch data at regular intervals (see 'duration' section). The _locationValues_ method is called for each location and set _true_ if Thailand, _false_ if worldwide.
  ```
  while (true) {
    for (int j = 0; j < THlocationIds.length; j++) {
        locationValues(THlocationIds[j], THlocationNames[j], client, apiKey, true);
    }

    for (int j = 0; j < locationIds.length; j++) {
        locationValues(locationIds[j], locationNames[j], client, apiKey, false);
    }
  ```
  The _locationValues_ method builds and sends an HTTP GET request by combining the location_id in the location parameter with the GET default request (/v3/locations/{locations_id}/sensors). Then, repeat the process with a delay of 60 seconds after each location is called.
  ```
  public static void locationValues(...) {
    String urlStr = "https://api.openaq.org/v3/locations/" + locationId + "/sensors";

    HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .header("Content-Type", "application/json")
        .uri(URI.create(urlStr))
        .build();

    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    ...
    Thread.sleep(60000)
  ```
  If the response is successful (200), then it processes JSON data to extract values. As the main JSONArray is 'result', it loops through the _results.length_ to find the _parameter_id_ and retrieve its _value_ within _latest_ JSONObject. It also retrieve the _latitude_ and _longitude_. Then, store those values into initialised values; store _PM2.5_ to all locations then _PM10, NO₂, CO, SO₂, and O₃_ values to location in Thailand only (when _getAllValue_ is true). The unit of concentrations are also converted using: $`\text{µg/m³} = ppb\times\frac{\text{molar mass of gas}}{\text{molar volume at STP}}`$.
  ```
  if (response.statusCode() == 200) {
    JSONObject jsonObject = new JSONObject(response.body());
    JSONArray results = jsonObject.getJSONArray("results");

  for (int k = 0; k < results.length(); k++) {
      // Find latest values and time, based on paramter IDs
      JSONObject sensor = results.getJSONObject(k);
      int parameterId = sensor.getJSONObject("parameter").getInt("id");
      JSONObject latestData = sensor.getJSONObject("latest");
      float latestValue = latestData.getFloat("value");
  
      // Find latitude and longitude from 'coordinates'
      JSONObject coordinates = latestData.getJSONObject("coordinates");
      latitude = coordinates.getFloat("latitude");
      longitude = coordinates.getFloat("longitude");
  
      // Store the latest data
      if (parameterId == 2) { // Store only pm2.5 values first
          pm25 = latestValue;
      }
      
      if (getAllValues == true) { // And if its all values
          switch (parameterId) {  // Rule switch
              case 1 -> pm10 = latestValue;
              case 7 -> no2 = ((latestValue*1000) * (no2Weight/divisor));
              case 8 -> co = ((latestValue*1000) * (coWeight/divisor));
              case 9 -> so2 = ((latestValue*1000) * (so2Weight/divisor));
              case 10 -> o3 = ((latestValue*1000) * (o3Weight/divisor));
          }
      }
  }
  ```
  #### 4. Duration <br>
  As most of the data source updates every hour, the code is programmed the fetch data every hour. However, due to internal server error (500), we have to send API request at a slower rate, with a delay of 60 seconds. As it takes around 14 minutes to retrieve all data, _LocalDateTime_ function is used for the duration of the nearest hour instead.
  ```
  LocalDateTime nextHour = currentTime.plusHours(1).withMinute(0).withSecond(0).withNano(0);
  Duration delayDuration = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Bangkok")), nextHour);
  long delay = delayDuration.toMillis();
  
  System.out.println("Delaying until next hour...");
  try {
      Thread.sleep(delay);
  } catch (Exception err3) {
      System.out.println(err3.toString());
      Thread.currentThread().interrupt();
  }
  ```
  #### 5. Printing:
  Print the values depending on the _locationValues_.
  ```
  if (getAllValues) {
      System.out.println(locationName + ": [PM2.5: " + String.format("%.2f", pm25) + " µg/m3, " +
          "PM10: " + String.format("%.2f", pm10) + " µg/m3, " +
          "NO2: " + String.format("%.4f", no2) + " µg/m3, " +
          "CO: " + String.format("%.4f", co) + " µg/m3, " +
          "SO2: " + String.format("%.4f", so2) + " µg/m3, " +
          "O3: " + String.format("%.4f", o3) + " µg/m3]");
  } else {
      System.out.println(locationName + ": [PM2.5: " + String.format("%.2f", pm25) + " µg/m3]");
  }
  ```
  #### 6. Error handling:
  Waits and retry if there is an error when the API call.
  ```
  } catch (Exception err) {
      System.out.println("Location data error: " + locationName + ", " + err);
      System.out.println("Retrying...");
      try {
          Thread.sleep(60000);
      } catch (Exception err2) {
          System.out.println(err2.toString());
      }
  }
  ```

  ### InfluxDB code:
  #### 1. Initialisation:
  ```
  String token = ""; 
  String bucket = "";
  String org = "";
  InfluxDBClient influxDBClient = InfluxDBClientFactory.create("", token.toCharArray());
  WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
  ```
  #### 2. Storage:
  For each location, the air quality data is stored in InfluxDB as a point measurement tagged with location. Store _PM2.5_ values in all locations, and _PM10, NO₂, CO, SO₂, and O₃_ values if the _locationValues_ is true.
  ```
  Point point = Point.measurement("air_quality")
  .addTag("location", locationName)
  .addField("pm25", pm25);

  if (getAllValues) {
      point.addField("pm10", pm10)
          .addField("no2", no2)
          .addField("co", co)
          .addField("so2", so2)
          .addField("o3", o3);
  }
  writeApi.writePoint(bucket, org, point);
  ```
  
  ### MySQL code:
  #### 1. Initialisation
  ```
  String sqlURL = "";
  String username = "";
  String password = "";
  ```
  #### 2. Storage:
  For each location, the air quality data is stored depending on the _locationValues_, in MySQL as the column and in either Thailand or worldwide table.
  ```
  Statement stm = connect.createStatement();

  if (getAllValues) {
  // Insert into the Thailand table
  sqlstm = String.format(
      "INSERT INTO Thailand (location_id, pm25, pm10, no2, co, so2, o3, recorded_at) " +
      "VALUES ('%d', %.2f, %.2f, %.4f, %.4f, %.4f, %.4f, NOW())",
      locationId, pm25, pm10, no2, co, so2, o3);
  } else {
  // Insert into the Worldwide table
  sqlstm = String.format(
      "INSERT INTO Worldwide (location_id, pm25, recorded_at) " +
      "VALUES ('%d', %.2f, NOW())",
      locationId, pm25);
  }

  stm.executeUpdate(sqlstm);
  ```
</details>

# About the project
The structure of this project runs as follows:
#### Data APIs -> Virtual Private Servers (Application Server and Database Server) -> Monitoring
...where the development process uses VS Code as text editor, Java as programming language and Apache Maven as framework. Based on the structure, I used:
#### HTTP Requests (Java) -> InfluxDB & HeidiSQL GUI -> Grafana

### Virtual private servers
All VPS are created using VMware and simulate Ubuntu as the operating software, where:
- __AQAppServer__: Return dynamic responses to a client request.
- __AQDataServer__: Store time-series(InfluxDB) & relational(MySQL) dataset.
- __AQVisualServer__: Monitor data through interactive visualisation.

### MySQL
There are 3 tables: Location, Thailand and Worldwide, and they are connected by _location_id_. The following shows the column list in each table:
- __Location__: _location_id, location_name, location_city, region, latitude and longitude._
- __Thailand__: _measurement_id, location_id, pm25, pm10, no2, co, so2, o3, recorded_at and AQI._
- __Worldwide__: _measurement_id, location_id, pm25, recorded_at and AQI._ <br>
The _AQI_ field is a calculated field. It calculates the AQI with PM2.5 values using:
$`\text{AQI} = \frac{\text{I}_{hi} - \text{I}_{lo}}{\text{BP}_{hi} - \text{BP}_{lo}}(\text{C}_p - \text{BP}_{lo}) + \text{I}_{lo}`$. For Thailand, it calculates based on Thailand AQI and worldwide calculates based on US AQI. <br>

__...Thailand AQI__
```
 CASE
  WHEN pm25 <= 15 THEN
    ROUND((25 - 0) / (15.0 - 0) * (pm25 - 0) + 0)
   WHEN pm25 > 15 AND pm25 <= 25 THEN
    ROUND((50 - 26) / (25 - 15.1) * (pm25 - 15.1) + 26)
  WHEN pm25 > 25 AND pm25 <= 37.5 THEN 
    ROUND((100 - 51) / (37.5 - 25.1) * (pm25 - 25.1) + 51)
  WHEN pm25 > 37.5 AND pm25 <= 75 THEN 
    ROUND((200 - 101) / (75 - 37.6) * (pm25 - 37.6) + 101)
   WHEN pm25 > 75.1 THEN 
    ROUND((300 - 201) / (125.5 - 75.1) * (pm25 - 75.1) + 201)
   ELSE NULL
 END;
```
__...US AQI__
```
 CASE
  WHEN pm25 <= 9 THEN
    ROUND((50 - 0) / (9.0 - 0) * (pm25 - 0) + 0)
   WHEN pm25 > 9 AND pm25 <= 35.4 THEN
    ROUND((100 - 51) / (35.4 - 9.1) * (pm25 - 9.1) + 51)
  WHEN pm25 > 35.4 AND pm25 <= 55.4 THEN 
    ROUND((150 - 101) / (55.4 - 35.5) * (pm25 - 35.5) + 101)
  WHEN pm25 > 55.4 AND pm25 <= 125.4 THEN 
    ROUND((200 - 151) / (125.4 - 55.5) * (pm25 - 55.5) + 151)
   WHEN pm25 > 125.4 AND pm25 <= 225.4 THEN 
    ROUND((300 - 201) / (225.4 - 125.5) * (pm25 - 125.5) + 201)
   WHEN pm25 > 225.5 THEN 
    ROUND((500 - 301) / (500.4 - 225.5) * (pm25 - 225.5) + 301)
   ELSE NULL
 END;
```

### Data visualisation
There are 2 dashboards available, Thailand and Worldwide. Both dashboards can display data in the past 24 hours, 3 days and 5 days, however, the time can be manually selected through Grafana's dashboard as well. The following is an example dashboard of each region: <br>
__...Thailand__
![thailand-dashboard](https://github.com/SUTAMPU/air-quality-dashboards/blob/main/dashboard/preview-1.jpg?raw=true)
__...Worldwide__
![worldwide-dashboard](https://github.com/SUTAMPU/air-quality-dashboards/blob/main/dashboard/preview-2.jpg?raw=true)

# Opening the project
### Executing the code
1. Open all VPSs.
2. Run the following command:
```
# Within VSCode via MVN
cd .\air-quality\apiApp
mvn exec:java

# Local excecution:
cd javaDeploy
java -jar AQRecordApp.jar

# Remote excecution:
ssh remote_username@remote_host -p 2222
cd javaDeploy
java -jar AQRecordApp.jar
```

### Opening the dashboard
To see the dashboard, both database and visual servers need to be opened.
#### AQDataServer
1. Opens the server.
2. Type the following command:
```
sudo systemctl start influxdb.service

# Verify the status of Grafana and server
sudo systemctl status influxdb.service
sudo systemctl status mysql.service
sudo systemctl status ufw.service
```
#### Opens AQVisualServer
1. Opens the server.
2. Type the following command:
```
sudo systemctl start grafana-server.service

# Verify the status of Grafana and server
sudo systemctl status grafana-server.service
sudo systemctl status ufw.service
```
Then, it can be accessed through the URL format of \<AQVisualServer IP Address>:3000 (in this case, http://192.168.40.132:3000/)\. <br> The IP address can be checked using the following command:
```
ip address
```

# Editing the project

  <details>
    <summary>Click to see required tools and softwares.</summary>
    
  #### Required:
  1. Text Editor: VS Code
  2. Programming Language: Java (JDK)
  3. Project Structure: Apache Maven
  4. Database (Structured): MySQL
  5. Database Management (Structured): Heidi SQL
  6. Database (Time-Serie): InfluxDB
  7. Database Management (Time-Serie): InfluxDB GUI
  8. File Transfer (Software): FileZilla
  9. File Transfer (Platform): GitHub
  10. Monitoring Dashboard: Grafana
  11. VPS Simulation: VMware
  12. Operating System: Ubuntu
  
  #### Download:
  1. Text Editor: VS Code
  2. Programming Language: Java (JDK)
  3. Project Structure: Apache Maven
  4. Database Management (Structured): Heidi SQL
  5. File Transfer (Software): FileZilla
  6. VPS Simulation: VMware
  7. Operating System: Ubuntu

  #### Sign-up:
  1. File Transfer (Software): FileZilla
  2. Monitoring Dashboard: Grafana
  
  #### Optional:
  1. API Platform: Postman
  </details>
  
  ### After making changes to the files
  1. Run the following command within VSCode:
  ```
  cd .\air-quality\apiApp
  mvn clean install
  mvn compile assembly:single
  ```
  2. Connect AQAppServer via FileZilla.
  3. Move the generated file (.\air-quality\apiApp\target) into javaDeply.
  4. Execute the code.
</details>

_More information about the project can be seen in this 
[presentation.](https://www.canva.com/design/DAGUS0DYtzk/upPoHhPKDY3g26YIdGCGCA/edit?utm_content=DAGUS0DYtzk&utm_campaign=designshare&utm_medium=link2&utm_source=sharebutton)_
