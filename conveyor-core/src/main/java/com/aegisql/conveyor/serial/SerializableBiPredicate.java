package com.aegisql.conveyor.serial;

import java.io.Serializable;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@FunctionalInterface
public interface SerializableBiPredicate<T> extends BiPredicate<String,T>, Serializable {

    SerializableBiPredicate ANY = (str,val)->true;

    static <T> SerializableBiPredicate<T> forKey(final String key, Predicate<T> filter){
        return (str,val)-> str.equals(key) && filter.test(val);
    }

    default boolean testMap(Map<String,T> map) {
        for(Map.Entry<String, T> entry: map.entrySet()) {
            if(this.test(entry.getKey(),entry.getValue())) {
                return true;
            }
        }
        return false;
    }

}
