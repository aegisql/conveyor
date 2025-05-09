package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.Testing;

import java.util.Objects;
import java.util.function.Supplier;

public class TaskExecutor <T> implements Supplier<T>, Testing {

    private T result;

    @Override
    public T get() {
        return result;
    }

    public void task(Supplier<T> task) {
        Objects.requireNonNull(task,"task cannot be null");
        this.result = task.get();
    }

    @Override
    public boolean test() {
        return true;
    }

}
