package com.example.temperaturemonitor.model;

import java.util.Objects;

public class DiscoveredDevice {
    private String serviceName;
    private String serviceType;
    private String hostAddress;
    private int port;

    public DiscoveredDevice(String serviceName, String serviceType, String hostAddress, int port) {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.hostAddress = hostAddress;
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return serviceName + " (" + hostAddress + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveredDevice that = (DiscoveredDevice) o;
        return port == that.port &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(serviceType, that.serviceType) &&
                Objects.equals(hostAddress, that.hostAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceType, hostAddress, port);
    }
}
