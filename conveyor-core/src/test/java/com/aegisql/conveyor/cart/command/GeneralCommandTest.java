package com.aegisql.conveyor.cart.command;

import com.aegisql.conveyor.CommandLabel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeneralCommandTest {
    @Test
    public void testGeneralCommand() {
        GeneralCommand<Integer, String> c1 = new GeneralCommand(1, "", CommandLabel.CHECK_BUILD, 0);
        GeneralCommand<Integer, String> c2 = new GeneralCommand(k->true,"",CommandLabel.CHECK_BUILD, 0,0);
        assertNotNull(c1.copy());
        assertNotNull(c2.copy());
    }
}