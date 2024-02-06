package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AcknowledgeStatusTest {

    @Test
    public void testAckStatus() {
        Map<String, Object> pm = new HashMap<>();
        pm.put("test", "TEST");
        AcknowledgeStatus<Integer> as = new AcknowledgeStatus<>(1, Status.READY, pm);
        System.out.println(as);
        assertNotNull(as.getProperties());
        assertEquals(Integer.valueOf(1),as.getKey());
        assertEquals(Status.READY,as.getStatus());
        assertEquals("TEST",as.getProperty("test",String.class));
    }

}