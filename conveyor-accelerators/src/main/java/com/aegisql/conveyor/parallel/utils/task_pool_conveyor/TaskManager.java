package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.function.Supplier;

public class TaskManager <T> implements Supplier<T>, Testing {

    private T result;
    private boolean ready = false;

    public void done(T result) {
        this.result = result;
        this.ready = true;
    }

    public void error(Throwable error) {
        throw new ConveyorRuntimeException("Task failed",error);
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    public boolean test() {
        return ready;
    }
}
