package com.aegisql.conveyor.consumers.scrap;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.*;

public class ScrapQueueTest {

    @Test
    public void constructorTest() {
        ScrapQueue sq1 = new ScrapQueue();
        ScrapQueue sq2 = new ScrapQueue(new ConcurrentLinkedDeque());
        ScrapQueue sq3 = new ScrapQueue(ConcurrentLinkedDeque::new);
        assertTrue(sq1.isEmpty());
        assertFalse(sq1.contains("X"));
        assertNotNull(sq1.iterator());
        assertNotNull(sq1.toArray());
        assertNotNull(sq1.toArray(new Object[]{}));
        assertFalse(sq1.remove("X"));
        assertFalse(sq1.containsAll(Arrays.asList("X")));
        assertFalse(sq1.removeAll(Arrays.asList("X")));
        assertFalse(sq1.retainAll(Arrays.asList("X")));
        assertNull(sq1.poll());
        assertNull(sq1.peek());
        sq1.accept(ScrapConsumerTest.getScrapBin(1,"test queue"));
        assertEquals("test queue",sq1.element());
        assertNotNull(sq1.getInnerQueue());
        assertNotNull(sq1.remove());
        sq1.clear();

    }

    @Test
    public void failingAddAllTest() {
        ScrapQueue sq = new ScrapQueue();
        assertThrows(RuntimeException.class,()->sq.addAll(null));
    }

    @Test
    public void failingAddTest() {
        ScrapQueue sq = new ScrapQueue();
        assertThrows(RuntimeException.class,()->sq.add(null));
    }

    @Test
    public void failingOfferTest() {
        ScrapQueue sq = new ScrapQueue();
        assertThrows(RuntimeException.class,()->sq.offer(null));
    }

    @Test
    public void ofTests() {
        ScrapQueue sq1 = ScrapQueue.of(null);
        ScrapQueue sq2 = ScrapQueue.of(null,new ConcurrentLinkedDeque());
        ScrapQueue sq3 = ScrapQueue.of(null, ConcurrentLinkedDeque::new);
        assertNotNull(sq1);
        assertNotNull(sq2);
        assertNotNull(sq3);
    }

}