package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.user.User;

public class ResultQueueTest {

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
	public void testQueue() {
		ResultQueue<User> q = new ResultQueue<>();
		assertEquals(0, q.size());
		ConcurrentLinkedDeque<User> u = q.<ConcurrentLinkedDeque<User>>unwrap();
		assertNotNull(u);
		ProductBin<String, User> b1 = new ProductBin<String, User>("", new User("","",1999), 0, Status.READY);
		q.accept(b1);

		assertEquals(1, q.size());
		
		User u1 = q.poll();
		assertNotNull(u1);
		assertEquals(0, q.size());

		ProductBin<String, User> b2 = new ProductBin<String, User>("", new User("","",1999), 0, Status.READY);
		q.accept(b2);
		assertEquals(1, u.size());
		User u2 = u.poll();
		assertNotNull(u2);
		assertEquals(0, u.size());

		
	}

}
