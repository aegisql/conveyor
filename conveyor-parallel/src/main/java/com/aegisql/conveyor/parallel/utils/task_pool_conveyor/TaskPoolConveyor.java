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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * An implementation of the Conveyor interface that wraps an inner conveyor instance.
 *
 * @param <K>   the key type
 * @param <OUT> the output type
 */
public class TaskPoolConveyor<K, L, OUT> extends ConveyorAdapter<K, L, OUT> {

    // Logger for this class
    protected final static Logger LOG = LoggerFactory.getLogger(TaskPoolConveyor.class);
    // Constants for labels and task conveyor
    public static final String LABEL = "__LABEL__";
    public static final String TASK_CONVEYOR = "__TASK_CONVEYOR__";
    public static final String NEXT_ATTEMPT = "__NEXT_ATTEMPT__";

    // Inner conveyor for managing tasks
    private final Conveyor<TaskId<K>,String,OUT> taskManager = new AssemblingConveyor<>();
    // Pool of part loaders
    private final PartLoader<TaskId<K>, Integer>[] loadersPool;
    // Array of conveyors
    private final Conveyor[] conveyors;
    // Counter for task IDs
    private final AtomicLong counter = new AtomicLong();
    // Round-robin loop for distributing tasks
    private final RoundRobinLoop rr;

    // Name of the conveyor
    private String name;

    // Scrap consumer for handling task failures by completing exceptionally
    public final ScrapConsumer<K,?> ON_FAILURE_COMPLETE_EXCEPTIONALLY = bin->{
        bin.conveyor.command().id(bin.key).completeExceptionally(bin.error != null ? bin.error:new ConveyorRuntimeException("Task failed: "+bin.failureType+" "+bin.comment,bin.error));
    };

    // Scrap consumer for handling task failures by keeping the conveyor running
    public final ScrapConsumer<K,?> ON_FAILURE_KEEP_RUNNING = bin->{
        bin.conveyor.command().id(bin.key).completeExceptionally(new KeepRunningConveyorException("Task failed: "+bin.failureType+" "+bin.comment+". Keep running.",bin.error));
    };

    // Scrap consumer for handling task failures by ignoring the error
    public final ScrapConsumer<K,?> ON_FAILURE_IGNORE = bin->{
        LOG.error("Task failed: {} {}. Error ignored.", bin.failureType, bin.comment);
    };

    // Scrap consumer for handling task failures by placing a default value
    public final ScrapConsumer<K,?> ON_FAILURE_PLACE_DEFAULT(L label, Object value) {
        return bin->bin.conveyor.part().id(bin.key).label(label).value(value).addProperties(bin.properties).place();
    };

    // Default scrap consumer for tasks
    public ScrapConsumer<K,?> taskScrapConsumer = ON_FAILURE_COMPLETE_EXCEPTIONALLY;

    // Getter for the task scrap consumer
    private ScrapConsumer<K,?> getTaskScrapConsumer() {
        return taskScrapConsumer;
    }

    /**
     * Constructor with pool size.
     *
     * @param poolSize the size of the pool
     */
    public TaskPoolConveyor(int poolSize) {
        this(new AssemblingConveyor<>(), poolSize);
    }

    /**
     * Constructor with conveyor and pool size.
     *
     * @param conv the conveyor
     * @param poolSize the size of the pool
     */
    public TaskPoolConveyor(Conveyor<K,L,OUT> conv, int poolSize) {
        super(conv);
        loadersPool = new PartLoader[poolSize];
        conveyors = new Conveyor[poolSize];
        rr = new RoundRobinLoop(poolSize);
        for (int i = 0; i < poolSize; i++) {
            var tec = new AssemblingConveyor<TaskId<K>,Integer,OUT>(PriorityBlockingQueue::new);
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
            L label = (L) bin.properties.remove(LABEL);
            bin.properties.remove(TASK_CONVEYOR);
            bin.properties.remove(NEXT_ATTEMPT);
            K id = bin.key.key();
            OUT product = bin.product;
            LOG.debug("The task for id={} label={} is ready. Product: {}", id, label,product);
            this.part().id(id).label(label).value(product).addProperties(bin.properties).place();
        })
                .set();
        taskManager.scrapConsumer(bin->{
            K id = bin.key.key();
            AssemblingConveyor tc = (AssemblingConveyor) bin.properties.get(TASK_CONVEYOR);
            if(tc != null) {
                LOG.error("The task for id={} is about to be canceled. Reason: {}{} properties: {}", id, bin.failureType, bin.error == null ? "" : " Error: "+bin.error.getMessage(), bin.properties);
                tc.kill(bin.key);
            }
        }).andThen(bin->{
                    TaskLoader<K,?> attempt = (TaskLoader<K,?>) bin.properties.get(NEXT_ATTEMPT);
                    if(attempt != null) {
                        LOG.debug("Next attempt found for task {}. Will try again. ",bin.key);
                        attempt.placeAsynchronous();
                    } else {
                        this.getTaskScrapConsumer().accept(mapScrapBin(bin));
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
        this.setName(conv.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Creates a new TaskLoader.
     *
     * @return a new TaskLoader
     */
    public TaskLoader<K,L> task() {
        return new TaskLoader<>(tl->{
            if(tl.filter != null) {
                return innerConveyor.command().foreach(tl.filter).peekId(k->{
                    TaskId<K> id = new TaskId<>(k, counter.incrementAndGet());
                    int next = rr.next();
                    taskManager.build()
                            .supplier(TaskManager::new)
                            .id(id)
                            .expirationTime(tl.expirationTime)
                            .addProperty(LABEL, tl.label)
                            .addProperty(TASK_CONVEYOR, conveyors[next])
                            .addProperty(NEXT_ATTEMPT, tl.attempts > 1 ? tl.attempts(tl.attempts - 1) : null)
                            .addProperties(tl.getAllProperties())
                            .create();
                    loadersPool[next].id(id).value(tl.valueSupplier).place();
                    LOG.debug("The task for id={} label={} has been scheduled in executor {}.", id, tl.label, next);
                });
            } else {
                TaskId<K> id = new TaskId<>(tl.key, counter.incrementAndGet());
                int next = rr.next();
                var taskManagerFuture = taskManager.build()
                        .supplier(TaskManager::new)
                        .id(id)
                        .expirationTime(tl.expirationTime)
                        .addProperty(LABEL, tl.label)
                        .addProperty(TASK_CONVEYOR, conveyors[next])
                        .addProperty(NEXT_ATTEMPT, tl.attempts > 1 ? tl.attempts(tl.attempts - 1) : null)
                        .addProperties(tl.getAllProperties())
                        .create();
                var taskFuture = loadersPool[next].id(id).value(tl.valueSupplier).place();
                LOG.debug("The task for id={} label={} has been scheduled in executor {}.", id, tl.label, next);
                return CompletableFuture.allOf(taskManagerFuture, taskFuture).thenApply(v -> taskManagerFuture.join() && taskFuture.join());
            }
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
    public void setName(String name) {
        String oldName = this.name;
        innerConveyor.setName(name);
        this.name = "task_pool_"+name;
        taskManager.setName("task_manager_"+name);
        for(int i = 0; i < conveyors.length; i++) {
            conveyors[i].setName("task_processor["+i+"]_"+name);
        }
        try {
            //unregister old name
            Conveyor.unRegister(oldName);
        } catch (Exception e) {
            //Ignore. Might be already unregistered
        }
        this.setMbean(this.name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Maps a ScrapBin to a new ScrapBin with the inner conveyor.
     *
     * @param bin the original ScrapBin
     * @return the mapped ScrapBin
     */
    private ScrapBin mapScrapBin(ScrapBin<TaskId<K>,?> bin) {
        return new ScrapBin((Conveyor<K, Object, Object>) innerConveyor,bin.key.key(),bin.scrap,bin.comment,bin.error,bin.failureType,bin.properties,bin.acknowledge.orElse(null));
    }

    /**
     * Creates a new ScrapConsumerLoader for task scrap consumers.
     *
     * @return a new ScrapConsumerLoader
     */
    public ScrapConsumerLoader<K> taskScrapConsumer() {
        return new ScrapConsumerLoader<>(sc -> {
            this.taskScrapConsumer = sc;
        }, this.taskScrapConsumer);
    }

    /**
     * Sets the task scrap consumer.
     *
     * @param scrapConsumer the scrap consumer
     * @return a ScrapConsumerLoader
     */
    public ScrapConsumerLoader<K> taskScrapConsumer(ScrapConsumer<K,?> scrapConsumer) {
        return taskScrapConsumer().first(scrapConsumer);
    }

    @Override
    public Class<?> mBeanInterface() {
        return TaskPoolConveyorMBean.class;
    }

    /**
     * Registers the MBean for this conveyor.
     *
     * @param name the name of the MBean
     */
    protected void setMbean(String name) {
        final var thisConv = this;
        Conveyor.register(this, new TaskPoolConveyorMBean() {
            @Override
            public String getName() {
                return thisConv.name;
            }

            @Override
            public String getEnclosedConveyorName() {
                return thisConv.innerConveyor.getName();
            }

            @Override
            public int getPoolSize() {
                return thisConv.getPoolSize();
            }

            @Override
            public <K, L, OUT> Conveyor<K, L, OUT> conveyor() {
                return (Conveyor<K, L, OUT>) thisConv;
            }
        });
    }

    /**
     * Gets the size of the pool.
     *
     * @return the pool size
     */
    public int getPoolSize() {
        return conveyors.length;
    }

}