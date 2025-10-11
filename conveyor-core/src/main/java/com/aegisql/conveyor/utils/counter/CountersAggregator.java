package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.Testing;

import java.util.*;
import java.util.function.Supplier;

public class CountersAggregator implements Supplier<Map<String,Map<String,Integer>>>, Testing {

    private final Set<String> names = new HashSet<>();
    private final Map<String,Counter> counters = new HashMap<>();

    private Counter counter(String name) {
        return counters.computeIfAbsent(name, Counter::new);
    }

    public void addExpected(String name, int value) {
        counter(name).setExpected(value);
        names.add(name);
    }

    public void addCounter(String name, int value) {
        counter(name).add(value);
        names.add(name);
    }

    public void addName(String name) {
        names.add(name);
        counter(name);
    }

    public void addNames(Collection<String> names) {
        names.forEach(this::addName);
    }

    @Override
    public boolean test() {
        return !names.isEmpty() && counters.values().stream().allMatch(Counter::test);
    }

    @Override
    public Map<String, Map<String, Integer>> get() {
        Set<String> allNames = new HashSet<>(names);
        allNames.addAll(counters.keySet());
        Map<String, Map<String, Integer>> result = new HashMap<>();

        allNames.forEach(name->{
            result.put(name, Map.of("expected", counter(name).getExpected(), "actual", counter(name).getCount()));
        });
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CountersAggregator.class.getSimpleName() + "[", "]")
                .add("names=" + names)
                .add("counters=" + counters)
                .toString();
    }
}
