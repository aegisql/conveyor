package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The type Product consumer.
 *
 * @param <T> the type parameter
 */
public class ProductConsumer <T> implements ResultConsumer<Object,T>{

    private final Consumer<T> consumer;

    /**
     * Of function.
     *
     * @param <K>      the type parameter
     * @param <T>      the type parameter
     * @param conveyor the conveyor
     * @return the function
     */
    public static <K,T> Function<Consumer<T>, ResultConsumer<K,T>> of(Conveyor<K,?,T> conveyor) {
        return consumer -> (ResultConsumer<K, T>) new ProductConsumer<T>(consumer);
    }

    private ProductConsumer(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(ProductBin<Object, T> bin) {
        consumer.accept(bin.product);
    }
}
