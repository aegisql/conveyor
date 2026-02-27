package com.aegisql.conveyor.utils.jdbc;

public class JdbcExecutionException extends RuntimeException {

    public JdbcExecutionException(String message) {
        super(message);
    }

    public JdbcExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
