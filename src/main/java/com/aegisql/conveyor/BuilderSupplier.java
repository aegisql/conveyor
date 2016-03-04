package com.aegisql.conveyor;

import java.util.function.Supplier;

public interface BuilderSupplier <T> extends Supplier<Supplier<? extends T>> {

}
