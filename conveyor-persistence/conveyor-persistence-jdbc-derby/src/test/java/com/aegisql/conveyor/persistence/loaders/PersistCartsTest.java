package com.aegisql.conveyor.persistence.loaders;

import com.aegisql.conveyor.cart.*;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.converters.StringConverter;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import com.aegisql.conveyor.serial.SerializablePredicate;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class PersistCartsTest {

	@BeforeAll
	public static void setUpBeforeClass() {
		Tester.removeDirectory("carts_db");
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
	
	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("derby", Integer.class)
			.database("carts_db").schema("carts_schema").autoInit(true).setArchived();

	
	public JdbcPersistenceBuilder<Integer> getPersistence(String table) {
		try {
			Thread.sleep(1000);

			return persistenceBuilder
					.partTable(table)
					.completedLogTable(table+"Completed")
					.labelConverter(new StringConverter<String>() {
						@Override
						public String fromPersistence(String p) {
							return p;
						}

						@Override
						public String conversionHint() {
							return "L:String";
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testShoppingCarts() throws Exception {
		Persistence<Integer> p = getPersistence("testShoppingCarts").build();
		p.archiveAll();

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART",0,0,10); 
		sc1.addProperty("PROPERTY","test");
		p.savePart(p.nextUniquePartId(), sc1);
		
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		
		assertEquals(1,allCarts.size());
		
		Cart<Integer, ?, String> scRestored = allCarts.iterator().next(); 
		
		assertEquals(sc1.getKey(), scRestored.getKey());
		assertEquals(sc1.getValue(), scRestored.getValue());
		assertEquals(sc1.getLabel(), scRestored.getLabel());
		assertEquals(sc1.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc1.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc1.getPriority(), scRestored.getPriority());
		assertEquals(10, scRestored.getPriority());
		assertEquals(sc1.getLoadType(), scRestored.getLoadType());
		assertEquals(sc1.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	@Test
	public void testShoppingCartFilters() throws Exception {
		Persistence<Integer> p = getPersistence("testShoppingCartFilters").addCartPersistenceFilter(c->{
			int id = c.getKey();
			return id > 10;
		}).build();
		p.archiveAll();

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART",0,0,10);
		ShoppingCart<Integer, String, String> sc2 = new ShoppingCart<Integer, String, String>(100, "sc1", "CART",0,0,10);
		sc1.addProperty("PROPERTY","test");
		sc2.addProperty("PROPERTY","test2");
		p.savePart(p.nextUniquePartId(), sc1);
		p.savePart(p.nextUniquePartId(), sc2);

		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();

		assertEquals(1,allCarts.size());

		Cart<Integer, ?, String> scRestored = allCarts.iterator().next();

		assertEquals(sc2.getKey(), scRestored.getKey());
		assertEquals(sc2.getValue(), scRestored.getValue());
		assertEquals(sc2.getLabel(), scRestored.getLabel());
		assertEquals(sc2.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc2.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc2.getPriority(), scRestored.getPriority());
		assertEquals(10, scRestored.getPriority());
		assertEquals(sc2.getLoadType(), scRestored.getLoadType());
		assertEquals(sc2.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	@Test
	public void testLabelFilters() throws Exception {
		Persistence<Integer> p = getPersistence("testLabelFilters").<String>addLabelPersistenceFilter(l->{
			return l.equalsIgnoreCase("BART");
		}).build();
		p.archiveAll();

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART",0,0,10);
		ShoppingCart<Integer, String, String> sc2 = new ShoppingCart<Integer, String, String>(100, "sc1", "BART",0,0,10);
		sc1.addProperty("PROPERTY","test");
		sc2.addProperty("PROPERTY","test2");
		p.savePart(p.nextUniquePartId(), sc1);
		p.savePart(p.nextUniquePartId(), sc2);

		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();

		assertEquals(1,allCarts.size());

		Cart<Integer, ?, String> scRestored = allCarts.iterator().next();

		assertEquals(sc2.getKey(), scRestored.getKey());
		assertEquals(sc2.getValue(), scRestored.getValue());
		assertEquals(sc2.getLabel(), scRestored.getLabel());
		assertEquals(sc2.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc2.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc2.getPriority(), scRestored.getPriority());
		assertEquals(10, scRestored.getPriority());
		assertEquals(sc2.getLoadType(), scRestored.getLoadType());
		assertEquals(sc2.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	@Test
	public void testLabelValueFilters() throws Exception {
		Persistence<Integer> p = getPersistence("testLabelValueFilters").<String,String>addLabelValuePersistenceFilter((l,v)->{
			return l.equalsIgnoreCase("BART") && v.equalsIgnoreCase("SC1");
		}).build();
		p.archiveAll();

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART",0,0,10);
		ShoppingCart<Integer, String, String> sc2 = new ShoppingCart<Integer, String, String>(100, "sc1", "BART",0,0,10);
		sc1.addProperty("PROPERTY","test");
		sc2.addProperty("PROPERTY","test2");
		p.savePart(p.nextUniquePartId(), sc1);
		p.savePart(p.nextUniquePartId(), sc2);

		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();

		assertEquals(1,allCarts.size());

		Cart<Integer, ?, String> scRestored = allCarts.iterator().next();

		assertEquals(sc2.getKey(), scRestored.getKey());
		assertEquals(sc2.getValue(), scRestored.getValue());
		assertEquals(sc2.getLabel(), scRestored.getLabel());
		assertEquals(sc2.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc2.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc2.getPriority(), scRestored.getPriority());
		assertEquals(10, scRestored.getPriority());
		assertEquals(sc2.getLoadType(), scRestored.getLoadType());
		assertEquals(sc2.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}


	@Test
	public void testMultyKeyCarts() throws Exception {
		Persistence<Integer> p = getPersistence("testMultyKeyCarts").build();
		p.archiveAll();

		MultiKeyCart<Integer, String, String> sc1 = new MultiKeyCart<Integer, String, String>((SerializablePredicate<Integer>)key->true, "sc1", "CART", 0, 0); 
		sc1.addProperty("PROPERTY","test");
		p.savePart(p.nextUniquePartId(), sc1);
		
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		
		assertEquals(1,allCarts.size());
		
		MultiKeyCart<Integer, String, String> scRestored = (MultiKeyCart<Integer, String, String>) allCarts.iterator().next(); 
		
		assertNull(scRestored.getKey());
		assertEquals(sc1.getValue(), scRestored.getValue());
		assertEquals(sc1.getLabel(), scRestored.getLabel());
		assertEquals(sc1.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc1.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc1.getLoadType(), scRestored.getLoadType());
		assertEquals(sc1.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
		System.out.println("---"+scRestored.getProperty("#CART_BUILDER",Object.class));
		assertTrue(scRestored.getValue().getFilter().test(1));
		
	}

	@Test
	public void testResultConsumerCarts() throws Exception {
		Persistence<Integer> p = getPersistence("testResultConsumerCarts").build();
		p.archiveAll();

		ResultConsumerCart<Integer, String, String> sc1 = new ResultConsumerCart<Integer, String, String>(1, bin->{
			System.out.println("TA-DA");
		},1, 2,0); //TODO: add priority?
		sc1.addProperty("PROPERTY","test");
		p.savePart(p.nextUniquePartId(), sc1);
		
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		
		assertEquals(1,allCarts.size());
		
		Cart<Integer, ?, String> scRestored = allCarts.iterator().next(); 
		
		assertEquals(sc1.getKey(), scRestored.getKey());
		ResultConsumer<Integer,String> rc = (ResultConsumer<Integer, String>) scRestored.getValue();
		assertNotNull(rc);
		rc.accept(null);
		assertEquals(sc1.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc1.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc1.getLoadType(), scRestored.getLoadType());
		//assertEquals(sc1.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	@Test
	public void testCreatingCarts() throws Exception {
		Persistence<Integer> p = getPersistence("testCreatingCarts").build();
		p.archiveAll();

		CreatingCart<Integer, Trio, String> sc1 = new CreatingCart<>(1, TrioBuilder::new,1, 2, 0); 
		sc1.addProperty("PROPERTY","test");
		p.savePart(p.nextUniquePartId(), sc1);
		
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		
		assertEquals(1,allCarts.size());
		
		Cart<Integer, ?, String> scRestored = allCarts.iterator().next(); 
		
		assertEquals(sc1.getKey(), scRestored.getKey());

		TrioBuilder tb = (TrioBuilder) sc1.getValue().get();
		
		assertNotNull(tb);
		assertEquals(sc1.getCreationTime(), scRestored.getCreationTime());
		assertEquals(sc1.getExpirationTime(), scRestored.getExpirationTime());
		assertEquals(sc1.getLoadType(), scRestored.getLoadType());
		//assertEquals(sc1.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	
	
}
