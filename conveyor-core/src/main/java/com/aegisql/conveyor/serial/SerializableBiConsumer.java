package com.aegisql.conveyor.serial;
import java.io.Serializable;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface SerializableBiConsumer<T,U> extends BiConsumer<T, U>, Serializable {

}
