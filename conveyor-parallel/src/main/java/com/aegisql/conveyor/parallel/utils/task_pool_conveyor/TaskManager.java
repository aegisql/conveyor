package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TaskManager <K,T> implements Supplier<T>, Testing, TimeoutAction {

    private final RoundRobinLoop rr;
    private final AssemblingConveyor<K, TaskHolder.Label, T>[] pool;
    private final Supplier<K> keySupplier;
    private final Supplier<Long> expirationTimeSupplier;
    private final Supplier<Map<String, Object>> currentProperties;

    private int conveyorID;
    private K key;
    private CompletableFuture<Boolean> future;
    private boolean ready = false;
    private long startTime;


    public enum Label implements TaskManagerLabel{
        EXECUTE((b,s)->b.execute(s))
        ;

        private final BiConsumer<TaskManager,Supplier> consumer;

        Label(BiConsumer<TaskManager,Supplier> consumer) {
            this.consumer = consumer;
        }

        @Override
        public BiConsumer<TaskManager,Supplier> get() {
            return consumer;
        }
    }

    private void cancel(T r) {
        result = r;
        pool[conveyorID].command().id(key).cancel();
        pool[conveyorID].interrupt(pool[conveyorID].getName(),key);
    }

    enum ProtectedLabel implements TaskManagerLabel{
        RESULT((b,s)->b.result(s))
        ,ERROR((b,s)->b.error((ScrapBin) s))
        ,STARTED((b,t)->b.started((Long) t))
        ;

        private final BiConsumer<TaskManager,Object> consumer;

        ProtectedLabel(BiConsumer<TaskManager,Object> consumer) {
            this.consumer = consumer;
        }

        @Override
        public BiConsumer<TaskManager,Object> get() {
            return consumer;
        }
    }

    private void started(long t) {
        this.startTime = t;
    }

    public TaskManager(RoundRobinLoop rr, AssemblingConveyor<K,TaskHolder.Label,T>[] pool, Supplier<K> keySupplier, Supplier<Long> current_expiration_time, Supplier<Map<String, Object>> current_properties) {
        this.rr = rr;
        this.pool = pool;
        this.keySupplier = keySupplier;
        this.expirationTimeSupplier = current_expiration_time;
        this.currentProperties = current_properties;
    }

    private T result;

    void execute(Supplier<T> supplier) {
        //send to the pool
        if(future == null) {
            conveyorID = rr.next();
            key = keySupplier.get();
            future = pool[conveyorID]
                    .part()
                    .expirationTime(expirationTimeSupplier.get())
                    .id(key)
                    .label(TaskHolder.Label.EXECUTE)
                    .value(supplier)
                    .place();
        } else {
            throw new KeepRunningConveyorException("Task "+key+" is already running");
        }
    }

    void result(T result) {
        this.result = result;
        if(this.future == null) {
            throw new RuntimeException("Task "+keySupplier.get()+" has been canceled or timed out");
        }
        this.ready = true;
    }

    void error(ScrapBin<K,T> scrap) {
        if(scrap.error != null) {
            throw new RuntimeException("Task Execution failed "+scrap,scrap.error);
        } else {
            throw new RuntimeException("Task Execution failed "+scrap);
        }
    }

    @Override
    public T get() {
        Map<String, Object> properties = currentProperties.get();
        properties.put("STARTED_TIME", startTime);
        properties.put("END_TIME", System.currentTimeMillis());
        return result;
    }

    @Override
    public boolean test() {
        return ready;
    }

    @Override
    public void onTimeout() {
        cancel(null);
    }


}
