package com.aegisql.conveyor.utils.queue_pump;

import com.aegisql.conveyor.consumers.result.ProductConsumer;
import org.junit.jupiter.api.Test;

public class QueuePumpTest {

    @Test
    public void basicQueuePumpTest() {
        QueuePump<String> qp = new QueuePump<>();
        qp.setName("basicQueuePumpTest");
        qp.resultConsumer(ProductConsumer.of(qp).apply(str->{
            System.out.println(str);
        })).set();
        qp.part().value("Hello").place();
        qp.part().value("World").place().join();
    }


}