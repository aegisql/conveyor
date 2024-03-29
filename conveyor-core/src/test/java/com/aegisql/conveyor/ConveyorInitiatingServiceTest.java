package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ConveyorInitiatingServiceTest {

    @Test
    public void loadServiceTest() {
        Set<String> loadedConveyorNames = Conveyor.getLoadedConveyorNames();
        assertNotNull(loadedConveyorNames);
        assertTrue(loadedConveyorNames.contains("SimpleTestServiceConveyor"));
        Conveyor simpleTestServiceConveyor = Conveyor.byName("SimpleTestServiceConveyor");
        assertNotNull(simpleTestServiceConveyor);
        List<ConveyorInitiatingService> loadedConveyorServices = Conveyor.getLoadedConveyorServices();
        assertNotNull(loadedConveyorServices);
        assertTrue(loadedConveyorServices.size()>0);
        ConveyorInitiatingService conveyorInitiatingService = loadedConveyorServices.get(0);
        assertNotNull(conveyorInitiatingService);
        assertEquals(simpleTestServiceConveyor,conveyorInitiatingService.getConveyor());
    }

}