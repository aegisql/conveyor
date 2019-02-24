package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.consumers.result.IgnoreResult;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ResultConsumerCartTest {

    @Test
    public void copyTest() {
        ResultConsumerCart<Integer,?,String> rcc = new ResultConsumerCart<>(1,new IgnoreResult<>(),0,0,0);
        assertNotNull(rcc.copy());
    }

}