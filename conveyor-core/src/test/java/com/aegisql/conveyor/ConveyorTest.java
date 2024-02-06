package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConveyorTest {

    @Test
    public void getConsumerFor() {
        LabeledValueConsumer lc1 = Conveyor.getConsumerFor(null);
        LabeledValueConsumer lc2 = Conveyor.getConsumerFor(null,null);
        assertNotNull(lc1);
        assertNotNull(lc2);
        try{
            lc1.accept("","",null);
            fail("Must fail");
        } catch (Exception e) { }
        try{
            lc2.accept("","",null);
            fail("Must fail");
        } catch (Exception e) { }
    }

    @Test
    public void unRegister() {
        assertThrows(RuntimeException.class,()->Conveyor.unRegister("something wrong :"));
    }
}