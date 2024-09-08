package com.aegisql.conveyor.poc.thread_pool;

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
