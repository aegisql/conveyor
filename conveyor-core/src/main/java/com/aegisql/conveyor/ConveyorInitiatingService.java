package com.aegisql.conveyor;

import java.util.List;

/**
 * The interface Conveyor initiating service.
 * To activate the service create a file
 * META-INF/services/com.aegisql.conveyor.ConveyorInitiatingService
 * file should contain a fully qualified name of the conveyor
 * initializing service interface implementation(s) per each line
 * Use one initializer per conveyor
 */
public interface ConveyorInitiatingService<K,L,OUT> {
    default String getName() {
        return getConveyor().getName();
    }
    Conveyor<K,L,OUT> getConveyor();
    Class<K> getKeyType();
    Class<L> getLabelType();
    List<Class<?>> getSupportedValueTypes(L label);
    default BuilderSupplier<OUT> useBuilderSupplier() {
        return null;
    }
}
