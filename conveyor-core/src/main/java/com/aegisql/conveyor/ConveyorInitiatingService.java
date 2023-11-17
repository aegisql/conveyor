package com.aegisql.conveyor;

import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    Conveyor<K,L,OUT> getConveyor();
    Class<K> getKeyType();
    Class<L> getLabelType();
    Class<OUT> getProductType();
    List<Class<?>> getSupportedValueTypes(L label);
    List<L> getLabels();
    default BuilderSupplier<OUT> builderSupplierFactory() {
        return null;
    }
}
