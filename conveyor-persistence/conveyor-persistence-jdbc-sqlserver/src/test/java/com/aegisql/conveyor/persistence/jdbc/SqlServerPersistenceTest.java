package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SqlServerPersistenceTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("sqlserver", Integer.class)
			.autoInit(true)
			.database("conveyor_db_test")
			.user(Tester.getSqlServerUser())
			.password(Tester.getSqlServerPassword())
			.host(Tester.getSqlServerHost())
			.port(Tester.getSqlServerPort());

	@BeforeAll
	public static void setUpBeforeClass() {
		assumeTrue(Tester.testSqlServerConnection());
		Tester.removeLocalSqlServerDatabase("conveyor_db_test");
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
	public void testWithDefaultArchivingStrategy() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		Persistence<Integer> p = persistenceBuilder
				.encryptionSecret("dfqejrfljheq")
				.idSupplier(ids::incrementAndGet)
				.build();
		assertNotNull(p);
		p.archiveAll();
		long id = p.nextUniquePartId();
		assertEquals(1, id);
		id = p.nextUniquePartId();
		assertEquals(2, id);
		Cart<Integer, String, String> c = new ShoppingCart<>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer, ?, String> c2 = p.getPart(2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		assertNotNull(allIds);
		assertEquals(1, allIds.size());
		Collection<Cart<Integer, ?, String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1, allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
		p.close();
	}

	@Test
	public void testWithArchivedArchivingStrategy() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		Persistence<Integer> p = persistenceBuilder
				.idSupplier(ids::incrementAndGet)
				.setArchived()
				.build();
		assertNotNull(p);
		p.archiveAll();
		long id = p.nextUniquePartId();
		assertEquals(1, id);
		id = p.nextUniquePartId();
		assertEquals(2, id);
		Cart<Integer, String, String> c = new ShoppingCart<>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer, ?, String> c2 = p.getPart(2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		assertNotNull(allIds);
		assertEquals(1, allIds.size());
		Collection<Cart<Integer, ?, String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1, allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
		p.close();
	}

	@Test
	public void testDeleteExpired() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		Persistence<Integer> p = persistenceBuilder
				.partTable("exp_test")
				.completedLogTable("exp_test_complete")
				.idSupplier(ids::incrementAndGet)
				.setArchived()
				.build();
		assertNotNull(p);
		p.archiveAll();

		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "value1", "label", System.currentTimeMillis() + 1000);
		Cart<Integer, String, String> c2 = new ShoppingCart<>(2, "value2", "label", System.currentTimeMillis() + 100000);
		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);

		p.archiveExpiredParts();
		assertNotNull(p.getPart(1));
		assertNotNull(p.getPart(2));

		Thread.sleep(3000);
		p.archiveExpiredParts();

		assertNull(p.getPart(1));
		assertNotNull(p.getPart(2));
		p.close();
	}

	@Test
	public void testSaveAndRead() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = persistenceBuilder
				.partTable("PART2")
				.completedLogTable("COMPLETED_LOG2")
				.user(Tester.getSqlServerUser())
				.password(Tester.getSqlServerPassword());

		JdbcPersistence<Integer> p = jpb.build();
		assertNotNull(p);
		Cart<Integer, String, String> cart = new ShoppingCart<>(100, "test", "label");
		p.savePart(1, cart);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
		p.close();
	}
}
