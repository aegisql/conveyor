package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class InputQueueLengthValidator <K,L> implements Consumer<Cart<K,?,L>> {

    private final Logger LOG = LoggerFactory.getLogger(Conveyor.class);
    private CompletableFuture<Boolean> prevFuture;
    private K prevKey;
    private final IntSupplier queueSizeSupplier;
    private final int maxSize;
    private final Runnable waiting;
    public InputQueueLengthValidator(int maxSize, IntSupplier queueSizeSupplier) {
        this.queueSizeSupplier = queueSizeSupplier;
        this.maxSize = maxSize;
        this.waiting = ()-> prevFuture.join();
    }
    public InputQueueLengthValidator(int maxSize, IntSupplier queueSizeSupplier, long maxTime, TimeUnit unit) {
        this.queueSizeSupplier = queueSizeSupplier;
        this.maxSize = maxSize;
        this.waiting = ()->{
            try {
                LOG.debug("Waiting cart "+prevKey+" queue size "+queueSizeSupplier.getAsInt());
                prevFuture.get(maxTime,unit);
            } catch (Exception e) {
                LOG.debug("Rejected cart "+prevKey);
                prevFuture = null;
                prevKey = null;
                throw new ConveyorRuntimeException("InputQueueLengthValidator timeout exception",e);
            }
        };
    }
    public void reset() {
        prevFuture = null;
    }
    @Override
    public void accept(Cart<K, ?, L> cart) {
        if(prevFuture != null) {
            int size = queueSizeSupplier.getAsInt();
            if(size >= maxSize) {
                LOG.debug("Queue reached max size. key = {} done: {}",prevKey,prevFuture.isDone());
                waiting.run();
                prevFuture = cart.getFuture();
                prevKey = cart.getKey();
                LOG.debug("Queue drained. Re-setting key = {} done: {}",prevKey,prevFuture.isDone());
            }
        } else {
            prevFuture = cart.getFuture();
            prevKey = cart.getKey();
            LOG.debug("Set cart {}",prevKey);
        }
    }
}
