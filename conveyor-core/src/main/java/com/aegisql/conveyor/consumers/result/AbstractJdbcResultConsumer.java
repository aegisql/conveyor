package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.utils.jdbc.JdbcOperationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

abstract class AbstractJdbcResultConsumer<K, OUT, S> implements ResultConsumer<K, OUT> {

    private final Function<ProductBin<K, OUT>, S> sourceSupplier;
    private final JdbcOperationExecutor<S> jdbcExecutor;

    protected AbstractJdbcResultConsumer(
            Function<ProductBin<K, OUT>, S> sourceSupplier,
            JdbcOperationExecutor<S> jdbcExecutor
    ) {
        this.sourceSupplier = Objects.requireNonNull(sourceSupplier, "Source supplier must be provided");
        this.jdbcExecutor = Objects.requireNonNull(jdbcExecutor, "JDBC executor must be provided");
    }

    @Override
    public void accept(ProductBin<K, OUT> bin) {
        Objects.requireNonNull(bin, "Product bin must be provided");
        List<S> sources = resolveSources(bin);
        if (sources.isEmpty()) {
            return;
        }
        if (sources.size() == 1) {
            jdbcExecutor.execute(sources.getFirst());
        } else {
            jdbcExecutor.executeBatch(sources);
        }
    }

    private List<S> resolveSources(ProductBin<K, OUT> bin) {
        S suppliedSource = sourceSupplier.apply(bin);

        if (!(bin.product instanceof Iterable<?> iterable)) {
            return List.of(suppliedSource);
        }

        ArrayList<S> sources = new ArrayList<>();
        if (suppliedSource instanceof ProductBin<?, ?>) {
            for (Object nextProduct : iterable) {
                sources.add(repackageBin(bin, nextProduct));
            }
        } else {
            for (Object nextProduct : iterable) {
                sources.add(cast(nextProduct));
            }
        }
        return sources;
    }

    @SuppressWarnings({"unchecked"})
    private S repackageBin(ProductBin<K, OUT> original, Object nextProduct) {
        return (S) original.withNewValue(nextProduct);
    }

    @SuppressWarnings("unchecked")
    private S cast(Object value) {
        return (S) value;
    }

}
