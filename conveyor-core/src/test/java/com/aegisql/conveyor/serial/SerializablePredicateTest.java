package com.aegisql.conveyor.serial;

import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

public class SerializablePredicateTest {
    @Test
    public void functionalInterfaceTest() {
        SerializablePredicate<String> sp = a->true;
        assertTrue(sp instanceof Serializable);
        assertTrue(sp.test("this"));
    }
}