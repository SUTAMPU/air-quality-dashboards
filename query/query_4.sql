-- Based on Thailand AQI
ALTER TABLE Thailand
ADD AQI INT AS (
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
   END
);