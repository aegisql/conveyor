package com.aegisql.conveyor.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConveyorServiceApplicationTest {

    @Test
    void mainStartsInNonWebDemoMode() {
        assertThatCode(() -> ConveyorServiceApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=demo"
        })).doesNotThrowAnyException();
    }

    @Test
    void constructorIsInstantiable() {
        assertThatCode(ConveyorServiceApplication::new).doesNotThrowAnyException();
    }
}
