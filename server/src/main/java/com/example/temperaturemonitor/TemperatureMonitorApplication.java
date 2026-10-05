package com.example.temperaturemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.example.temperaturemonitor.model")
public class TemperatureMonitorApplication {
	public static void main(String[] args) {
		SpringApplication.run(TemperatureMonitorApplication.class, args);
	}
}

