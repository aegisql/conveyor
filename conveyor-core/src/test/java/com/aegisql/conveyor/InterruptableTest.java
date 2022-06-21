package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.consumers.result.LastResultReference;

public class InterruptableTest {

	@BeforeClass
	public static void setUpBeforeClass() {
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	
	public static class Count {
		int count;
		@Override
		public String toString() {
			return "Count [" + count + "]";
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
	}
	
	public static class CountBuilder implements Supplier<Count> {

		private int count;
		
		@Override
		public Count get() {
			Count c = new Count();
			c.setCount(count);
			return c;
		}
	
		public static void add(CountBuilder b, int x) {
			for(int i = 0; i < x; i++) {
				b.count++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}

	public static class CountBuilderSmart implements Supplier<Count>, Interruptable {

		private int count;
		boolean continueLoop = true;
		
		@Override
		public Count get() {
			Count c = new Count();
			c.setCount(count);
			return c;
		}
	
		public static void add(CountBuilderSmart b, int x) {
			for(int i = 0; i < x; i++) {
				b.count++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				if( ! b.continueLoop ) {
					break;
				}
			}
		}

		@Override
		public void interrupt(Thread conveyorThread) {
			continueLoop = false;
			conveyorThread.interrupt(); //this has zero effect on the running thread. Not in the wait or sleep state
		}
		
	}

	
	SmartLabel<CountBuilder> ADD = SmartLabel.of(CountBuilder::add);
	SmartLabel<CountBuilderSmart> ADD_SMART = SmartLabel.of(CountBuilderSmart::add);
	
	@Test(expected=RuntimeException.class)
	public void testSimpleInterruption() throws InterruptedException {
		
		
		
		AssemblingConveyor<Integer, SmartLabel<CountBuilder> , Count> ac = new AssemblingConveyor<>();
		ac.setName("testInterrupt1");
		ac.setBuilderSupplier(CountBuilder::new);
		ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(ADD));
		
		LastResultReference<Integer, Count> last = new LastResultReference<>();
		
		ac.resultConsumer(last).set();
		
		CompletableFuture<Boolean> f = ac.part().id(1).label(ADD).value(10).place();
		Thread.sleep(1000);
		ac.interrupt(ac.getName());
		f.join();
		Count cnt = last.getCurrent();
		System.out.println(cnt);
		
	}

	@Test
	public void testSmartInterruption() throws InterruptedException {
		
		
		
		AssemblingConveyor<Integer, SmartLabel<CountBuilderSmart> , Count> ac = new AssemblingConveyor<>();
		ac.setName("testInterrupt1");
		ac.setBuilderSupplier(CountBuilderSmart::new);
		ac.setReadinessEvaluator(Conveyor.getTesterFor(ac).accepted(ADD_SMART));
		
		LastResultReference<Integer, Count> last = new LastResultReference<>();
		
		ac.resultConsumer(last).set();
		
		CompletableFuture<Boolean> f = ac.part().id(1).label(ADD_SMART).value(10).place();
		Thread.sleep(2000);
		ac.interrupt(ac.getName());
		f.join();
		Count cnt = last.getCurrent();
		System.out.println(cnt);
		assertTrue(cnt.count < 10);
		
	}

}
