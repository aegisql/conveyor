package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.exception.KeepRunningConveyorException;

public class Counter {

    private final String name;
    private int count = 0;
    private int expected = -1;

    public Counter(String name) {
        this.name = name;
    }

    public boolean test() {
        if( expected < 0 || expected > count) {
            return false;
        } else if (expected == count) {
            return true;
        } else {
            throw new IllegalStateException("Counter " + name + " expected: " + expected + ", actual: " + count);
        }
    }

    public void add(int value) {
        if(value < 0) {
            throw new IllegalArgumentException("Counter " + name + " cannot be decremented, value: " + value);
        }
        count += value;
    }

    public void setExpected(int expected) {
        if(expected < 0) {
            throw new IllegalArgumentException("Counter " + name + " cannot have negative expected value: " + expected);
        }
        if(this.expected >= 0 && this.expected != expected) {
            throw new KeepRunningConveyorException("Counter " + name + " already has expected value set: " + this.expected + ", cannot be changed to: " + expected);
        }
        this.expected = expected;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public int getExpected() {
        return expected;
    }
    @Override
    public String toString() {
        return "Counter{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", expected=" + expected +
                '}';
    }
}
