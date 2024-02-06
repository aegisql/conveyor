package com.aegisql.conveyor.consumers.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultCounterTest {

    @Test
    public void counterTest() {
        ResultConsumer rc = new ResultCounter();
        rc.accept(ResultConsumerTest.getProductBin(1,"test"));
        System.out.println(rc);
        assertEquals(1,((ResultCounter) rc).get());
    }

}