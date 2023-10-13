package com.aegisql.conveyor.utils.queue_pump;

import com.aegisql.conveyor.Testing;

import java.util.function.Supplier;

public class ScalarHolder <OUT> implements Supplier<OUT>, Testing {
    private OUT value;

    public void setValue(OUT value) {
        this.value = value;
    }
    @Override
    public OUT get() {
        return value;
    }

    @Override
    public boolean test() {
        return true;
    }


}
