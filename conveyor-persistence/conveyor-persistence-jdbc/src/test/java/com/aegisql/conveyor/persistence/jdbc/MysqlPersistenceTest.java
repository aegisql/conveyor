package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;

public class MysqlPersistenceTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
			.autoInit(true)
			.user("root");
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assume.assumeTrue(Tester.testMySqlConnection());
		Tester.removeLocalMysqlDatabase("conveyor_db");
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
	public void testWithDefaultArchivingStrategy() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		
		Persistence<Integer> p = persistenceBuilder
				.encryptionSecret("dfqejrfljheq")
				.idSupplier(ids::incrementAndGet).build();//pb.build();
		assertNotNull(p);
		p.archiveAll();
		long id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(1, id);
		id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(2, id);
		Cart<Integer,String,String> c = new ShoppingCart<Integer, String, String>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer,?,String> c2 = p.getPart(2);
		System.out.println(c2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		System.out.println("all IDs "+allIds);
		assertNotNull(allIds);
		assertEquals(1,allIds.size());
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1,allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		System.out.println("Completed:"+completed);
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
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
		System.out.println("ID="+id);
		assertEquals(1, id);
		id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(2, id);
		Cart<Integer,String,String> c = new ShoppingCart<Integer, String, String>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer,?,String> c2 = p.getPart(2);
		System.out.println(c2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		System.out.println("all IDs "+allIds);
		assertNotNull(allIds);
		assertEquals(1,allIds.size());
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1,allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		System.out.println("Completed:"+completed);
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
	}

	
	@Test
	public void testDeleteExpired() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		Persistence<Integer> p = persistenceBuilder
				.partTable("exp_test")
				.completedLogTable("exp_test_complete")
				.idSupplier(ids::incrementAndGet)
				.setArchived().build();
		assertNotNull(p);
		p.archiveAll();
		
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "value1", "label",System.currentTimeMillis()+1000);
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(2, "value2", "label",System.currentTimeMillis()+100000);
		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);

		p.archiveExpiredParts();
		
		Cart<Integer,?,String> rc1 = p.getPart(1);
		Cart<Integer,?,String> rc2 = p.getPart(2);
		System.out.println(rc1);
		System.out.println(rc2);
		assertNotNull(rc1);
		assertNotNull(rc2);

		Thread.sleep(3000);
		p.archiveExpiredParts();

		Cart<Integer,?,String> rc12 = p.getPart(1);
		Cart<Integer,?,String> rc22 = p.getPart(2);
		assertNull(rc12);
		assertNotNull(rc22);
		
	}
	
	@Test
	public void testSaveAndRead() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
				.autoInit(true)
				.partTable("PART2")
				.completedLogTable("COMPLETED_LOG2")
				.user("root")
				.addField(String.class, "ADDON")
				.addUniqueFields("ADDON");
				;
		
		JdbcPersistence<Integer> p = jpb.build();
		
		assertNotNull(p);
		Cart<Integer,String,String> cartA = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartA.addProperty("ADDON", "A");
		Cart<Integer,String,String> cartB = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartB.addProperty("ADDON", "B");
		Cart<Integer,String,String> cartC = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartC.addProperty("ADDON", "A");
		p.savePart(1, cartA);
		p.savePart(2, cartB);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
		System.out.println(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
		try {
			p.savePart(3, cartC);
			fail("Must not be saved!");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


	
}
