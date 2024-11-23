package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.utils.ConveyorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * An implementation of the Conveyor interface that wraps an inner conveyor instance.
 *
 * @param <K>   the key type
 * @param <OUT> the output type
 */
public class TaskPoolConveyor<K, L, OUT> extends ConveyorAdapter<K, L, OUT> {

    protected final static Logger LOG = LoggerFactory.getLogger(TaskPoolConveyor.class);

    private final Conveyor<TaskId<K>,String,OUT> taskManager = new AssemblingConveyor<>();
    private final PartLoader<TaskId<K>, Integer>[] loadersPool;
    private final Conveyor[] conveyors;
    private final AtomicLong counter = new AtomicLong();
    private final RoundRobinLoop rr;

    public TaskPoolConveyor(int poolSize) {
        this(new AssemblingConveyor<>(), poolSize);
    }

    public TaskPoolConveyor(Conveyor<K,L,OUT> conv, int poolSize) {
        super(conv);
        loadersPool = new PartLoader[poolSize];
        conveyors = new Conveyor[poolSize];
        rr = new RoundRobinLoop(poolSize);
        for (int i = 0; i < poolSize; i++) {
            var tec = new AssemblingConveyor<TaskId<K>,Integer,OUT>();
            tec.setBuilderSupplier(TaskExecutor::new);
            tec.setDefaultCartConsumer((Integer l,Supplier<OUT> v,TaskExecutor<OUT> b)->{
                b.task(v);
            });
            tec.resultConsumer(bin->{
                LOG.debug("The task {} has been completed.", bin.key);
                taskManager.part().id(bin.key).label("done").value(bin.product).place();
            }).set();
            tec.scrapConsumer(bin->{
                LOG.error("The task {} has failed. Failure type: {}{}", bin.key, bin.failureType, bin.error == null ? "" : " Error: "+bin.error.getMessage());
                taskManager.part().id(bin.key).label("error").value(bin.error).place();
            }).set();
            tec.setName(conv.getName()+"_task_executor["+i+"]");
            loadersPool[i] = tec.part().label(1);
            conveyors[i] = tec;
        }
        taskManager.setBuilderSupplier(TaskManager::new);
        taskManager.resultConsumer(bin->{
            L label = (L) bin.properties.get("LABEL");
            K id = bin.key.key();
            OUT product = bin.product;
            LOG.debug("The task for id={} label{} is ready.", id, label);
            innerConveyor.part().id(id).label(label).value(product).place();
        }).set();
        taskManager.scrapConsumer(bin->{
            K id = bin.key.key();
            AssemblingConveyor ac = (AssemblingConveyor) bin.properties.get("TASK_CONVEYOR");
            if(ac != null) {
                LOG.error("The task for id={} is about to be canceled. Reason: {}{}", id, bin.failureType, bin.error == null ? "" : " Error: "+bin.error.getMessage());
                ac.command().id(bin.key).cancel();
                ac.interrupt(ac.getName(), bin.key);
                innerConveyor.command().id(id).cancel(bin.error != null ? bin.error:new TimeoutException("Task Timed Out"));
            }
        }).set();
        taskManager.setDefaultCartConsumer(Conveyor.getConsumerFor(taskManager)
                .when("error",(b,v)->{
                    var tm = (TaskManager) b;
                    tm.error((Throwable) v);
                }).when("done",(b,v)->{
                    var tm = (TaskManager) b;
                    tm.done(v);
                })
        );
        taskManager.setName(conv.getName()+"_task_manager");
    }

    @Override
    public String toString() {
        return getName();
    }

    public TaskLoader<K,L> task() {
        return new TaskLoader<>(tl->{
            TaskId<K> id = new TaskId<>(tl.key, counter.incrementAndGet());
            int next = rr.next();
            var taskManagerFuture = taskManager.build()
                    .id(id)
                    .expirationTime(tl.expirationTime)
                    .addProperty("LABEL", tl.label)
                    .addProperty("TASK_CONVEYOR", conveyors[next])
                    .create();
            var taskFuture = loadersPool[next].id(id).value(tl.valueSupplier).place();
            LOG.debug("The task for id={} label={} has been scheduled.", tl.key, tl.label);
            return CompletableFuture.allOf(taskManagerFuture, taskFuture).thenApply(v-> taskManagerFuture.join() && taskFuture.join());
        });
    }

    @Override
    public CompletableFuture<Boolean> completeAndStop() {
        CompletableFuture<Boolean> future = innerConveyor.completeAndStop();
        return future.thenApply(f->{
            CompletableFuture<Boolean> tmFuture = taskManager.completeAndStop();
            List<CompletableFuture> list = Arrays.stream(conveyors).map(Conveyor::completeAndStop).toList();
            CompletableFuture<Boolean>[] futures = new CompletableFuture[list.size()+1];
            futures[0] = tmFuture;
            for(int i = 1; i < futures.length; i++) {
                futures[i] = list.get(i-1);
            }
            CompletableFuture<Boolean> combined = CompletableFuture.allOf(futures).thenApply(v -> {
                boolean res = true;
                for (int i = 0; i < futures.length; i++) {
                    res = res && futures[i].join();
                }
                return res;
            });
            return f && combined.join();
        });
    }

    @Override
    public void setName(String string) {
        super.setName(string);
        taskManager.setName("task_manager_"+string);
        for(int i = 0; i < conveyors.length; i++) {
            conveyors[i].setName("task_processor["+i+"]_"+string);
        }
    }

}