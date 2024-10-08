package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.utils.ConveyorAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of the Conveyor interface that wraps an inner conveyor instance.
 *
 * @param <K>   the key type
 * @param <OUT> the output type
 */
public class TaskPoolConveyor<K, OUT> extends ConveyorAdapter<K, TaskManagerLabel<K,OUT>, OUT> {

    private final int poolSize;
    private final AssemblingConveyor<K,TaskHolder.Label,OUT>[] pool;
    private final RoundRobinLoop rr;

    /**
     * Constructs a TaskPoolConveyor with the specified inner conveyor.
     *
     * @param poolSize the inner conveyor instance to wrap
     */
    public TaskPoolConveyor(int poolSize) {
        super(new AssemblingConveyor<>());
        AssemblingConveyor ac = (AssemblingConveyor) innerConveyor;
        AssemblingConveyorMBean mBeanInstance = (AssemblingConveyorMBean) ac.getMBeanInstance(ac.getName());
        this.poolSize = poolSize;
        this.pool = new AssemblingConveyor[poolSize];
        this.rr = new RoundRobinLoop(poolSize);
        this.setBuilderSupplier(()->new TaskManager<>(rr,pool,ac.current_id,ac.current_expiration_time, ac.current_properties));
        this.addBeforeKeyEvictionAction(evictionStatus->{
            K key = evictionStatus.getKey();
            Status status = evictionStatus.getStatus();
            switch (status) {
                case CANCELED -> {
                    LOG.info(key+" has been canceled");
                    Arrays.stream(pool).forEach(c->c.command().id(key).cancel());
                }
            }
        });
        for (int i = 0; i < poolSize; i++) {
            final int poolId = i;
            pool[i] = new AssemblingConveyor<>();
            pool[i].setBuilderSupplier(()->new TaskHolder<>(this,pool[poolId]));
            pool[i].resultConsumer(bin->{
                this.part().id(bin.key).label(TaskManager.ProtectedLabel.RESULT).value(bin.product).place();
            }).set();
            pool[i].scrapConsumer(scrap->{
                this.part().id(scrap.key).label(TaskManager.ProtectedLabel.ERROR).value(scrap).place();
            }).set();
        }
        this.setName("TaskPoolConveyor_"+mBeanInstance.getThreadId());
    }

    @Override
    public PartLoader<K, TaskManagerLabel<K, OUT>> part() {
        return innerConveyor.part().label(TaskManager.Label.EXECUTE);
    }

    /**
     * Gets the number of conveyors.
     *
     * @return the number of conveyors
     */
    public int getPoolSize() {
        return poolSize;
    }

    @Override
    public String toString() {
        return getName()+"["+Arrays.toString(pool)+"]";
    }

    @Override
    public void setName(String name) {
        innerConveyor.setName(name);
        for (int i = 0; i < pool.length; i++) {
            pool[i].setName(name+"_"+i);
        }
    }

    @Override
    public void interrupt(final String conveyorName) {
        Arrays.stream(pool).filter(c->c.getName().equals(conveyorName)).limit(1).forEach(c->c.interrupt(conveyorName));
    }

    @Override
    public void interrupt(String conveyorName, K key) {
        Arrays.stream(pool).filter(c->c.getName().equals(conveyorName)).limit(1).forEach(c->c.interrupt(conveyorName,key));
    }

    @Override
    public void stop() {
        Arrays.stream(pool).forEach(Conveyor::stop);
        innerConveyor.stop();
    }

    @Override
    public CompletableFuture<Boolean> completeAndStop() {
        CompletableFuture<Boolean>[] completableFutures = new CompletableFuture[pool.length+1];
        for(int i = 0; i < completableFutures.length-1; i++) {
            completableFutures[i] = pool[i].completeAndStop();
        }
        var future = innerConveyor.completeAndStop();
        completableFutures[pool.length] = future;

        return CompletableFuture.allOf(completableFutures).thenApply(v -> {
            List<Boolean> results = Arrays.stream(completableFutures)
                    .map(CompletableFuture::join)
                    .toList();
            return results.stream().allMatch(result -> result);
        });
    }

}