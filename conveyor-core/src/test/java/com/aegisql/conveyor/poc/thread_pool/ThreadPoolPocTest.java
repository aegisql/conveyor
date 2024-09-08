package com.aegisql.conveyor.poc.thread_pool;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.loaders.FutureLoader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadPoolPocTest {

    @Test
    public void testRoundRobinLoop() {
        RoundRobinLoop loop = new RoundRobinLoop(3);
        assertEquals(0,loop.next());
        assertEquals(1,loop.next());
        assertEquals(2,loop.next());
        assertEquals(0,loop.next());
        assertEquals(1,loop.next());
        assertEquals(2,loop.next());
        assertEquals(0,loop.next());
    }


    public int slowMethod(int n) {
        int sum = 0;
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


    @Test
    public void poolTest() {

        RoundRobinLoop rr = new RoundRobinLoop(3);

        AssemblingConveyor<Integer,TaskHolder.Label,Integer> c0 = new AssemblingConveyor<>();
        AssemblingConveyor<Integer,TaskHolder.Label,Integer> c1 = new AssemblingConveyor<>();
        AssemblingConveyor<Integer,TaskHolder.Label,Integer> c2 = new AssemblingConveyor<>();

        Conveyor[] pool = new Conveyor[]{c0,c1,c2};

        AssemblingConveyor<Integer,SmartLabel<TaskManager>,Integer> pc = new AssemblingConveyor<>();

        pc.setName("pool_conveyor");
        pc.setBuilderSupplier(TaskManager::new);
        pc.resultConsumer(LogResult.debug(pc)).set();
        pc.staticPart().label(TaskManager.PrivateLabel.MANAGER).value(pc).place();
        pc.staticPart().label(TaskManager.PrivateLabel.POOL).value(pool).place();
        pc.staticPart().label(TaskManager.PrivateLabel.RR).value(rr).place();
        pc.staticPart().label(TaskManager.PrivateLabel.MY_ID).value(pc.current_id).place();

        c0.setName("task_conveyor[0]");
        c0.setBuilderSupplier(TaskHolder::new);
        c0.staticPart().label(TaskHolder.Label.MY_ID).value(c0.current_id).place();
        c0.resultConsumer(bin->{
            System.out.println("result[0] "+bin);
            pc.part().id(bin.key).label(TaskManager.PrivateLabel.RESULT).value(bin.product).place();

        }).set();

        c1.setName("task_conveyor[1]");
        c1.setBuilderSupplier(TaskHolder::new);
        c1.staticPart().label(TaskHolder.Label.MY_ID).value(c1.current_id).place();
        c1.resultConsumer(bin->{
            System.out.println("result[1] "+bin);

        }).set();

        c2.setName("task_conveyor[2]");
        c2.setBuilderSupplier(TaskHolder::new);
        c2.staticPart().label(TaskHolder.Label.MY_ID).value(c2.current_id).place();
        c2.resultConsumer(bin->{
            System.out.println("result[2] "+bin);

        }).set();

        Supplier<Integer> supplier = ()->slowMethod(3);

        CompletableFuture<Integer> future = pc.build().id(1).createFuture();

        pc.part().id(1).label(TaskManager.Label.EXECUTE).value(supplier).place();

        future.join();

    }


}
