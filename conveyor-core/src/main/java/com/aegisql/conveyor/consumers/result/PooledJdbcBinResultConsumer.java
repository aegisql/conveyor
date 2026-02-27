package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.utils.jdbc.JdbcOperationConfig;
import com.aegisql.conveyor.utils.jdbc.PooledConnectionJdbcOperationExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class PooledJdbcBinResultConsumer<K, OUT> extends AbstractJdbcResultConsumer<K, OUT, ProductBin<K, OUT>> {

    @SafeVarargs
    public PooledJdbcBinResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<ProductBin<K, OUT>, ?>... parameterMappers
    ) {
        this(connectionSupplier, sql, List.of(parameterMappers));
    }

    public PooledJdbcBinResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            List<Function<ProductBin<K, OUT>, ?>> parameterMappers
    ) {
        super(
                Function.identity(),
                new PooledConnectionJdbcOperationExecutor<>(
                        new JdbcOperationConfig<>(connectionSupplier, sql, parameterMappers)
                )
        );
    }

    @SafeVarargs
    public static <K, OUT> PooledJdbcBinResultConsumer<K, OUT> of(
            Conveyor<K, ?, OUT> conveyor,
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<ProductBin<K, OUT>, ?>... parameterMappers
    ) {
        return new PooledJdbcBinResultConsumer<>(connectionSupplier, sql, parameterMappers);
    }
}
