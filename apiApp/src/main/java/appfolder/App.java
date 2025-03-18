package appfolder;

// ---- HTTP Dependencies ----
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.json.JSONArray;
import org.json.JSONObject;

public class App {
    public static void main(String[] args) {
        int i = 1; // Execution count
        String apiKey = "";

        // ---- InfluxDB ----
        String token = "";
        String bucket = "air-quality";
        String org = "";
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http:///", token.toCharArray());
        WriteApiBlocking writeApi = influxDBClient .getWriteApiBlocking();

        // ---- MySQL ----
        String sqlURL = "jdbc:mysql:///air_quality";
        String username = "";
        String password = "";

        // ------------------------- CHOOSING PARAMETERS -------------------------
        // Thailand
        int[] THlocationIds = {225629, 225567, 228544, 353740, 228627, 354124}; // Sensor IDs
        String[] THlocationNames = {"Bangkok", "North", "Northeast", "East", "South", "West"};
        
        // Worldwide
        int[] locationIds = {1324, 589967, 2162178, 1590755, 8118, 225617, 1214722, 9466};
        String[] locationNames = {"North America", "South America", "Europe", "Africa", "Asia(India)", "Asia(Thailand)", "Asia(Japan)", "Australia"};

        HttpClient client = HttpClient.newHttpClient();

        try {
            while (true) {
                System.out.println("---------------------------------");
                System.out.println("Execution Sequence " + i);
                LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Bangkok"));
                LocalDateTime roundedTime = currentTime.withMinute(0).withSecond(0).withNano(0);
                System.out.println("Asia/Bangkok Time: " + roundedTime);

                // Create a MySQL connection
                try (Connection connect = DriverManager.getConnection(sqlURL, username, password)) {
                    if (connect != null) {
                        System.out.println("Connected to MySQL successfully!");

                        // Loop through for Thailand locations (PM10, PM2.5, O3, NO2, SO2, and CO)
                        for (int j = 0; j < THlocationIds.length; j++) {
                            locationValues(THlocationIds[j], THlocationNames[j], client, apiKey, true, writeApi, bucket, org, connect);
                        }

                        // Loop through for Worldwide locations (PM2.5)
                        for (int j = 0; j < locationIds.length; j++) {
                            locationValues(locationIds[j], locationNames[j], client, apiKey, false, writeApi, bucket, org, connect);
                        }
                    } 
                } catch (SQLException sqlErr) {
                    System.out.println("...Failed connection");
                    System.out.println(sqlErr.toString());
                }

                i++;
                // Delay calculations
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
            }
        } catch (Exception connErr) {
            System.out.println(connErr.toString());
            Thread.currentThread().interrupt();
        } finally {
            influxDBClient.close();
        }
    }

    // ------------------------- FETCHING API RESPONSE -------------------------
    public static void locationValues(int locationId, String locationName, HttpClient client, String apiKey, boolean getAllValues, 
                                        WriteApiBlocking writeApi, String bucket, String org, Connection connect) {
        String urlStr = "https://api.openaq.org/v3/locations/" + locationId + "/sensors";

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "Java HttpClient")
            .header("Cache-Control", "no-cache")
            .header("X-API-Key", apiKey)
            .uri(URI.create(urlStr))
            .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            
            // If status text is 200 = OK = successful request
            if (response.statusCode() == 200) {
                JSONObject jsonObject = new JSONObject(response.body());
                JSONArray results = jsonObject.getJSONArray("results");

                // Initialising latest values and sensor's location
                float pm25 = 0, pm10 = 0, no2 = 0, co = 0, so2 = 0,  o3 = 0, latitude = 0, longitude = 0;
                // Conversions
                float no2Weight = 46.0055f, coWeight = 28.0101f, so2Weight = 64.064f, o3Weight = 47.9982f, divisor = 22.414f;

                // Loop through 'results' array
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

                // InfluxDB measurements
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
                // Write the points to InfluxDB
                writeApi.writePoint(bucket, org, point);

                // MySQL measurements
                try {
                    Statement stm = connect.createStatement();
                    String sqlstm;
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
                    System.out.println("Query Inserted for: " + locationName);

                } catch (SQLSyntaxErrorException stmErr) {
                    System.out.println("Invalid Query");
                }
                
                // Print based on the required values
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
                
                Thread.sleep(60000); // Wait 60 seconds before calling the next location
            
            // Try again if request isn't successful
            } else {
                System.out.println("Location error: " + locationName + ", Status code " + response.statusCode());
                System.out.println("Retrying...");
                Thread.sleep(60000);
            }

        } catch (Exception err) {
            System.out.println("Location data error: " + locationName + ", " + err);
            System.out.println("Retrying...");
            try {
                Thread.sleep(60000);
            } catch (Exception err2) {
                System.out.println(err2.toString());
            }
        }
    }
}