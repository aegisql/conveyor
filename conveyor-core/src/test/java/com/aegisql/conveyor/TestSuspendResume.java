package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static com.aegisql.conveyor.user.UserBuilderEvents.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestSuspendResume {

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

	@Test
	public void suspendEnumTest() {
		assertThrows(AbstractMethodError.class,()->CommandLabel.SUSPEND.get());
	}
}
