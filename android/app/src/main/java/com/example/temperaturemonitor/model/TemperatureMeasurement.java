package com.example.temperaturemonitor.model;

import com.google.gson.annotations.SerializedName;

public class TemperatureMeasurement {
    @SerializedName("id")
    private Long id;
    @SerializedName("temperature")
    private Double temperature;
    @SerializedName("humidity")
    private Double humidity;
    @SerializedName("timestamp")
    private String timestamp;
    @SerializedName("object")
    private DeviceLocation objectEntity;

    public TemperatureMeasurement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public DeviceLocation getObjectEntity() { return objectEntity; }
    public void setObjectEntity(DeviceLocation objectEntity) { this.objectEntity = objectEntity; }
}