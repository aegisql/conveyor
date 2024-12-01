package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;
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

    public final ScrapConsumer<K,?> ON_FAILURE_COMPLETE_EXCEPTIONALLY = bin->{
        bin.conveyor.command().id(bin.key).completeExceptionally(bin.error != null ? bin.error:new ConveyorRuntimeException("Task failed: "+bin.failureType+" "+bin.comment,bin.error));
    };

    public final ScrapConsumer<K,?> ON_FAILURE_KEEP_RUNNING = bin->{
        bin.conveyor.command().id(bin.key).completeExceptionally(new KeepRunningConveyorException("Task failed: "+bin.failureType+" "+bin.comment+". Keep running.",bin.error));
    };

    public final ScrapConsumer<K,?> ON_FAILURE_IGNORE = bin->{
        LOG.error("Task failed: {} {}. Error ignored.", bin.failureType, bin.comment);
    };

    public final ScrapConsumer<K,?> ON_FAILURE_PLACE_DEFAULT(L label, Object value) {
        return bin->bin.conveyor.part().id(bin.key).label(label).value(value).place();
    };

    private ScrapConsumer<K,?> taskScrapConsumer = ON_FAILURE_COMPLETE_EXCEPTIONALLY;

    private ScrapConsumer<K,?> getTaskScrapConsumer() {
        return taskScrapConsumer;
    }

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
                LOG.debug("The task {} has been completed. Product:{}", bin.key,bin.product);
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
        taskManager.resultConsumer(bin->{
            L label = (L) bin.properties.get("LABEL");
            K id = bin.key.key();
            OUT product = bin.product;
            LOG.debug("The task for id={} label={} is ready. Product: {}", id, label,product);
            this.part().id(id).label(label).value(product).place();
        })
                .set();
        taskManager.scrapConsumer(bin->{
            K id = bin.key.key();
            AssemblingConveyor tc = (AssemblingConveyor) bin.properties.get("TASK_CONVEYOR");
            if(tc != null) {
                LOG.error("The task for id={} is about to be canceled. Reason: {}{}", id, bin.failureType, bin.error == null ? "" : " Error: "+bin.error.getMessage());
                tc.command().id(bin.key).cancel();
                tc.interrupt(tc.getName(), bin.key);
            }
        })
                .andThen(bin->{
                    this.getTaskScrapConsumer().accept(mapScrapBin(bin));
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
                    .supplier(TaskManager::new)
                    .id(id)
                    .expirationTime(tl.expirationTime)
                    .addProperty("LABEL", tl.label)
                    .addProperty("TASK_CONVEYOR", conveyors[next])
                    .create();
            var taskFuture = loadersPool[next].id(id).value(tl.valueSupplier).place();
            LOG.debug("The task for id={} label={} has been scheduled in executor {}.", id, tl.label,next);
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
    public void stop() {
        super.stop();
        taskManager.stop();
        Arrays.stream(conveyors).forEach(Conveyor::stop);
    }

    @Override
    public void setName(String string) {
        super.setName(string);
        taskManager.setName("task_manager_"+string);
        for(int i = 0; i < conveyors.length; i++) {
            conveyors[i].setName("task_processor["+i+"]_"+string);
        }
    }

    private ScrapBin mapScrapBin(ScrapBin<TaskId<K>,?> bin) {
        return new ScrapBin((Conveyor<K, Object, Object>) innerConveyor,bin.key.key(),bin.scrap,bin.comment,bin.error,bin.failureType,bin.properties,bin.acknowledge.orElse(null));
    }

    public ScrapConsumerLoader<K> taskScrapConsumer() {
        return new ScrapConsumerLoader<>(sc -> {
            this.taskScrapConsumer = sc;
        }, this.taskScrapConsumer);
    }

    public ScrapConsumerLoader<K> taskScrapConsumer(ScrapConsumer<K,?> scrapConsumer) {
        return taskScrapConsumer().first(scrapConsumer);
    }


}