package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.utils.jdbc.JdbcOperationConfig;
import com.aegisql.conveyor.utils.jdbc.SharedConnectionJdbcOperationExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class JdbcBinResultConsumer<K, OUT> extends AbstractJdbcResultConsumer<K, OUT, ProductBin<K, OUT>> {

    @SafeVarargs
    public JdbcBinResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<ProductBin<K, OUT>, ?>... parameterMappers
    ) {
        this(connectionSupplier, sql, List.of(parameterMappers));
    }

    public JdbcBinResultConsumer(
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            List<Function<ProductBin<K, OUT>, ?>> parameterMappers
    ) {
        super(
                Function.identity(),
                new SharedConnectionJdbcOperationExecutor<>(
                        new JdbcOperationConfig<>(connectionSupplier, sql, parameterMappers)
                )
        );
    }

    @SafeVarargs
    public static <K, OUT> JdbcBinResultConsumer<K, OUT> of(
            Conveyor<K, ?, OUT> conveyor,
            Supplier<? extends Connection> connectionSupplier,
            String sql,
            Function<ProductBin<K, OUT>, ?>... parameterMappers
    ) {
        return new JdbcBinResultConsumer<>(connectionSupplier, sql, parameterMappers);
    }
}
