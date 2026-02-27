package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.utils.jdbc.JdbcOperationConfig;
import com.aegisql.conveyor.utils.jdbc.PooledConnectionJdbcOperationExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class PooledJdbcProductResultConsumer<K, OUT> extends AbstractJdbcResultConsumer<K, OUT, OUT> {

    @SafeVarargs
    public PooledJdbcProductResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<OUT, ?>... parameterMappers
    ) {
        this(connectionSupplier, sql, List.of(parameterMappers));
    }

    public PooledJdbcProductResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            List<Function<OUT, ?>> parameterMappers
    ) {
        super(
                bin -> bin.product,
                new PooledConnectionJdbcOperationExecutor<>(
                        new JdbcOperationConfig<>(connectionSupplier, sql, parameterMappers)
                )
        );
    }

    @SafeVarargs
    public static <K, OUT> PooledJdbcProductResultConsumer<K, OUT> of(
            Conveyor<K, ?, OUT> conveyor,
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<OUT, ?>... parameterMappers
    ) {
        return new PooledJdbcProductResultConsumer<>(connectionSupplier, sql, parameterMappers);
    }
}
