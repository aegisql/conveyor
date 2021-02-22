package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

public final class Priority {

    private final static int INITIAL_CAPACITY = 100;

    public final static Supplier<PriorityBlockingQueue<Cart>> FIFO = priorityQueueSupplier(Comparator.comparingLong(Cart::getCartCreationNanoTime));

    public final static Supplier<PriorityBlockingQueue<Cart>> FILO = priorityQueueSupplier((c1,c2)->Long.compare(c2.getCartCreationNanoTime(),c1.getCartCreationNanoTime()));

    public final static Supplier<PriorityBlockingQueue<Cart>> OLDEST_FIRST = priorityQueueSupplier((c1,c2)->{
        var ct1 = c1.getCreationTime();
        var ct2 = c2.getCreationTime();
        if(ct1==ct2) {
            return Long.compare(c1.getCartCreationNanoTime(),c2.getCartCreationNanoTime());
        } else {
            return Long.compare(ct1, ct2);
        }
    });

    public final static Supplier<PriorityBlockingQueue<Cart>> NEWEST_FIRST = priorityQueueSupplier((c1,c2)->{
        var ct1 = c1.getCreationTime();
        var ct2 = c2.getCreationTime();
        if(ct1==ct2) {
            return Long.compare(c1.getCartCreationNanoTime(),c2.getCartCreationNanoTime());
        } else {
            return Long.compare(ct2, ct1);
        }
    });

    public final static Supplier<PriorityBlockingQueue<Cart>> EXPIRE_SOONER_FIRST = priorityQueueSupplier((c1,c2)->{
        var et1 = c1.getExpirationTime();
        var et2 = c2.getExpirationTime();
        if(et1 == 0) et1 = Long.MAX_VALUE; //never
        if(et2 == 0) et2 = Long.MAX_VALUE; //never
        if(et1==et2) {
            return Long.compare(c1.getCartCreationNanoTime(),c2.getCartCreationNanoTime());
        } else {
            return Long.compare(et1, et2);
        }
    });

    public final static Supplier<PriorityBlockingQueue<Cart>> PRIORITIZED = PriorityBlockingQueue::new;

    public static Supplier<PriorityBlockingQueue<Cart>> priorityQueueSupplier(Comparator<Cart> comparator) {
        return ()->new PriorityBlockingQueue(INITIAL_CAPACITY,comparator);
    }

    public static Supplier<PriorityBlockingQueue<Cart>> prioritizedByProperty(String property) {
        return priorityQueueSupplier((c1,c2)->{
            var p1 = (Comparable)c1.getProperty(property,Object.class);
            var p2 = (Comparable)c2.getProperty(property,Object.class);
            if(p1 == null) p1 = Long.MIN_VALUE;
            if(p2 == null) p2 = Long.MIN_VALUE;
            var cmpRes = p2.compareTo(p1); //cart with higher priority go's first
            if(cmpRes==0) {
                cmpRes = Long.compare(c1.getCartCreationNanoTime(),c2.getCartCreationNanoTime()); //cart with same priority, first go's oldest
            }
            return cmpRes;
        });
    }

}
