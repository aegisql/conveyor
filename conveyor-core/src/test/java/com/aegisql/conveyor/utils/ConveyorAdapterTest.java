package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConveyorAdapterTest {

    class TestAdapterConveyor extends ConveyorAdapter<Integer,String,String>{

        /**
         * Constructs a AssemblingConveyorAdapter with the specified inner conveyor.
         *
         */
        public TestAdapterConveyor() {
            super(new AssemblingConveyor<>());
        }
    }

    @Test
    public void instanceTest() {
        TestAdapterConveyor c = new TestAdapterConveyor();
        c.setName("TestAdapterConveyor");
        c.stop();
    }

}