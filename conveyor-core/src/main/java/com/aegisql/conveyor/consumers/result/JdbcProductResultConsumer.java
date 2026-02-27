package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.utils.jdbc.JdbcOperationConfig;
import com.aegisql.conveyor.utils.jdbc.SharedConnectionJdbcOperationExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class JdbcProductResultConsumer<K, OUT> extends AbstractJdbcResultConsumer<K, OUT, OUT> {

    @SafeVarargs
    public JdbcProductResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<OUT, ?>... parameterMappers
    ) {
        this(connectionSupplier, sql, List.of(parameterMappers));
    }

    public JdbcProductResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            List<Function<OUT, ?>> parameterMappers
    ) {
        super(
                bin -> bin.product,
                new SharedConnectionJdbcOperationExecutor<>(
                        new JdbcOperationConfig<>(connectionSupplier, sql, parameterMappers)
                )
        );
    }

    @SafeVarargs
    public static <K, OUT> JdbcProductResultConsumer<K, OUT> of(
            Conveyor<K, ?, OUT> conveyor,
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<OUT, ?>... parameterMappers
    ) {
        return new JdbcProductResultConsumer<>(connectionSupplier, sql, parameterMappers);
    }
}
