package com.aegisql.conveyor.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "conveyor.service")
public class ConveyorServiceProperties {

    private Path uploadDir = Paths.get(System.getProperty("user.home"), ".conveyor-service", "upload");
    private boolean uploadEnable = true;
    private boolean oauth2LoginEnable = true;

    public Path getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(Path uploadDir) {
        this.uploadDir = uploadDir;
    }

    public boolean isUploadEnable() {
        return uploadEnable;
    }

    public void setUploadEnable(boolean uploadEnable) {
        this.uploadEnable = uploadEnable;
    }

    public boolean isOauth2LoginEnable() {
        return oauth2LoginEnable;
    }

    public void setOauth2LoginEnable(boolean oauth2LoginEnable) {
        this.oauth2LoginEnable = oauth2LoginEnable;
    }
}
