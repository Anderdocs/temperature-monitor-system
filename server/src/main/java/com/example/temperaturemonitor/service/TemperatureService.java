package com.example.temperaturemonitor.service;

import com.example.temperaturemonitor.model.TemperatureMeasurement;
import com.example.temperaturemonitor.model.DeviceReadingRequest;
import java.util.List;

public interface TemperatureService {
    void recordTemperature(DeviceReadingRequest readingRequest);
    List<TemperatureMeasurement> getTemperatureHistory(String location);
    boolean deleteCity(String location);
    List<String> getAllCities();
}