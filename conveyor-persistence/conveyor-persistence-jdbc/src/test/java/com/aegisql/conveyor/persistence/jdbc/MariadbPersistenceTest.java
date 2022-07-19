package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;

//@Ignore
public class MariadbPersistenceTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("mariadb", Integer.class)
			.autoInit(true)
			.database("conveyor_maria_db")
			.user(Tester.getMariadbUser());
	
	@BeforeClass
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
		Assume.assumeTrue(Tester.testMariaDbConnection());
		Tester.removeLocalMariaDbDatabase("conveyor_maria_db");
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
	public void testWithDefaultArchivingStrategy() throws Exception {
		Persistence<Integer> p = persistenceBuilder
				.encryptionSecret("dfqejrfljheq")
				.idSupplier(Tester::nextId).build();//pb.build();
		assertNotNull(p);
		p.archiveAll();
		long id = p.nextUniquePartId();
		Cart<Integer,String,String> c = new ShoppingCart<Integer, String, String>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer,?,String> c2 = p.getPart(id);
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
		Persistence<Integer> p = persistenceBuilder
				.idSupplier(Tester::nextId)
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
		Persistence<Integer> p = persistenceBuilder
				.partTable("exp_test_del")
				.completedLogTable("exp_test_complete_del")
				.idSupplier(Tester::nextId)
				.setArchived()
				.build();
		assertNotNull(p);
		//p.archiveAll();

		long id1 = p.nextUniquePartId();
		long id2 = p.nextUniquePartId();

		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(11, "value1", "label",System.currentTimeMillis()+1000);
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(22, "value2", "label",System.currentTimeMillis()+100000);
		p.savePart(id1, c1);
		p.savePart(id2, c2);

		p.archiveExpiredParts();
		
		Cart<Integer,?,String> rc1 = p.getPart(id1);
		Cart<Integer,?,String> rc2 = p.getPart(id2);
		System.out.println(rc1);
		System.out.println(rc2);
		assertNotNull(rc1);
		assertNotNull(rc2);

		Thread.sleep(3000);
		p.archiveExpiredParts();

		Cart<Integer,?,String> rc12 = p.getPart(id1);
		Cart<Integer,?,String> rc22 = p.getPart(id2);
		assertNull(rc12);
		assertNotNull(rc22);
		
	}
	
	@Test
	public void testSaveAndRead() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("mariadb", Integer.class)
				.autoInit(true)
				.partTable("PART3")
				.completedLogTable("COMPLETED_LOG3")
				.deleteArchiving()
				.user(Tester.getMariadbUser())
				;
		
		JdbcPersistence<Integer> p = jpb.build();
		p.archiveAll();
		
		assertNotNull(p);
		Cart<Integer,String,String> cart = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cart.addProperty("ADDON","TEST");
		long id1 = p.nextUniquePartId();
		//long id2 = p.nextUniquePartId();
		//long id3 = p.nextUniquePartId();

		p.savePart(id1, cart);
		Cart restored = p.getPart(id1);
		assertNotNull(restored);
		System.out.println(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
	}


	
}
