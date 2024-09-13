package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Priority;
import com.aegisql.conveyor.cart.LoadType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TaskPoolConveyor <K,OUT> extends AssemblingConveyor<K,TaskManagerLabel<K,OUT>,OUT> {

    AssemblingConveyor<K,TaskHolder.Label,OUT>[] pool;
    RoundRobinLoop rr;

    public TaskPoolConveyor(int poolSize) {
        super(PriorityBlockingQueue::new);
        this.rr = new RoundRobinLoop(poolSize);
        this.setBuilderSupplier(()->new TaskManager<>(rr,pool,current_id,current_expiration_time, current_properties));
        this.addCartBeforePlacementValidator(cart->{
            Objects.requireNonNull(cart.getValue(),"Cart value is null");
            if(cart.getLabel() == TaskManager.Label.EXECUTE) {
                if (!(cart.getValue() instanceof Supplier)) {
                    throw new IllegalArgumentException("Value must be of a Supplier type, but is " + cart.getValue().getClass());
                }
            }
        });
        pool = new AssemblingConveyor[poolSize];
        for (int i = 0; i < poolSize; i++) {
            final int poolId = i;
            pool[i] = new AssemblingConveyor<>();
            pool[i].setBuilderSupplier(()->new TaskHolder(this,pool[poolId]));
            pool[i].resultConsumer(bin->{
                this.part().id(bin.key).label(TaskManager.ProtectedLabel.RESULT).value(bin.product).place();
            }).set();
            pool[i].scrapConsumer(scrap->{
                this.part().id(scrap.key).label(TaskManager.ProtectedLabel.ERROR).value(scrap).place();
            }).set();
        }
        this.setName("TaskPoolConveyor_"+ this.innerThread.threadId());
    }

    @Override
    public void setName(String name) {
        super.setName(name);
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


    /**
     * Gets the number of conveyors.
     *
     * @return the number of conveyors
     */
    public int getPoolSize() {
        return pool.length;
    }

    @Override
    public void stop() {
        Arrays.stream(pool).forEach(Conveyor::stop);
        super.stop();
    }

    @Override
    public CompletableFuture<Boolean> completeAndStop() {
        CompletableFuture<Boolean>[] completableFutures = new CompletableFuture[pool.length+1];
        for(int i = 0; i < completableFutures.length-1; i++) {
            completableFutures[i] = pool[i].completeAndStop();
        }
        var future = super.completeAndStop();
        completableFutures[pool.length] = future;

        return CompletableFuture.allOf(completableFutures).thenApply(v -> {
            List<Boolean> results = Arrays.stream(completableFutures)
                    .map(CompletableFuture::join)
                    .toList();
            return results.stream().allMatch(result -> result);
        });
    }

    @Override
    public String toString() {
        return getName()+"["+Arrays.toString(pool)+"]";
    }
}
