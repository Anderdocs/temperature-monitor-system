package com.example.temperaturemonitor.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class ObjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @OneToMany(mappedBy = "object", cascade = CascadeType.REMOVE)
    private List<TemperatureMeasurement> temperatureMeasurements;
}
