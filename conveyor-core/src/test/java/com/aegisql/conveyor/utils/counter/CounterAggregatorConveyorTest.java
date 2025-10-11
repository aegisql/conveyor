package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CounterAggregatorConveyorTest {

    @Test
    public void testCounterAggregatorConveyor() {
        LastResultReference<Integer, Map<String,Map<String,Integer>>> resultRef = new LastResultReference<>();
        LastScrapReference<Integer> scrapRef = new LastScrapReference<>();
        CounterAggregatorConveyor<Integer> c = new CounterAggregatorConveyor<>();
        c.setNamesLabel("_TASK_NAMES_");
        c.setExpectedLabelSuffix("_EXPECTED_VALUE");
        c.resultConsumer(LogResult.debug(c)).andThen(resultRef).set();
        c.scrapConsumer(LogScrap.error(c)).andThen(scrapRef).set();

        c.staticPart().label("_TASK_NAMES_").value(List.of("task1","task2")).place();
        var pl = c.part().id(1);
        pl.label("task1_EXPECTED_VALUE").value(6).place();
        pl.label("task2_EXPECTED_VALUE").value(3).place();
        pl.label("task1_EXPECTED_VALUE").value(100).place();
        pl.label("task1").value(3).place();
        pl.label("task1").value(3).place();
        pl.label("task2").value(1).place();
        pl.label("task2").value(1).place();
        pl.label("task2").value(1).place();
        c.completeAndStop().join();
        Map<String, Map<String, Integer>> map = resultRef.getCurrent();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("task1"));
        assertTrue(map.containsKey("task2"));
        System.out.println(resultRef.getCurrent());
        System.out.println(scrapRef.getCurrent().error);
    }

}