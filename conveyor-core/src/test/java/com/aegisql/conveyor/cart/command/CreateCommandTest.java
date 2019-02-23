package com.aegisql.conveyor.cart.command;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CreateCommandTest {
    @Test
    public void createTest() {
        CreateCommand<Integer,String> cc = new CreateCommand<>(1,0,0);
        assertNotNull(cc.copy());
    }
}