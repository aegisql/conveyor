package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

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
    public void oldestInNanoTimeShouldBeFirstTest() {
        PriorityBlockingQueue<Cart> queue = Priority.OLDEST_FIRST.get();

        long now = System.currentTimeMillis();
        //same creation time, but nano resolution is different
        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now,0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",now,0,0);
        // put in reversed order
        queue.add(c3);
        queue.add(c2);
        queue.add(c1);
        // read in normal order
        assertEquals(1,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(3,queue.poll().getKey());

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
    public void newestShouldBeFirstWithNanoResolutionTest() {
        PriorityBlockingQueue<Cart> queue = Priority.NEWEST_FIRST.get();

        long now = System.currentTimeMillis();

        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now,0,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);
        Cart<Integer,String,String> c3 = new ShoppingCart<>(3,"v3","l3",now,0,0);

        queue.add(c3);
        queue.add(c2);
        queue.add(c1);

        assertEquals(1,queue.poll().getKey());
        assertEquals(2,queue.poll().getKey());
        assertEquals(3,queue.poll().getKey());

    }


    @Test
    public void expireSoonerShouldBeFirstTest() {
        PriorityBlockingQueue<Cart> queue = Priority.EXPIRE_SOONER_FIRST.get();

        long now = System.currentTimeMillis();

        Cart<Integer,String,String> c4 = new ShoppingCart<>(4,"v4","l4",now,now+10_000,0);
        Cart<Integer,String,String> c1 = new ShoppingCart<>(1,"v1","l1",now,now+100_000,0);
        Cart<Integer,String,String> c2 = new ShoppingCart<>(2,"v2","l2",now,0,0);//unexpireable
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

    static class A {
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

    static class B {
        String val1;
        String val2;
        public B(String val1, String val2) {
            this.val1 = val1;
            this.val2 = val2;
        }
        public String toString() {
            return val1+" "+val2;
        }
    }
    class BBuilder implements Supplier<B> {
        String val1;
        String val2;
        public B get() {
            return new B(val1, val2);
        }
    }

    @Test
    public void testExistingPriorityWithConveyor() {

        SimpleConveyor<Integer,B> c = new SimpleConveyor(Priority.EXISTING_BUILDS_FIRST,BBuilder::new);

        ResultQueue<Integer, B> results = ResultQueue.of(c);
        c.resultConsumer(results).set();
        c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted("val1","val2"));
        c.part().id(1).label("val1").value("v1-1").place().join(); //1 is created
        c.suspend();
        c.part().id(2).label("val1").value("v2-1").place(); //both 2s placed before last 1
        c.part().id(2).label("val2").value("v2-2").place();
        c.part().id(1).label("val2").value("v1-2").place(); //last 1
        assertEquals(3,c.getInputQueueSize());
        c.resume();
        c.completeAndStop().join();
        assertEquals(2,results.size());
        B r1 = results.poll();
        B r2 = results.poll();
        assertEquals("v1-1 v1-2",r1.toString());
        assertEquals("v2-1 v2-2",r2.toString());// 1 completed before 2
    }

    @Test
    public void valueOfReturnSuppliers() {
        assertNotNull(Priority.valueOf("fifo"));
        assertNotNull(Priority.valueOf("filo"));
        assertNotNull(Priority.valueOf("NEWEST_FIRST"));
        assertNotNull(Priority.valueOf("OLDEST_FIRST"));
        assertNotNull(Priority.valueOf("EXPIRE_SOONER_FIRST"));
        assertNotNull(Priority.valueOf("PRIORITIZED"));
    }

    @Test
    public void valueOfFails() {
        assertThrows(ConveyorRuntimeException.class,()->Priority.valueOf("superimportantfirst"));
    }

}