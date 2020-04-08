package com.aegisql.conveyor.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiValue {

    private final List<Object> values = new ArrayList<>();

    public MultiValue() {

    }

    public MultiValue(Object first) {
        values.add(first);
    }

    private MultiValue(MultiValue old, Object next) {
        values.addAll(old.values);
        values.add(next);
    }

    public MultiValue add(Object more) {
        return new MultiValue(this,more);
    }

    public List<Object> getValues() {
        return Collections.unmodifiableList(values);
    }

    public Object[] asArray() {
        return values.toArray();
    }

    public <T> T cast(Class<T> cls, int i) {
        return (T) values.get(i);
    }

    public Object asObject(int i) {
        return values.get(i);
    }

    public String asString(int i) {
        return (String) values.get(i);
    }

    public Integer asInteger(int i) {
        return (Integer) values.get(i);
    }

    public Long asLong(int i) {
        return (Long) values.get(i);
    }

    public Double asDouble(int i) {
        return (Double) values.get(i);
    }

    public Boolean asBoolean(int i) {
        return (Boolean) values.get(i);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MultiValue");
        sb.append(values);
        return sb.toString();
    }
}
