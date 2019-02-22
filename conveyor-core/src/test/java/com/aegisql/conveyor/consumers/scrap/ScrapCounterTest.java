package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.AssemblingConveyor;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScrapCounterTest {

    @Test
    public void testOf() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>();
        ScrapCounter<Integer> sc1 = ScrapCounter.of(ac);
        ScrapCounter<Integer> sc2 = ScrapCounter.of(ac,x->true);
        sc1.accept(null);
        assertEquals(1,sc1.get());
        System.out.println(sc1);
    }

}