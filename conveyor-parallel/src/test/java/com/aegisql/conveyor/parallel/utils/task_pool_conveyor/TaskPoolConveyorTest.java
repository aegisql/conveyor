package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.consumers.result.LogResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.aegisql.conveyor.parallel.utils.task_pool_conveyor.TaskManager.Label.EXECUTE;
import static org.junit.jupiter.api.Assertions.*;

class TaskPoolConveyorTest {

    public int slowMethod(int n) {
        int sum = 0;
        if(n<0) {
            throw new IllegalArgumentException("Illegal argument: "+n);
        }
        for (int i = 0; i < n; i++) {
            sum += i;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return sum;
    }

    Supplier<Integer> getSupplier(int n) {
        return () -> slowMethod(n);
    }

    @Test
    public void basicTest() throws InterruptedException {
        TaskPoolConveyor tpc = new TaskPoolConveyor<>(3);
        assertEquals(3,tpc.getPoolSize());
        tpc.resultConsumer(LogResult.debug(tpc)).set();
        CompletableFuture future = tpc.future().id(1).get();
        tpc.part().id(1).label(EXECUTE).value(getSupplier(3)).place();
        future.join();
    }

    @Test
    public void basicErrorTest() throws InterruptedException {
        TaskPoolConveyor tpc = new TaskPoolConveyor<>(3);
        tpc.resultConsumer(LogResult.debug(tpc)).set();
        CompletableFuture future = tpc.future().id(1).get();
        tpc.part().id(1).label(EXECUTE).value(getSupplier(-3)).place();
        assertThrows(RuntimeException.class,()->future.join());
    }

    @Test
    public void basicTimeoutTest() throws InterruptedException {
        TaskPoolConveyor tpc = new TaskPoolConveyor<>(3);
        tpc.resultConsumer(LogResult.debug(tpc)).set();
        CompletableFuture future = tpc.future().ttl(Duration.ofSeconds(3)).id(1).get();
        tpc.part().id(1).label(EXECUTE).value(getSupplier(30)).place();

        tpc.part().id(2).label(EXECUTE).value(getSupplier(3)).place();
        tpc.part().id(3).label(EXECUTE).value(getSupplier(3)).place();

        CompletableFuture future2 = tpc.future().ttl(Duration.ofSeconds(2)).id(4).get();
        tpc.part().id(4).label(EXECUTE).value(getSupplier(30)).place();

        assertThrows(RuntimeException.class,()->future.join());
        assertThrows(RuntimeException.class,()->future2.join());
    }

    @Test
    public void completeAndStopTest () throws InterruptedException {
        TaskPoolConveyor<Integer,Integer> tpc = new TaskPoolConveyor<>(3);
        assertEquals(3,tpc.getPoolSize());
        tpc.resultConsumer(LogResult.debug(tpc)).set();

        CompletableFuture<Boolean> placed = null;
        for(int i = 1; i <=6; i++) {
            placed = tpc.part().id(i).label(EXECUTE).value(getSupplier(2)).place();
        }
        placed.join();
        assertTrue(tpc.completeAndStop().join());
        tpc.stop();
    }

}