package com.aegisql.conveyor.validation;

public class DuplicateValueException extends RuntimeException {
    public DuplicateValueException(String s) {
        super(s);
    }
}
