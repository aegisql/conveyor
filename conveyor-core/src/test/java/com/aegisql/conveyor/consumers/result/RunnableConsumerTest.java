package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import org.junit.Test;

public class RunnableConsumerTest {

    @Test
    public void runnableTest() {
        RunnableConsumer<Integer> rc = new RunnableConsumer<>();
        ProductBin<Integer,Runnable> bin = new ProductBin<>(null,
                1, new Runnable() {
            @Override
            public void run() {
                System.out.println("RAN!");
            }
        },0, Status.CANCELED,null,null
        );
        rc.accept(bin);
    }

}