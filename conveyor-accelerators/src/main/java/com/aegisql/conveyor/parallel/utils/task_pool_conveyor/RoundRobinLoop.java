package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

public class RoundRobinLoop {

    private final int size;

    private volatile int next = 0;

    public RoundRobinLoop(int size) {
        this.size = size;
    }

    public synchronized int next() {
        if(next == size) {
            next = 0;
        }
        return next++;
    }

}
