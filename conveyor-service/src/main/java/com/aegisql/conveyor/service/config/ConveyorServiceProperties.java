package com.aegisql.conveyor.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "conveyor.service")
public class ConveyorServiceProperties {

    private Path uploadDir = Paths.get(System.getProperty("user.home"), ".conveyor-service", "upload");

    public Path getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(Path uploadDir) {
        this.uploadDir = uploadDir;
    }
}

