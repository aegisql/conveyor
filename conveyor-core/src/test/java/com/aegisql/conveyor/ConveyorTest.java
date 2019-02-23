package com.aegisql.conveyor;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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

    @Test(expected = RuntimeException.class)
    public void unRegister() {
        Conveyor.unRegister("something wrong :");
    }
}