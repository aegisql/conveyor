package com.aegisql.conveyor.service;

import com.aegisql.conveyor.service.config.ConveyorServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConveyorServiceProperties.class)
public class ConveyorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConveyorServiceApplication.class, args);
    }
}
