package com.aegisql.conveyor.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonCompatibilityConfigTest {

    @Test
    void objectMapperBeanFactoryReturnsConfiguredMapper() {
        ObjectMapper mapper = new JacksonCompatibilityConfig().objectMapper();
        String json = mapper.createObjectNode().put("ok", true).toString();

        assertThat(mapper).isNotNull();
        assertThat(json).contains("\"ok\":true");
    }
}
