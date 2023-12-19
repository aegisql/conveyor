package com.aegisql.conveyor.meta;

import com.aegisql.conveyor.BuilderSupplier;

import java.util.*;

public class ConveyorMetaInfo <K,L,OUT> {
    private final Class<K> keyType;
    private final Class<L> labelType;
    private final Class<OUT> productType;
    private final Map<L, Set<Class<?>>> supportedValueTypes;
    private final Set<L> labels;
    private final BuilderSupplier<OUT> builderSupplier;

    public ConveyorMetaInfo(Class<K> keyType, Class<L> labelType, Class<OUT> productType, Map<L, Set<Class<?>>> supportedValueTypes, Collection<L> labels, BuilderSupplier<OUT> builderSupplier) {
        this.keyType = keyType;
        this.labelType = labelType;
        this.productType = productType;
        HashMap<L, Set<Class<?>>> buff = new HashMap<>();
        supportedValueTypes.forEach((l,t)->{
            buff.put(l,Collections.unmodifiableSet(new HashSet<>(t)));
        });
        this.supportedValueTypes = Collections.unmodifiableMap(buff);
        this.labels = Collections.unmodifiableSet(new HashSet<>(labels));
        this.builderSupplier = builderSupplier;
    }

    public Class<K> getKeyType() {
        return keyType;
    }
    public Class<L> getLabelType() {
        return labelType;
    }
    public Class<OUT> getProductType() {
        return productType;
    }
    public Set<Class<?>> getSupportedValueTypes(L label) {
        return supportedValueTypes.get(label);
    }
    public Set<L> getLabels() {
        return labels;
    }
    public BuilderSupplier<OUT> builderSupplierFactory() {
        return builderSupplier;
    }

    @Override
    public String toString() {
        return "ConveyorMetaInfo{" +
                "keyType=" + keyType +
                ", labelType=" + labelType +
                ", productType=" + productType +
                ", supportedValueTypes=" + supportedValueTypes +
                ", labels=" + labels +
                '}';
    }
}
