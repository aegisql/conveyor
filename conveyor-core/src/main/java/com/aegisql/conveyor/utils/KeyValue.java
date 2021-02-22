package com.aegisql.conveyor.utils;

import java.io.Serializable;
import java.util.Objects;

public class KeyValue <K extends Serializable,V extends Serializable> implements Serializable {
    public final K key;
    public final V value;

    public KeyValue() {
        this(null,null);
    }

    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public KeyValue<K,V> key(K key) {
        return new KeyValue<>(key, this.value);
    }

    public KeyValue<K,V> value(V value) {
        return new KeyValue<>(this.key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValue<?, ?> keyValue = (KeyValue<?, ?>) o;
        return Objects.equals(key, keyValue.key) && Objects.equals(value, keyValue.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KeyValue{'");
        sb.append(key);
        sb.append("'->'").append(value);
        sb.append("'}");
        return sb.toString();
    }
}
