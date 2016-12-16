package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

public class BuilderSupplier2Test {

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
	public void test() {
		BuilderSupplier<User> bs = BuilderSupplier
				.of(UserBuilder::new)
				.expire(1000)
				.withFuture(new CompletableFuture<User>());
		assertNotNull(bs);
		Expireable ex = (Expireable)bs.get();
		assertEquals(1000, ex.getExpirationTime());
		FutureSupplier<User> fs = (FutureSupplier<User>)bs;
		assertNotNull(fs.getFuture());
	}

}
