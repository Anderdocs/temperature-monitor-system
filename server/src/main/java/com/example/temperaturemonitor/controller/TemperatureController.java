package com.example.temperaturemonitor.controller;

import com.example.temperaturemonitor.model.TemperatureMeasurement;
import com.example.temperaturemonitor.service.TemperatureService;
import com.example.temperaturemonitor.model.DeviceReadingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/temperature")
public class TemperatureController {
    @Autowired
    private TemperatureService temperatureService;

    @PostMapping("/record")
    public ResponseEntity<String> recordTemperature(@RequestBody DeviceReadingRequest readingRequest) {
        try {
            temperatureService.recordTemperature(readingRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("Данные от ESP успешно записаны.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при записи данных от ESP: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<TemperatureMeasurement>> getTemperatureHistory(@RequestParam(required = true) String location) {
        List<TemperatureMeasurement> history = temperatureService.getTemperatureHistory(location);
        if (history.isEmpty() && !temperatureService.getAllCities().contains(location)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
        return ResponseEntity.ok(history);
    }

    @GetMapping("/cities")
    public List<String> getAllCities() {
        return temperatureService.getAllCities();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteCity(@RequestParam String location) {
        boolean deleted = temperatureService.deleteCity(location);
        if (deleted) {
            return ResponseEntity.ok("Локация ESP '" + location + "' успешно удалена.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Локация ESP '" + location + "' не найдена.");
        }
    }
}