package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.List;

public class ConveyorMetaInfo <K,L,OUT> {

    private final Conveyor<K,L,OUT> conveyor;
    private final Class<K> keyType;
    private final Class<L> labelType;
    private final Class<OUT> productType;
    private final HashMap<L,List<Class<?>>> supportedValueTypes;
    private final List<L> labels;
    private final BuilderSupplier<OUT> builderSupplier;

    public ConveyorMetaInfo(Conveyor<K, L, OUT> conveyor, Class<K> keyType, Class<L> labelType, Class<OUT> productType, HashMap<L, List<Class<?>>> supportedValueTypes, List<L> labels, BuilderSupplier<OUT> builderSupplier) {
        this.conveyor = conveyor;
        this.keyType = keyType;
        this.labelType = labelType;
        this.productType = productType;
        this.supportedValueTypes = supportedValueTypes;
        this.labels = labels;
        this.builderSupplier = builderSupplier;
    }

    public Conveyor<K,L,OUT> getConveyor(){
        return conveyor;
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
    public List<Class<?>> getSupportedValueTypes(L label) {
        return supportedValueTypes.get(label);
    }
    public List<L> getLabels() {
        return labels;
    }
    public BuilderSupplier<OUT> builderSupplierFactory() {
        return builderSupplier;
    }
}
