package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import org.junit.Test;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class PrioritySupplierTest {

    @Test
    public void fifoShouldPreserveInitialOrderTest() {
        PriorityBlockingQueue<Cart> queue = Priority.FIFO.get();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",System.currentTimeMillis(),0,0);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(1,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(3,queue.poll().getKey());

    }

    @Test
    public void filoShouldReverseInitialOrderTest() {
        PriorityBlockingQueue<Cart> queue = Priority.FILO.get();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",System.currentTimeMillis(),0,0);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(3,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());

    }

    @Test
    public void oldestShouldBeFirstTest() {
        PriorityBlockingQueue<Cart> queue = Priority.OLDEST_FIRST.get();

        long now = System.currentTimeMillis();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now-5000,0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",now-10000,0,0);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(3,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());

    }

    @Test
    public void newestShouldBeFirstTest() {
        PriorityBlockingQueue<Cart> queue = Priority.NEWEST_FIRST.get();

        long now = System.currentTimeMillis();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now-5000,0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",now-10000,0,0);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(2,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());
        assertEquals(3,queue.poll().getKey());

    }

    @Test
    public void expireSoonerShouldBeFirstTest() {
        PriorityBlockingQueue<Cart> queue = Priority.EXPIRE_SOONER_FIRST.get();

        long now = System.currentTimeMillis();

        Cart<Integer,String,String> c4 = new ShoppingCart<>(4,"v4","l4",now,now+10_000,0);
        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now,now+100_000,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",now,now+10_000,0);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);
        queue.add(c4);

        assertEquals(4,queue.poll().getKey());//4 is older
        assertEquals(3,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());

    }

    @Test
    public void prioritizedOrderTest() {
        PriorityBlockingQueue<Cart> queue = Priority.PRIORITIZED.get();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",System.currentTimeMillis(),0,1);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",System.currentTimeMillis(),0,2);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",System.currentTimeMillis(),0,3);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(3,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());

    }

    @Test
    public void prioritizedByLongPropertyOrderTest() {
        PriorityBlockingQueue<Cart> queue = Priority.prioritizedByProperty("TEST_PRIORITY").get();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",System.currentTimeMillis(),0,0);

        c1.addProperty("TEST_PRIORITY",1L);
        c2.addProperty("TEST_PRIORITY",2L);
        c3.addProperty("TEST_PRIORITY",3L);

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(3,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());

    }

    @Test
    public void prioritizedByStringPropertyOrderTest() {
        PriorityBlockingQueue<Cart> queue = Priority.prioritizedByProperty("TEST_PRIORITY").get();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",System.currentTimeMillis(),0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",System.currentTimeMillis(),0,0);

        c1.addProperty("TEST_PRIORITY","A");
        c2.addProperty("TEST_PRIORITY","B");
        c3.addProperty("TEST_PRIORITY","C");

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(3,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(1,queue.poll().getKey());

    }

    class A {
        String val;
        public A(String val) {
            this.val = val;
        }
        public String toString() {
            return val;
        }
    }
    class ABuilder implements Supplier<A> {
        String val;
        public A get() {
            return new A(val);
        }
    }
    @Test
    public void testPropertyPriorityWithConveyor() {
        SimpleConveyor<Integer,A> c = new SimpleConveyor(Priority.prioritizedByProperty("TEST_PRIORITY"),ABuilder::new);
        c.suspend();
        ResultQueue<Integer, A> results = ResultQueue.of(c);
        c.resultConsumer(results).set();
        c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("val"));
        c.part().id(1).label("val").value("v1").addProperty("TEST_PRIORITY",1L).place();
        c.part().id(2).label("val").value("v2").addProperty("TEST_PRIORITY",2L).place();
        c.part().id(3).label("val").value("v3").addProperty("TEST_PRIORITY",2L).place();
        c.part().id(4).label("val").value("v4").addProperty("TEST_PRIORITY",4L).place();
        c.resume();
        c.completeAndStop().join();

        assertEquals("v4",results.poll().val);
        assertEquals("v2",results.poll().val);//2 is older
        assertEquals("v3",results.poll().val);
        assertEquals("v1",results.poll().val);
    }

}