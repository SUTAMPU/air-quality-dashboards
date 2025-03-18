ALTER TABLE Worldwide
ADD AQI_level VARCHAR(50) AS (
    CASE
        WHEN pm25 <= 9 THEN 'Good'
        WHEN pm25 > 9 AND pm25 <= 35.4 THEN 'Moderate'
        WHEN pm25 > 35.4 AND pm25 <= 55.4 THEN 'Unhealthy for Sensitive Groups'
        WHEN pm25 > 55.4 AND pm25 <= 125.4 THEN 'Unhealthy'
        WHEN pm25 > 125.4 AND pm25 <= 225.4 THEN 'Very Unhealthy'
        WHEN pm25 > 225.5 THEN 'Hazardous'
        ELSE 'Unknown'
    END
);