package com.aegisql.conveyor.service.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConveyorServicePropertiesTest {

    @Test
    void gettersAndSettersRoundTripAllFields() {
        ConveyorServiceProperties properties = new ConveyorServiceProperties();
        Path uploadDir = Path.of("/tmp/conveyor-upload-test");

        properties.setUploadDir(uploadDir);
        properties.setUploadEnable(false);
        properties.setOauth2LoginEnable(false);
        properties.setOauth2ResourceServerEnable(false);

        assertThat(properties.getUploadDir()).isEqualTo(uploadDir);
        assertThat(properties.isUploadEnable()).isFalse();
        assertThat(properties.isOauth2LoginEnable()).isFalse();
        assertThat(properties.isOauth2ResourceServerEnable()).isFalse();

        properties.setUploadEnable(true);
        properties.setOauth2LoginEnable(true);
        properties.setOauth2ResourceServerEnable(true);
        assertThat(properties.isUploadEnable()).isTrue();
        assertThat(properties.isOauth2LoginEnable()).isTrue();
        assertThat(properties.isOauth2ResourceServerEnable()).isTrue();
    }
}
