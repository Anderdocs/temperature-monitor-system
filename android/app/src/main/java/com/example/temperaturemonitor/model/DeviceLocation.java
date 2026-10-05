package com.example.temperaturemonitor.model;

import com.google.gson.annotations.SerializedName;

public class DeviceLocation {
    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("location")
    private String location;

    public DeviceLocation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}