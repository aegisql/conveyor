package com.aegisql.conveyor;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.Assert.assertNotNull;

public class BuilderAndFutureSupplierTest {

    @Test
    public void getTest() {
        BuilderAndFutureSupplier<String> s = new BuilderAndFutureSupplier<>(new BuilderSupplier<String>() {
            @Override
            public Supplier<String> get() {
                return ()->"TEST";
            }
        }, new CompletableFuture<>());
        assertNotNull(s.get());
        assertNotNull(s.getFuture());
        assertNotNull(s.toString());
    }

}