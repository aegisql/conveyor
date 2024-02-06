package com.aegisql.conveyor.serial;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializableBiPredicateTest {

    @Test
    public void functionalInterfaceTest() {
        SerializableBiPredicate<String> sbp = (a,b)->true;
        assertTrue(sbp instanceof Serializable);
        assertTrue(sbp.test("this","that"));
    }

    @Test
    public void forKey() {
        SerializableBiPredicate<Object> biPredicate = SerializableBiPredicate.forKey("test", val -> val.equals("TEST"));
        assertTrue(biPredicate.test("test","TEST"));
        assertFalse(biPredicate.test("TEST","test"));    }

    @Test
    public void testMap() {
        SerializableBiPredicate<String> biPredicate = SerializableBiPredicate.forKey("test", val -> val.equals("TEST"));
        Map<String,String> m = new HashMap<>();
        m.put("test","TEST");
        assertTrue(biPredicate.testMap(m));
        m.put("test","REST");
        assertFalse(biPredicate.testMap(m));
    }
}