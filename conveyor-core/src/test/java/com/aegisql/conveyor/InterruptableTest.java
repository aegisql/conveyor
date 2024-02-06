package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterruptableTest {

	@BeforeAll
	public static void setUpBeforeClass() {
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
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
	
	@Test
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
		assertThrows(RuntimeException.class,()->f.join());
		last.getCurrent();
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
