package com.aegisql.conveyor.utils.jdbc;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public record JdbcOperationConfig<S>(
        Supplier<? extends Connection> connectionSupplier,
        String sql,
        List<Function<S, ?>> parameterMappers
) {
    public JdbcOperationConfig {
        connectionSupplier = Objects.requireNonNull(connectionSupplier, "Connection supplier must be provided");
        sql = Objects.requireNonNull(sql, "SQL must be provided");
        parameterMappers = List.copyOf(Objects.requireNonNull(parameterMappers, "Parameter mappers must be provided"));
    }
}
