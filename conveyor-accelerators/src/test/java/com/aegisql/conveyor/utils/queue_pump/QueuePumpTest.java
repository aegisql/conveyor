package com.aegisql.conveyor.utils.queue_pump;

import com.aegisql.conveyor.consumers.result.ProductConsumer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueuePumpTest {

    @Test
    public void basicQueuePumpTest() {
        QueuePump<String> qp = new QueuePump<>();
        try {
            qp.setName("basicQueuePumpTest");
            qp.resultConsumer(ProductConsumer.of(qp).apply(str->{
                System.out.println(str);
            })).set();
            qp.part().value("Hello").place();
            qp.part().value("World").place().join();
        } finally {
            qp.stop();
        }
    }

    @Test
    public void queuePumpLoadersShouldBePinnedToPumpIdAndLabel() {
        QueuePump<String> qp = new QueuePump<>(() -> new ConcurrentLinkedQueue<>());
        try {
            assertTrue(qp.part().toString().contains("key=PUMP_ID"));
            assertTrue(qp.part().toString().contains("label=PUMP"));
            assertTrue(qp.staticPart().toString().contains("label=PUMP"));
            assertTrue(qp.build().toString().contains("key=PUMP_ID"));
            assertTrue(qp.future().toString().contains("key=PUMP_ID"));
        } finally {
            qp.stop();
        }
    }
}
