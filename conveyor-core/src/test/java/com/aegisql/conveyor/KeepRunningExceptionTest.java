package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import org.junit.*;

import java.util.function.Supplier;

import static org.junit.Assert.*;

public class KeepRunningExceptionTest {
	
	public static class ObjectBuilder implements Supplier<Object>, Testing{

		private static final long serialVersionUID = 1L;
		private boolean ready = false;
		private int x = 0;
		
		@Override
		public Object get() {
			return Integer.valueOf(x);
		}
		
		public static void action(ObjectBuilder b, Integer i) {
			switch (i) {
			case 0:
				throw new RuntimeException("0 value!");
			case 1:
				b.x++;
				throw new KeepRunningConveyorException("1 value!");
			default:
				b.ready = true;
				b.x++;
				break;
			}
		}

		@Override
		public boolean test() {
			return ready;
		}
		
	}

	SmartLabel<ObjectBuilder> SET = SmartLabel.of("SET", ObjectBuilder::action);
	
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

	@Test
	public void test() throws InterruptedException {
		
		AssemblingConveyor<Integer, SmartLabel<ObjectBuilder>, Object> c = new AssemblingConveyor<>();
		c.setBuilderSupplier(ObjectBuilder::new);
		
		LastResultReference<Integer, Object> lastResultConsumer = new LastResultReference<>();
		LastScrapReference<Integer> lastScrap = new LastScrapReference<>();
		
		c.resultConsumer(lastResultConsumer).set();
		c.scrapConsumer(lastScrap).set();
		
		c.part().id(1).value(10).label(SET).place().join();
		System.out.println(lastResultConsumer);
		
		assertEquals(1, lastResultConsumer.getCurrent());
		
		c.part().id(1).value(0).label(SET).place();
		assertFalse(c.command().id(1).check().join());
		System.out.println(lastScrap);

		c.part().id(1).value(1).label(SET).place();
		c.part().id(1).value(1).label(SET).place();
		Thread.sleep(10);
		assertTrue(c.command().id(1).check().join());
		System.out.println(lastScrap);
		c.part().id(1).value(10).label(SET).place().join();
		System.out.println(lastResultConsumer);
		assertEquals(3, lastResultConsumer.getCurrent());
		
	}

}
