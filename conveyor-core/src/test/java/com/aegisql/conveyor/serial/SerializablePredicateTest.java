package com.aegisql.conveyor.serial;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializablePredicateTest {
    @Test
    public void functionalInterfaceTest() {
        SerializablePredicate<String> sp = a->true;
        assertTrue(sp instanceof Serializable);
        assertTrue(sp.test("this"));
    }
}