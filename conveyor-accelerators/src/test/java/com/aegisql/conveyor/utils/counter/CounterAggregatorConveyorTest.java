package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CounterAggregatorConveyorTest {

    static class ExposedCounterAggregatorConveyor<K> extends CounterAggregatorConveyor<K> {
        ExposedCounterAggregatorConveyor() {
            super();
        }

        ExposedCounterAggregatorConveyor(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier) {
            super(cartQueueSupplier);
        }

        ConveyorMetaInfo<K, String, Map<String, Map<String, Integer>>> exposedMetaInfo(Class<K> keyType) {
            return getMetaInfoBuilder()
                    .keyType(keyType)
                    .builderSupplier(CountersAggregator::new)
                    .get();
        }

        static Class<Map<String, Map<String, Integer>>> exposedProductMapType() {
            return productMapType();
        }
    }

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

    @Test
    public void testCustomQueueConstructorAndMetaInfoBuilder() {
        ExposedCounterAggregatorConveyor<Integer> c =
                new ExposedCounterAggregatorConveyor<>(() -> new PriorityBlockingQueue<Cart<Integer, Object, Object>>());
        try {
            ConveyorMetaInfo<Integer, String, Map<String, Map<String, Integer>>> defaultMeta = c.exposedMetaInfo(Integer.class);
            assertTrue(defaultMeta.getLabels().containsAll(Set.of("_NAMES_", ".*_EXPECTED", ".*")));
            assertEquals(Set.of(List.class), defaultMeta.getSupportedValueTypes("_NAMES_"));
            assertEquals(Set.of(Integer.class), defaultMeta.getSupportedValueTypes("x_EXPECTED"));
            assertEquals(ExposedCounterAggregatorConveyor.exposedProductMapType(), defaultMeta.getProductType());

            c.setNamesLabel("_TASK_NAMES_");
            c.setExpectedLabelSuffix("_EXPECTED_VALUE");
            ConveyorMetaInfo<Integer, String, Map<String, Map<String, Integer>>> customMeta = c.exposedMetaInfo(Integer.class);
            assertTrue(customMeta.getLabels().containsAll(Set.of("_TASK_NAMES_", ".*_EXPECTED_VALUE", ".*")));
            assertEquals(Set.of(List.class), customMeta.getSupportedValueTypes("_TASK_NAMES_"));
            assertEquals(Set.of(Integer.class), customMeta.getSupportedValueTypes("task_EXPECTED_VALUE"));
        } finally {
            c.stop();
        }
    }

}
