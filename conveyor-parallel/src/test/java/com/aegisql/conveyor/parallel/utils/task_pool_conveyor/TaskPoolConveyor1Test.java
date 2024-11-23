package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskPoolConveyor1Test {

    class Summ implements Supplier<Integer> {

        private Integer sum = 0;

        public void first(Integer first) {
            sum += first;
        }

        public void last(Integer last) {
            sum += last;
        }

        @Override
        public Integer get() {
            return sum;
        }
    }
    
    public int slowMethod(int n) {
        int sum = 0;
        for (int i = 0; i < Math.abs(n); i++) {
            sum += i;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(n<0) {
                throw new IllegalArgumentException("Illegal argument: "+n);
            }
        }
        return sum;
    }

    Supplier<Integer> getSupplier(int n) {
        return () -> slowMethod(n);
    }

    @Test
    public void noTaskTest() throws InterruptedException {
        var conveyor = new AssemblingConveyor<Integer,String,Integer>();
        LastResultReference<Integer, Integer> reference = LastResultReference.of(conveyor);
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.resultConsumer(LogResult.debug(conveyor)).andThen(reference).set();
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted("first","last"));
        conveyor.setDefaultCartConsumer((l,v,b)->{
            Summ summ = (Summ) b;
            switch (l) {
                case "first": summ.first((Integer) v);
                    break;
                case "last": summ.last((Integer) v);
                    break;
                default: throw new IllegalStateException("Unexpected value: "+l);
            }
        });

        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(conveyor,1);
        taskPoolConveyor.setName("pool");

        taskPoolConveyor.part().id(1).label("first").value(10).place();
        taskPoolConveyor.part().id(1).label("last").value(10).place().join();

        taskPoolConveyor.completeAndStop().join();

        assertEquals(20,reference.getCurrent());

    }


    @Test
    public void basicTaskTest() throws InterruptedException {
        var conveyor = new AssemblingConveyor<Integer,String,Integer>();
        LastResultReference<Integer, Integer> reference = LastResultReference.of(conveyor);
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.resultConsumer(LogResult.debug(conveyor)).andThen(reference).set();
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted("first","last"));
        conveyor.setDefaultCartConsumer((l,v,b)->{
            Summ summ = (Summ) b;
            switch (l) {
                case "first": summ.first((Integer) v);
                break;
                case "last": summ.last((Integer) v);
                break;
                default: throw new IllegalStateException("Unexpected value: "+l);
            }
        });

        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(conveyor,1);
        taskPoolConveyor.setName("pool");

        taskPoolConveyor.part().id(2).label("first").value(10).place();
        taskPoolConveyor.task().id(2).label("last").valueSupplier(()->slowMethod(1)).ttl(Duration.ofSeconds(3)).placeAsynchronous();

        taskPoolConveyor.completeAndStop().join();

        assertEquals(10,reference.getCurrent());

    }

    @Test
    public void basicTaskFailureTest() throws InterruptedException {
        var conveyor = new AssemblingConveyor<Integer,String,Integer>();
        LastResultReference<Integer, Integer> reference = LastResultReference.of(conveyor);
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.resultConsumer(LogResult.debug(conveyor)).andThen(reference).set();
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted("first","last"));
        conveyor.setDefaultCartConsumer((l,v,b)->{
            Summ summ = (Summ) b;
            switch (l) {
                case "first": summ.first((Integer) v);
                    break;
                case "last": summ.last((Integer) v);
                    break;
                default: throw new IllegalStateException("Unexpected value: "+l);
            }
        });

        LastScrapReference<Integer> scrapReference = LastScrapReference.of(conveyor);

        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(conveyor,1);
        taskPoolConveyor.setName("pool");
        taskPoolConveyor.scrapConsumer().andThen(scrapReference).set();

        taskPoolConveyor.part().id(1).label("first").value(10).place();

        taskPoolConveyor.part().id(2).label("first").value(10).place().join();
        taskPoolConveyor.task().id(2).label("last").valueSupplier(()->slowMethod(-1)).placeAsynchronous();


        CompletableFuture<Boolean> stop = taskPoolConveyor.completeAndStop();
        taskPoolConveyor.part().id(1).label("last").value(10).place();

        stop.join();
        System.out.println(scrapReference.getCurrent());
        assertNotNull(scrapReference.getCurrent());

    }


}