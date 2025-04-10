package com.aegisql.conveyor.poc;

import org.junit.jupiter.api.Test;

public class LinkedHashMapAccessOrderTest {

    @Test
    public void testAccessOrder() {
        // Create a LinkedHashMap with access order
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>(16, 0.75f, true);

        // Add some entries
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        System.out.println(map.keySet());

        // Access some entries
        map.get("key1");
        map.get("key2");

        // Print the order of keys
        System.out.println(map.keySet());
        map.get("key3");
        System.out.println(map.keySet());
    }

}
