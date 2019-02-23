package com.aegisql.conveyor.cart;

import org.junit.Test;

import java.util.HashMap;

public class ShoppingCartTest {
    @Test
    public void creationTest() {
        Cart<Integer,String,String> c = new ShoppingCart(1, "test", "label", 0, 0, new HashMap<>(), LoadType.PART, 0, true);
        c.addProperty("test","val");
        c.clearProperty("test");
    }
}