package com.example.temperaturemonitor.repository;

import com.example.temperaturemonitor.model.ObjectEntity;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface ObjectRepository extends CrudRepository<ObjectEntity, Long> {
    Optional<ObjectEntity> findByLocation(String location);
}

