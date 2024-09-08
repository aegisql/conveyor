package com.aegisql.conveyor.poc.thread_pool;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Testing;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TaskManager <OUT> implements Supplier<OUT>, Testing {

    enum PrivateLabel implements SmartLabel<TaskManager> {
        MANAGER((tm,c)->tm.setTmc((Conveyor)c))
        ,RESULT((tm,res)->tm.setResult(res))
        ,RR((tm,rr)->tm.setRr((RoundRobinLoop) rr))
        ,POOL((tm,pool)->tm.setPool((Conveyor[]) pool))
        ,MY_ID((tm,supplier)->tm.setMyId((Supplier<Integer>) supplier))
        ;

        private final BiConsumer<TaskManager, Object> consumer;

        <T> PrivateLabel(BiConsumer<TaskManager, T> consumer) {
            this.consumer = (BiConsumer<TaskManager, Object>) consumer;
        }

        @Override
        public BiConsumer<TaskManager, Object> get() {
            return consumer;
        }
    }

    public enum Label implements SmartLabel<TaskManager> {
        EXECUTE((tm, c)->tm.execute((Supplier)c))
        ;

        private final BiConsumer<TaskManager, Object> consumer;

        <T> Label(BiConsumer<TaskManager, T> consumer) {
            this.consumer = (BiConsumer<TaskManager, Object>) consumer;
        }

        @Override
        public BiConsumer<TaskManager, Object> get() {
            return consumer;
        }
    }

    private final static AtomicLong idgen = new AtomicLong(1);

    private boolean ready = false;
    private OUT result = null;
    private RoundRobinLoop rr;
    private Conveyor tmc;
    private Conveyor[] pool;
    private Supplier<Integer> myId;
    private int executor = -1;
    private Long id = idgen.get();
    private CompletableFuture future;

    private void execute(Supplier supplier) {
        executor = rr.next();
        Conveyor c = pool[executor];
        this.future = c.part().id(myId.get()).label(TaskHolder.Label.EXECUTE).value(supplier).place();
    }

    private void setResult(OUT result) {
        this.result = result;
        this.ready = true;
    }

    private void setRr(RoundRobinLoop rr) {
        this.rr = rr;
    }

    private void setTmc(Conveyor tmc) {
        this.tmc = tmc;
    }

    private void setPool(Conveyor[] pool) {
        this.pool = pool;
    }

    public void setMyId(Supplier<Integer> myId) {
        this.myId = myId;
    }

    @Override
    public OUT get() {
        return result;
    }

    @Override
    public boolean test() {
        return ready;
    }
}
