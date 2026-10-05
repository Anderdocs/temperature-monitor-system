package com.example.temperaturemonitor.service;

import com.example.temperaturemonitor.model.ObjectEntity;
import com.example.temperaturemonitor.model.TemperatureMeasurement;
import com.example.temperaturemonitor.repository.ObjectRepository;
import com.example.temperaturemonitor.repository.TemperatureMeasurementRepository;
import com.example.temperaturemonitor.model.DeviceReadingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TemperatureServiceImpl implements TemperatureService {
    @Autowired
    private TemperatureMeasurementRepository temperatureMeasurementRepository;

    @Autowired
    private ObjectRepository objectRepository;

    @Override
    @Transactional
    public void recordTemperature(DeviceReadingRequest readingRequest) {
        if (readingRequest == null) {
            System.err.println("Получен некорректный запрос на запись: DTO объект (readingRequest) = null");
            return;
        }

        if (readingRequest.getLocation() == null) {
            System.err.println(
                    "Получен некорректный запрос на запись: readingRequest.getLocation() = null. " +
                            "Содержимое полученного DTO: " +
                            "temperature=" + readingRequest.getTemperature() +
                            ", humidity=" + readingRequest.getHumidity() +
                            ", (сам DTO объект: " + readingRequest.toString() + ")"
            );
            return;
        }

        String location = readingRequest.getLocation();
        double temperature = readingRequest.getTemperature();
        double humidity = readingRequest.getHumidity();

        ObjectEntity object = objectRepository.findByLocation(location)
                .orElseGet(() -> {
                    ObjectEntity newObject = new ObjectEntity();
                    newObject.setName(location);
                    newObject.setLocation(location);
                    return objectRepository.save(newObject);
                });

        TemperatureMeasurement measurement = new TemperatureMeasurement();
        measurement.setTemperature(temperature);
        measurement.setHumidity(humidity);
        measurement.setTimestamp(LocalDateTime.now());
        measurement.setObject(object);

        temperatureMeasurementRepository.save(measurement);
        System.out.println("Сохранены данные от ESP '" + location + "': Temp=" + temperature + ", Hum=" + humidity);
    }

    @Override
    public List<TemperatureMeasurement> getTemperatureHistory(String location) {
        ObjectEntity object = objectRepository.findByLocation(location).orElse(null);
        return object != null ? temperatureMeasurementRepository.findByObject(object) : List.of();
    }

    @Override
    public List<String> getAllCities() {
        return StreamSupport.stream(objectRepository.findAll().spliterator(), false)
                .map(ObjectEntity::getLocation)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean deleteCity(String location) {
        return objectRepository.findByLocation(location)
                .map(city -> {
                    objectRepository.delete(city);
                    return true;
                }).orElse(false);
    }
}