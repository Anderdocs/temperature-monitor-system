# Temperature Monitor

Remote temperature and humidity monitoring system: ESP sensors send readings to the server, and the Android app displays them by location.

```
ESP32 + DHT11/DS18B20 --POST JSON--> Spring Boot server <--REST-- Android app
```

| Part | Stack | Folder |
|---|---|---|
| Server | Java 23, Spring Boot 3.3, Spring Data JPA, MySQL | [`server/`](server) |
| Android client | Java, Retrofit 2, ViewModel/LiveData, NSD (mDNS) | [`android/`](android) |

## Server API (`/api/temperature`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/record` | Save a reading from an ESP device (`location`, `temperature`, `humidity`) |
| GET | `/cities` | List of locations |
| GET | `/history?location=` | Measurement history for a location |
| DELETE | `/delete?location=` | Delete a location and its data |

## Running the server

```bash
cp .env.example .env   # fill in DB_* or export the variables
cd server
./mvnw spring-boot:run
```

Requires MySQL with a `monitor` database; tables are created automatically (`ddl-auto=update`).

## Android client

Open the `android/` folder in Android Studio. The server address is set in
`app/src/main/java/com/example/temperaturemonitor/network/RetrofitClient.java` (`BASE_URL`).
