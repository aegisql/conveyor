package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.consumers.scrap.ScrapCounter;
import com.aegisql.conveyor.consumers.scrap.ScrapQueue;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TaskPoolConveyor1Test {

    public static final String FIRST = "first";
    public static final String LAST = "last";

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
        int sum = 1;
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
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setDefaultCartConsumer(consumer());

        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(conveyor,1);
        taskPoolConveyor.setName("pool");

        taskPoolConveyor.part().id(1).label(FIRST).value(10).place();
        taskPoolConveyor.part().id(1).label(LAST).value(10).place().join();

        taskPoolConveyor.completeAndStop().join();

        assertEquals(20,reference.getCurrent());

    }


    @Test
    public void basicTaskTest() throws InterruptedException {
        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(1);
        LastResultReference<Integer, Integer> reference = LastResultReference.of(taskPoolConveyor);
        taskPoolConveyor.setBuilderSupplier(Summ::new);
        taskPoolConveyor.resultConsumer(LogResult.debug(taskPoolConveyor)).andThen(reference).set();
        taskPoolConveyor.setReadinessEvaluator(Conveyor.getTesterFor(taskPoolConveyor).accepted(FIRST, LAST));
        taskPoolConveyor.setDefaultCartConsumer(consumer());

        taskPoolConveyor.setName("pool");

        assertEquals(1,taskPoolConveyor.getPoolSize());

        taskPoolConveyor.part().id(2).label(FIRST).value(10).place();
        taskPoolConveyor.task().id(2).label(LAST).valueSupplier(()->slowMethod(1)).ttl(Duration.ofSeconds(3)).addProperty("test","basicTaskTest").placeAsynchronous();

        taskPoolConveyor.completeAndStop().join();

        assertEquals(11,reference.getCurrent());

    }

    @Test
    public void foreachTaskTest() throws InterruptedException {
        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(1);
        ResultQueue<Integer, Integer> reference = ResultQueue.of(taskPoolConveyor);
        taskPoolConveyor.setBuilderSupplier(Summ::new);
        taskPoolConveyor.resultConsumer(LogResult.debug(taskPoolConveyor)).andThen(reference).set();
        taskPoolConveyor.setReadinessEvaluator(Conveyor.getTesterFor(taskPoolConveyor).accepted(FIRST, LAST));
        taskPoolConveyor.setDefaultCartConsumer(consumer());

        taskPoolConveyor.setName("pool");

        taskPoolConveyor.part().id(1).label(FIRST).value(10).place();
        taskPoolConveyor.part().id(2).label(FIRST).value(10).place().join();
        taskPoolConveyor.task().foreach().label(LAST).valueSupplier(()->slowMethod(1)).placeAsynchronous();

        taskPoolConveyor.completeAndStop().join();
        assertEquals(11,reference.poll());
        assertEquals(11,reference.poll());

    }

    @Test
    public void basicTaskFailureTest() throws InterruptedException {
        var conveyor = new AssemblingConveyor<Integer,String,Integer>();
        LastResultReference<Integer, Integer> reference = LastResultReference.of(conveyor);
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.resultConsumer(LogResult.debug(conveyor)).andThen(reference).set();
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setDefaultCartConsumer(consumer());

        LastScrapReference<Integer> scrapReference = LastScrapReference.of(conveyor);

        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(conveyor,1);
        taskPoolConveyor.setName("pool");
        taskPoolConveyor.scrapConsumer().andThen(scrapReference).set();

        taskPoolConveyor.part().id(1).label(FIRST).value(10).place();

        taskPoolConveyor.part().id(2).label(FIRST).value(10).place().join();
        taskPoolConveyor.task().id(2).label(LAST).valueSupplier(()->slowMethod(-1)).addProperty("test","basicTaskFailureTest").placeAsynchronous();


        CompletableFuture<Boolean> stop = taskPoolConveyor.completeAndStop();
        taskPoolConveyor.part().id(1).label(LAST).value(10).place();

        stop.join();
        System.out.println(scrapReference.getCurrent());
        assertNotNull(scrapReference.getCurrent());

    }

    @Test
    public void customTaskFailureTest() throws InterruptedException {
        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(1);
        LastResultReference<Integer, Integer> reference = LastResultReference.of(taskPoolConveyor);
        taskPoolConveyor.setBuilderSupplier(Summ::new);
        taskPoolConveyor.resultConsumer(LogResult.debug(taskPoolConveyor)).andThen(reference).set();
        taskPoolConveyor.setReadinessEvaluator(Conveyor.getTesterFor(taskPoolConveyor).accepted(FIRST, LAST));
        taskPoolConveyor.setDefaultCartConsumer(consumer());

        taskPoolConveyor.taskScrapConsumer(bin->{
            bin.conveyor.command().id(bin.key).completeExceptionally(new KeepRunningConveyorException("Just Keep Running",bin.error));
        }).set();

        LastScrapReference<Integer> scrapReference = LastScrapReference.of(taskPoolConveyor);

        taskPoolConveyor.setName("pool");
        taskPoolConveyor.scrapConsumer().andThen(scrapReference).set();

        taskPoolConveyor.part().id(1).label(FIRST).value(10).place();

        assertTrue(taskPoolConveyor.part().id(2).label(FIRST).value(10).place().join());
        taskPoolConveyor.task().id(2).label(LAST).valueSupplier(()->slowMethod(-1)).placeAsynchronous();

        Thread.sleep(2000);
        taskPoolConveyor.part().id(1).label(LAST).value(10).place().join();
        taskPoolConveyor.part().id(2).label(LAST).value(100).place().join();


        CompletableFuture<Boolean> stop = taskPoolConveyor.completeAndStop();
        stop.join();
        System.out.println(scrapReference.getCurrent());
        assertNotNull(scrapReference.getCurrent());

    }


    private LabeledValueConsumer<String, Object, Supplier<? extends Integer>> consumer() {
        return (l, v, b) -> {
            Summ summ = (Summ) b;
            switch (l) {
                case FIRST:
                    summ.first((Integer) v);
                    break;
                case LAST:
                    summ.last((Integer) v);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + l);
            }
        };
    }

    @Test
    public void testBigTasks() throws InterruptedException {
        TaskPoolConveyor<Integer,String,Integer> conveyor = new TaskPoolConveyor<>(20);
        ResultQueue<Integer,Integer> results = ResultQueue.of(conveyor);
        ResultCounter<Integer,Integer> resultCounter = ResultCounter.of(conveyor);
        LogResult<Integer,Integer> log = LogResult.debug(conveyor);
        ScrapQueue<Integer> errors = ScrapQueue.of(conveyor);
        ScrapCounter<Integer> errorCounter = ScrapCounter.of(conveyor);
        conveyor.resultConsumer(resultCounter).andThen(results).andThen(log).set();
        conveyor.scrapConsumer(errorCounter).andThen(errors).set();
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.setDefaultCartConsumer(consumer());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setName("testBigTasks");

        int N = 100;

        for (int i = 1; i <= N; i++) {
            conveyor.command().id(i).create();
        }
        var shuffledList = getDoubleShuffledeList(N);
        assertEquals(2*N,shuffledList.size());

        shuffledList.forEach(next->conveyor.task().id((int) next.id()).label(next.key()).valueSupplier(getSupplier(1)).placeAsynchronous());

        conveyor.completeAndStop().join();

        assertEquals(0,errorCounter.get());
        assertEquals(100,resultCounter.get());

    }

    @Test
    public void testBigTaskWithRandomPriority() throws InterruptedException {
        TaskPoolConveyor<Integer,String,Integer> conveyor = new TaskPoolConveyor<>(20);
        ResultQueue<Integer,Integer> results = ResultQueue.of(conveyor);
        ResultCounter<Integer,Integer> resultCounter = ResultCounter.of(conveyor);
        LogResult<Integer,Integer> log = LogResult.debug(conveyor);
        ScrapQueue<Integer> errors = ScrapQueue.of(conveyor);
        ScrapCounter<Integer> errorCounter = ScrapCounter.of(conveyor);
        conveyor.resultConsumer(resultCounter).andThen(results).andThen(log).set();
        conveyor.scrapConsumer(errorCounter).andThen(errors).set();
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.setDefaultCartConsumer(consumer());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setName("testBigTaskWithRandomPriority");

        int N = 100;

        for (int i = 1; i <= N; i++) {
            conveyor.command().id(i).create();
        }

        List<Integer> priorities = new ArrayList<>(2*N);
        List<TaskId<String>>  list = new ArrayList<>(2*N);
        for (int i = 1; i <= N; i++) {
            priorities.add(i);
            priorities.add(i);
            list.add(new TaskId<>(FIRST, i));
            list.add(new TaskId<>(LAST, i));
        }
        Collections.shuffle(priorities);

        Iterator<Integer> priority = priorities.iterator();

        list.forEach(next->conveyor.task().id((int) next.id()).label(next.key()).valueSupplier(getSupplier(1)).priority(priority.next()).placeAsynchronous());

        conveyor.completeAndStop().join();

        assertEquals(0,errorCounter.get());
        assertEquals(100,resultCounter.get());

    }

    @Test
    public void testBigTasksWithExpiration() throws InterruptedException {
        TaskPoolConveyor<Integer,String,Integer> conveyor = new TaskPoolConveyor<>(20);
        ResultQueue<Integer,Integer> results = ResultQueue.of(conveyor);
        ResultCounter<Integer,Integer> resultCounter = ResultCounter.of(conveyor);
        LogResult<Integer,Integer> log = LogResult.debug(conveyor);
        ScrapQueue<Integer> errors = ScrapQueue.of(conveyor);
        ScrapCounter<Integer> errorCounter = ScrapCounter.of(conveyor);
        conveyor.resultConsumer(resultCounter).andThen(results).andThen(log).set();
        conveyor.scrapConsumer(errorCounter).andThen(errors).set();
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.setDefaultCartConsumer(consumer());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setName("testBigTasksWithExpiration");

        int N = 100;
        AtomicReference<CompletableFuture<Boolean>> f = new AtomicReference<>();
        for (int i = 1; i <= N; i++) {
            f.set(conveyor.command().id(i).create());
        }
        f.get().join();
        var shuffledList = getDoubleShuffledeList(N);
        assertEquals(2*N,shuffledList.size());

        shuffledList.forEach(next-> f.set(conveyor.task().id((int) next.id()).label(next.key()).valueSupplier(getSupplier(1)).ttl(Duration.ofSeconds(7)).placeAsynchronous()));

        conveyor.completeAndStop().join();

        assertTrue(resultCounter.get()<100);
        assertTrue(errorCounter.get()>0);


    }

    @Test
    public void testBigTasksWithAttempts() throws InterruptedException {
        TaskPoolConveyor<Integer,String,Integer> conveyor = new TaskPoolConveyor<>(20);
        ResultQueue<Integer,Integer> results = ResultQueue.of(conveyor);
        ResultCounter<Integer,Integer> resultCounter = ResultCounter.of(conveyor);
        LogResult<Integer,Integer> log = LogResult.debug(conveyor);
        ScrapQueue<Integer> errors = ScrapQueue.of(conveyor);
        ScrapCounter<Integer> errorCounter = ScrapCounter.of(conveyor);
        conveyor.resultConsumer(resultCounter).andThen(results).andThen(log).set();
        conveyor.scrapConsumer(errorCounter).andThen(errors).set();
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.setDefaultCartConsumer(consumer());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setName("testBigTasksWithAttempts");

        int N = 100;
        AtomicReference<CompletableFuture<Boolean>> f = new AtomicReference<>();
        for (int i = 1; i <= N; i++) {
            f.set(conveyor.command().id(i).create());
        }
        f.get().join();
        var shuffledList = getDoubleShuffledeList(N);
        assertEquals(2*N,shuffledList.size());

        shuffledList.forEach(next-> f.set(conveyor.task().id((int) next.id()).label(next.key()).valueSupplier(()->{
            exceptionWithProbability(0.1); // 10% failure probability for each attempt
            return getSupplier(1).get();
        }).attempts(5).placeAsynchronous())); // 5 attempts

        conveyor.completeAndStop().join();

        assertTrue(resultCounter.get()==100); //with 5 attempts there is a very low provability of failure, yet it can happen
        assertTrue(errorCounter.get()==0);


    }

    @Test
    public void testBigTasksWithDefaultOnExpiration() throws InterruptedException {
        TaskPoolConveyor<Integer,String,Integer> conveyor = new TaskPoolConveyor<>(20);
        ResultQueue<Integer,Integer> results = ResultQueue.of(conveyor);
        ResultCounter<Integer,Integer> resultCounter = ResultCounter.of(conveyor);
        LogResult<Integer,Integer> log = LogResult.debug(conveyor);
        ScrapQueue<Integer> errors = ScrapQueue.of(conveyor);
        ScrapCounter<Integer> errorCounter = ScrapCounter.of(conveyor);
        conveyor.resultConsumer(resultCounter).andThen(results).andThen(log).set();
        conveyor.scrapConsumer(errorCounter).andThen(errors).set();
        conveyor.setBuilderSupplier(Summ::new);
        conveyor.setDefaultCartConsumer(consumer());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(FIRST, LAST));
        conveyor.setName("testBigTasksWithExpiration");
        conveyor.taskScrapConsumer(conveyor.ON_FAILURE_PLACE_DEFAULT("last",0)).set();

        int N = 100;
        AtomicReference<CompletableFuture<Boolean>> f = new AtomicReference<>();
        for (int i = 1; i <= N; i++) {
            f.set(conveyor.part().id(i).label("first").value(10).place());
        }
        f.get().join();
        var shuffledList = getShuffledeList(N);
        assertEquals(N,shuffledList.size());

        shuffledList.forEach(next-> f.set(conveyor.task().id((int) next.id()).label(next.key()).valueSupplier(getSupplier(1)).ttl(Duration.ofSeconds(7)).addProperty("test","testBigTasksWithDefaultOnExpiration").placeAsynchronous()));

        conveyor.completeAndStop().join();

        assertTrue(resultCounter.get()==100);
        assertTrue(errorCounter.get()==0);

    }


    public List<TaskId<String>> getDoubleShuffledeList(int size) {
        List<TaskId<String>> list = new ArrayList<>(2*size);
        for (int i = 1; i <= size; i++) {
            list.add(new TaskId<>(FIRST, i));
            list.add(new TaskId<>(LAST, i));
        }
        Collections.shuffle(list);
        return list;
    }

    public List<TaskId<String>> getShuffledeList(int size) {
        List<TaskId<String>> list = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            list.add(new TaskId<>(LAST, i));
        }
        Collections.shuffle(list);
        return list;
    }


    private static final Random random = new Random();

    public static boolean trueWithProbability(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("Probability must be between 0 and 1 inclusive.");
        }
        return random.nextDouble() < probability;
    }

    public static void exceptionWithProbability(double probability) {
        if (trueWithProbability(probability)) {
            throw new RuntimeException("Probability check failed.");
        }
    }

    @Test
    public void testExceptionWithProbability() throws InterruptedException {

        assertThrows(RuntimeException.class, ()->{for(;;){
            System.out.print(".");
            exceptionWithProbability(0.1);
        }});
        System.out.println();
    }

    @Test
    public void taskLoaderTest() throws InterruptedException {
        var taskPoolConveyor = new TaskPoolConveyor<Integer,String,Integer>(1);
        LastResultReference<Integer, Integer> reference = LastResultReference.of(taskPoolConveyor);
        taskPoolConveyor.setBuilderSupplier(Summ::new);
        taskPoolConveyor.resultConsumer(LogResult.debug(taskPoolConveyor)).andThen(reference).set();
        taskPoolConveyor.setReadinessEvaluator(Conveyor.getTesterFor(taskPoolConveyor).accepted(FIRST, LAST));
        taskPoolConveyor.setDefaultCartConsumer(consumer());

        taskPoolConveyor.setName("taskLoaderTest");

        Supplier<TaskLoader<Object, Object>> blah = TaskLoader.lazySupplier("blah");

        assertNotNull(blah);
        assertThrows(ConveyorRuntimeException.class, blah::get);

        Supplier<TaskLoader<Integer, String>> tls = TaskLoader.lazySupplier("taskLoaderTest");
        TaskLoader<Integer, String> tl = tls.get();

        taskPoolConveyor.part().id(2).label(FIRST).value(10).place();
        tl.id(2).label(LAST).valueSupplier(getSupplier(1)).addProperty("test","basicTaskTest").placeAsynchronous();
        taskPoolConveyor.completeAndStop().join();

        assertEquals(11,reference.getCurrent());

    }

}