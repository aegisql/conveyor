package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Acknowledge;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IgnoreResultTest {

    @Test
    void shouldAcceptAndIgnoreProductBin() {
        IgnoreResult<Integer, String> singleton = IgnoreResult.INSTANCE;
        assertNotNull(singleton);

        IgnoreResult<Integer, String> fromFactory = IgnoreResult.of((Conveyor<Integer, Object, String>) null);
        assertNotNull(fromFactory);

        ProductBin<Integer, String> productBin = new ProductBin<>(
                null,
                1,
                "payload",
                0L,
                Status.READY,
                Map.of("source", "test"),
                new Acknowledge() {
                    @Override
                    public void ack() {
                    }

                    @Override
                    public boolean isAcknowledged() {
                        return false;
                    }
                }
        );

        singleton.accept(productBin);
        fromFactory.accept(productBin);
    }
}
