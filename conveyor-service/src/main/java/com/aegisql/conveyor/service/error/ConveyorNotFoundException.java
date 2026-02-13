package com.aegisql.conveyor.service.error;

public class ConveyorNotFoundException extends RuntimeException {
    public ConveyorNotFoundException(String name) {
        super("Conveyor not found: " + name);
    }
}
