package com.aegisql.conveyor;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LazyConveyorSupplierTest {

    @Test
    public void getConveyor() {

        AssemblingConveyor<String,String,String> ac = new AssemblingConveyor<>();
        ac.setName("testLazySupplier");
        LazyConveyorSupplier<String,String,String> ls = new LazyConveyorSupplier("testLazySupplier");
        assertNotNull(ls.get());
        ls.reset();
        assertNotNull(ls.get());
        System.out.println(ls);
    }
}