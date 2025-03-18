-- Based on US AQI
ALTER TABLE Worldwide
ADD AQI INT AS (
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
   END
);