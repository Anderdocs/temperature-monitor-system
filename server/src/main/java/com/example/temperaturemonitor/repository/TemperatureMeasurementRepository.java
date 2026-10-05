package com.example.temperaturemonitor.repository;

import com.example.temperaturemonitor.model.TemperatureMeasurement;
import com.example.temperaturemonitor.model.ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemperatureMeasurementRepository extends JpaRepository<TemperatureMeasurement, Long> {
    List<TemperatureMeasurement> findByObject(ObjectEntity object);
}
