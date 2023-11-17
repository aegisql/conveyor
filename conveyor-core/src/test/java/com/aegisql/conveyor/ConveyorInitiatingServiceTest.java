package com.aegisql.conveyor;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderEvents;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

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

        assertEquals(Integer.class,conveyorInitiatingService.getKeyType());
        assertEquals(UserBuilderEvents.class,conveyorInitiatingService.getLabelType());

        Object o = conveyorInitiatingService.builderSupplierFactory().get();
        assertNotNull(o);
        assertEquals(UserBuilder.class,o.getClass());
    }

}