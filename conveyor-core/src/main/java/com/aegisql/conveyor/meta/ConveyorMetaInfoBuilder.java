package com.aegisql.conveyor.meta;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.*;
import java.util.function.Supplier;

public class ConveyorMetaInfoBuilder<K,L,OUT> implements Supplier<ConveyorMetaInfo<K,L,OUT>> {

    private final Class<K> keyType;
    private final Class<L> labelType;
    private final Class<OUT> productType;
    private final HashMap<L, Set<Class<?>>> supportedValueTypes = new HashMap<>();
    private final Set<L> labels = new HashSet<>();
    private final BuilderSupplier<OUT> builderSupplier;

    private ConveyorMetaInfoBuilder(Class<K> keyType, Class<L> labelType, Class<OUT> productType, BuilderSupplier<OUT> builderSupplier, HashMap<L, Set<Class<?>>> supportedValueTypes, Collection<L> labels) {
        this.keyType = keyType;
        this.labelType = labelType;
        this.productType = productType;
        this.builderSupplier = builderSupplier;
        this.supportedValueTypes.putAll(supportedValueTypes);
        this.labels.addAll(labels);
    }

    public static <K,L,OUT> ConveyorMetaInfoBuilder<K,L,OUT> of(Conveyor<K,L,OUT> c) {
        return new ConveyorMetaInfoBuilder<>(null,null,null,null,new HashMap<>(),new HashSet<>());
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> keyType(Class<K> k) {
        return new ConveyorMetaInfoBuilder<>(k,labelType,productType,builderSupplier,supportedValueTypes,labels);
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> labelType(Class<L> l) {
        return new ConveyorMetaInfoBuilder<>(keyType,l,productType,builderSupplier,supportedValueTypes,labels);
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> productType(Class<OUT> p) {
        return new ConveyorMetaInfoBuilder<>(keyType,labelType,p,builderSupplier,supportedValueTypes,labels);
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> builderSupplier(BuilderSupplier<OUT> s) {
        return new ConveyorMetaInfoBuilder<>(keyType,labelType,productType,s,supportedValueTypes,labels);
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> labels(L l, L... more) {
        Set<L> labels = new HashSet<>(this.labels);
        labels.add(l);
        if(more != null) {
            labels.addAll(Arrays.asList(more));
        }
        return new ConveyorMetaInfoBuilder<>(keyType,labelType,productType,builderSupplier,supportedValueTypes,labels);
    }

    public ConveyorMetaInfoBuilder<K,L,OUT> supportedTypes(L l, Class<?> cl, Class<?>... more) {
        HashMap<L, Set<Class<?>>> supportedValueTypes = new HashMap<>(this.supportedValueTypes);
        Set<Class<?>> classes = supportedValueTypes.computeIfAbsent(l, lbl -> new HashSet<>());
        classes.add(cl);
        if(more != null) {
            classes.addAll(Arrays.asList(more));
        }
        return new ConveyorMetaInfoBuilder<>(keyType,labelType,productType,builderSupplier,supportedValueTypes,labels);
    }

    @Override
    public ConveyorMetaInfo<K, L, OUT> get() {
        Objects.requireNonNull(keyType,"Define the surrogate key type");
        Objects.requireNonNull(labelType,"Define the label type");
        Objects.requireNonNull(productType,"Define the product type");
        Set<L> lSet = supportedValueTypes.keySet();
        if( ! labels.containsAll(lSet) || ! lSet.containsAll(labels)) {
            throw new ConveyorRuntimeException("Set of labels do not match. labels:"+labels+"; supported types:"+supportedValueTypes);
        };
        return new ConveyorMetaInfo<>(keyType,labelType,productType,supportedValueTypes,labels,builderSupplier);
    }
}
