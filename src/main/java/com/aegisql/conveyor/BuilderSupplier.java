package com.aegisql.conveyor;

import java.util.function.Supplier;

/**
 * The Interface BuilderSupplier.
 *
 * @param <T> the generic type
 */
public interface BuilderSupplier <T> extends Supplier<Supplier<? extends T>> {

}
