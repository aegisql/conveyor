package com.aegisql.conveyor.meta;

import com.aegisql.conveyor.BuilderSupplier;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConveyorMetaInfo <K,L,OUT> {
    private final Class<K> keyType;
    private final Class<L> labelType;
    private final Class<OUT> productType;
    private final Map<L, Set<Class<?>>> supportedValueTypes;
    private final Set<L> labels;
    private final BuilderSupplier<OUT> builderSupplier;
    private final Map<String, Predicate<String>> matchers = new LinkedHashMap<>();
    private final boolean hasMatchers;


    public ConveyorMetaInfo(Class<K> keyType, Class<L> labelType, Class<OUT> productType, Map<L, Set<Class<?>>> supportedValueTypes, Collection<L> labels, BuilderSupplier<OUT> builderSupplier) {
        this.keyType = keyType;
        this.labelType = labelType;
        this.productType = productType;
        HashMap<L, Set<Class<?>>> buff = new HashMap<>();
        supportedValueTypes.forEach((l,t)->{
            buff.put(l,Collections.unmodifiableSet(new HashSet<>(t)));
        });
        this.supportedValueTypes = Collections.unmodifiableMap(buff);
        this.labels = Collections.unmodifiableSet(new LinkedHashSet<>(labels));
        this.builderSupplier = builderSupplier;
        this.hasMatchers = String.class == labelType;
        if(hasMatchers) {
            labels.stream().filter(Objects::nonNull).map(Object::toString).forEach(l->matchers.put(l,Pattern.compile(l).asMatchPredicate()));
        }
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
        if(hasMatchers) {
            for(var es: matchers.entrySet()) {
                if(es.getValue().test(label.toString())) {
                    return supportedValueTypes.get(es.getKey());
                }
            }
            return null;
        } else {
            return supportedValueTypes.get(label);
        }
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
    public String generic() {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(this.getKeyType().getSimpleName()).append(",");
        sb.append(this.getLabelType().getSimpleName()).append(",");
        sb.append(this.getProductType().getSimpleName()).append(">");
        return sb.toString();
    }

}
