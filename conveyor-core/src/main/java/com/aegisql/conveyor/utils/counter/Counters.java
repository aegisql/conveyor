package com.aegisql.conveyor.utils.counter;

import java.util.*;

public class Counters {
    private final Map<String,Integer> counters = new HashMap<>();

    public void add(String name, int value) {
        counters.merge(name, value, Integer::sum);
    }

    public Map<String,Integer> getCounters() {
        return Collections.unmodifiableMap(counters);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Counters.class.getSimpleName() + "[", "]")
                .add("counters=" + counters)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Counters counters1 = (Counters) o;
        return Objects.equals(counters, counters1.counters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(counters);
    }

    public boolean containsAllNames(Collection<String> names) {
        return counters.keySet().containsAll(names);
    }
}
