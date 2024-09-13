package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinLoopTest {

    @Test
    void test() {
        RoundRobinLoop loop = new RoundRobinLoop(3);
        assertEquals(0,loop.next());
        assertEquals(1,loop.next());
        assertEquals(2,loop.next());
        assertEquals(0,loop.next());
        assertEquals(1,loop.next());
        assertEquals(2,loop.next());
        assertEquals(0,loop.next());
    }

}