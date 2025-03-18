# How to run Grafana locally (Windows)

### First installation
<details>
<summary>Click to see first installation process</summary>

<br> Download the __JSON and CSV__ files within this (dashboard) folder and install __MySQL and Grafana__ into your local machine. <br><br>
__MySQL:__ Import the CSV files into your server. <br>
__Grafana:__ Choose MySQL as the database and import the dashboard.

<details>
<summary>Setting up MySQL</summary>

### 1. Installing MySQL
Visit the [MySQL download page](https://dev.mysql.com/downloads/installer/) and install the latest version of MySQL.

### 2. Importing CSV files
The CSV files can be imported directly through MySQL Command Line Client or using HeidiSQL GUI. From here, MySQL service has to be running.
_Note: After installation, MySQL service should start running automatically but if it is not, you can enable it through the command line as the administrator using:_
```
net start mysql80
```
<details>
<summary>Through MySQL Command Line Client</summary>
    
1. Create a new database:
```
CREATE DATABASE `air_quality`;
USE air-quality-static;
```
2. Create new tables:
```
CREATE TABLE Location (
    location_id INT PRIMARY KEY,
    location_name VARCHAR(50),
    location_city VARCHAR(50),
    region VARCHAR(50),
    latitude FLOAT,
    longitude FLOAT
);

CREATE TABLE Thailand (
    measurement_id INT PRIMARY KEY AUTO_INCREMENT,
    location_id INT,
    pm25 FLOAT,
    pm10 FLOAT,
    no2 FLOAT,
    co FLOAT,
    so2 FLOAT,
    o3 FLOAT,
    recorded_at TIMESTAMP,
    AQI INT,
    FOREIGN KEY (location_id) REFERENCES Location(location_id)
);

CREATE TABLE Worldwide (
    measurement_id INT PRIMARY KEY AUTO_INCREMENT,
    location_id INT,
    pm25 FLOAT,
    recorded_at TIMESTAMP,
    AQI INT,
    FOREIGN KEY (location_id) REFERENCES Location(location_id)
);
```
3. Importing CSV files
```
LOAD DATA INFILE '/path/to/Location.csv'
INTO TABLE Location
FIELDS TERMINATED BY ',' 
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

LOAD DATA INFILE '/path/to/Thailand.csv'
INTO TABLE Thailand
FIELDS TERMINATED BY ',' 
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;

LOAD DATA INFILE '/path/to/Worldwide.csv'
INTO TABLE Worldwide
FIELDS TERMINATED BY ',' 
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
```
</details>

#### Through HeidiSQL
1. Connect to the server:
  - The hostname or IP address depends on how your MySQL server is set up.
  - By default, if you are running from your local machine, you can use localhost or 127.0.0.1 as the hostname.
  - Enter your username and password as registered.
3. Create a new `air-quality-static` database
4. Create 3 tables: `location`, `thailand` and `worldwide`.
5. Import CSV files by navigating to `Tools -> Import text file` and using `Client parses file contents` as the import method _(See 'Notes' section if there is an error)_.

### Notes
- If there is an error during the importing process, you must enable loading local data by running:
```
SET GLOBAL local_infile = 1;

# Verifying:
SHOW VARIABLES LIKE 'local_infile';
```
- Here are the list of commands for verification:
```
# Checking the port:
SHOW VARIABLES LIKE 'port';
# Alternatively, you can navigate to the "my.ini" file in C:\ProgramData\MySQL\MySQL Server 8.0\ and find the MySQL port that is listening on Windows.

# Checking the database:
SHOW DATABASES;

# Checking the tables:
USE database-name;
SHOW TABLES;
```

</details>

<details>
<summary>Setting up Grafana</summary>

### 1. Installing Grafana
Visit the [Grafana download page](https://grafana.com/grafana/download?platform=windows) and install the latest version of Grafana.

### 2. Running Grafana
Enable Grafana by running the `grafana-server` application.
```
cd C:\Program Files\GrafanaLabs\grafana\bin
grafana-server
```
_If you wish to change the default port link, modify the `[server]` section. Make sure to restart the server by running the application again if you do so._
```
[server]
http_port = 3001
```

### 3. Accessing Grafana
- Using the default Grafana's port link `http://localhost:3000` (_if not changed_), insert it to your browser.
- Log in using the default username (_admin_) and password (_admin_).

### 4. Adding the data source
- Navigate to `Connections -> Data sources`.
- Press `Add data source` and select `MySQL` data source type.
- Configurate MySQL:
    - __Host:__ ip-address/port: `localhost:3306`.
    - __Database:__ database-name: `air_quality`.
    - __Username:__ username.
    - __Password:__ password.
- Save & Test`.

### 5. Installing plugins
The dashboard uses a plugin to display the buttons for the data timespan. You can install it by:
1. Navigate to `Administration -> Plugins and data -> Plugins`.
2. Install `Timepicker Buttons Panel`.

### 6. Importing the dashboard
- Navigate to `Dashboards`.
- Import the dashboard by pressing `New -> Import`
- Upload the _JSON_ file and select _MySQL_ as `air-quality-data`.
- Press `import`.
_Note: If the dashboard is shown with the default dark theme and you wish to change it, navigate to `Administration -> Default preferences -> Interfact theme` and select the theme that you want._

</details>

</details>

### After installation
<details open>
<summary>Click to see how to access Grafana after installation process</summary>
    
1. Start MySQL server through the command line as the administrator using:
```
net start mysql
```
2. Enable Grafana by running the `grafana-server` application.
```
cd C:\Program Files\GrafanaLabs\grafana\bin
grafana-server
```
_Note: You must leave the command line opens as the server will automatically shuts down if you close it._ <br>

3. Opens Grafana with your port link (`http://localhost:3000` by default).
4. Navigate to `Dashboards`.

</details>
