package com.aegisql.conveyor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ReadinessTesterTest {

    class B implements Supplier<String> {

        @Override
        public String get() {
            return "TEST";
        }
    }

    class BT extends B implements Testing {
        @Override
        public boolean test() {
            return true;
        }
    }

    @Test
    public void basicResinessTesterTest() {
        ReadinessTester rt = new ReadinessTester();
        rt = rt.andThen(sup->true);
        assertNotNull(rt);
        assertTrue(rt.test(null,null));
        assertTrue(rt.immediatelyReady().test(null,null));
        assertFalse(rt.neverReady().test(null,null));
        assertFalse(rt.accepted("l",1).test(new State(
                1,0,0,0,0,0,
                new HashMap<>(), new ArrayList<>()
        ),null));
    }

    @Test
    public void fromBuilderTest() {
        ReadinessTester rt = new ReadinessTester();
        rt = rt.usingBuilderTest(BT.class);
        assertTrue(rt.test(null,new BT()));
    }

    @Test(expected = ClassCastException.class)
    public void fromBuilderTestFail() {
        ReadinessTester rt = new ReadinessTester();
        rt.usingBuilderTest(B.class);
    }

}