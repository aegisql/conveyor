package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class InputQueueLengthValidator <K,L> implements Consumer<Cart<K,?,L>> {

    private CompletableFuture<Boolean> prevFuture;
    private final IntSupplier queueSizeSupplier;
    private final int maxSize;
    private final Runnable waiting;

    public InputQueueLengthValidator(int maxSize, IntSupplier queueSizeSupplier) {
        this.queueSizeSupplier = queueSizeSupplier;
        this.maxSize = maxSize;
        this.waiting = ()->{
            prevFuture.join();
        };
    }

    public InputQueueLengthValidator(int maxSize, IntSupplier queueSizeSupplier, long maxTime, TimeUnit unit) {
        this.queueSizeSupplier = queueSizeSupplier;
        this.maxSize = maxSize;
        this.waiting = ()->{
            try {
                prevFuture.get(maxTime,unit);
            } catch (Exception e) {
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
                waiting.run();
            }
        }
        prevFuture = cart.getFuture();
    }
}
