package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class AssemblingConveyorTest {

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testBasics() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> c 
		= new AssemblingConveyor<>(
				    UserBuilder::new,
				    (lot, builder) -> {
					UserBuilder userBuilder = (UserBuilder) builder;
					System.out.println("Executing "+lot.label+"("+ +lot.key+") already done "+lot.previouslyAccepted);
					switch (lot.label) {
					case "setFirst":
						userBuilder.setFirst((String) lot.value);
						break;
					case "setLast":
						userBuilder.setLast((String) lot.value);
						break;
					case "setYearOfBirth":
						userBuilder.setYearOfBirth((Integer) lot.value);
						break;
					default:
						throw new RuntimeException("Unknown lot " + lot);
					}
					if(lot.previouslyAccepted == 2) {
						userBuilder.setReady(true);
					}
				},
				    res->{
				    	outQueue.add(res);
				    }
				    );

		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new Cart<>(1, "Doe", "setLast");
		Cart<Integer, String, String> c3 = new Cart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new Cart<>(1, 1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new Cart<>(3, 1999, "setBlah");

		c.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		c.offer(c2);
		c.offer(c3);
		c.offer(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		c.offer(c5);
		Thread.sleep(100);

		c.stop();
	}

	@Test
	public void testSmart() {
		UserBuilderSmart ub = new UserBuilderSmart();
		
		UserBuilderEvents.SET_FIRST.getSetter().accept(ub, "first");
		UserBuilderEvents.SET_LAST.getSetter().accept(ub, "last");
		UserBuilderEvents.SET_YEAR.getSetter().accept(ub, 1970);
		
		System.out.println(ub.build());
		
	}
	
}
