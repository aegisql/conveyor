package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;
import org.junit.*;

import java.util.concurrent.TimeUnit;

import static com.aegisql.conveyor.user.UserBuilderEvents.*;
import static org.junit.Assert.*;

public class TestSuspendResume {

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
	public void testSuspendResume() {
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(SET_FIRST,SET_LAST,SET_YEAR));
		conveyor.setName("SuspendableConveyor");
		assertFalse(conveyor.isSuspended());
		conveyor.suspend();
		assertTrue(conveyor.isSuspended());
		conveyor.part().id(1).label(UserBuilderEvents.SET_FIRST).value("A").place();
		conveyor.part().id(1).label(UserBuilderEvents.SET_LAST).value("B").place();
		conveyor.part().id(1).label(UserBuilderEvents.SET_YEAR).value(2000).place();

		conveyor.part().id(2).label(UserBuilderEvents.SET_FIRST).value("C").place();
		conveyor.part().id(2).label(UserBuilderEvents.SET_LAST).value("D").place();
		try {
			conveyor.part().id(2).label(UserBuilderEvents.SET_YEAR).value(2010).place().get(1,TimeUnit.SECONDS);
			fail("Must not reach this in suspended conveyor");
		} catch (Exception e) {
		} finally {
			assertEquals(0,conveyor.getCollectorSize());
			assertEquals(6,conveyor.getInputQueueSize());
			conveyor.resume();
		}
		conveyor.completeAndStop().join();
		assertEquals(2, outQueue.size());
	}

	@Test(expected = AbstractMethodError.class)
	public void suspendEnumTest() {
		CommandLabel.SUSPEND.get();
	}
}
