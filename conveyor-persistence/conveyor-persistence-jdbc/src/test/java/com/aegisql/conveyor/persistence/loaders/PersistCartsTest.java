package com.aegisql.conveyor.persistence.loaders;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceCart;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.jdbc.converters.StringConverter;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;
import com.aegisql.conveyor.serial.SerializableFunction;
import com.aegisql.conveyor.serial.SerializablePredicate;

public class PersistCartsTest {

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
	
	public Persistence<Integer> getPersistence(String table) {
		try {
			Thread.sleep(1000);

			return DerbyPersistence
					.forKeyClass(Integer.class)
					.schema("carts_db")
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
					})
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testShoppingCarts() {
		Persistence<Integer> p = getPersistence("testShoppingCarts");
		p.archiveAll();

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART"); 
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
		assertEquals(sc1.getLoadType(), scRestored.getLoadType());
		assertEquals(sc1.getProperty("PROPERTY", String.class), scRestored.getProperty("PROPERTY", String.class));
	}

	@Test
	public void testMultyKeyCarts() {
		Persistence<Integer> p = getPersistence("testMultyKeyCarts");
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

	
//	@Test
//	public void testPersistentKeyCarts() {
//		Persistence<Integer> p = getPersistence("testMultyKeyCarts");
//		p.archiveAll();
//
//		MultiKeyCart<Integer, String, String> sc1 = new MultiKeyCart<Integer, String, String>(key->true, "sc1", "CART", 0, 0); 
//		sc1.addProperty("PROPERTY","test");
//		
//		AcknowledgeBuildingConveyor<Integer> ab = new AcknowledgeBuildingConveyor<>(p, null, null);
//		PersistenceCart<Integer> pc1 = PersistenceCart.of(sc1,ab.CART);
//		
//		p.savePart(p.nextUniquePartId(), pc1);
//		
//		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
//		
//		assertEquals(1,allCarts.size());
//		
//		Cart<Integer, ?, String> scRestored = allCarts.iterator().next(); 
//		
//		assertNull(scRestored.getKey());
//		System.out.println(scRestored);
//	}

	@Test
	public void testResultConsumerCarts() {
		Persistence<Integer> p = getPersistence("testResultConsumerCarts");
		p.archiveAll();

		ResultConsumerCart<Integer, String, String> sc1 = new ResultConsumerCart<Integer, String, String>(1, bin->{
			System.out.println("TA-DA");
		},1, 2); 
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
	public void testCreatingCarts() {
		Persistence<Integer> p = getPersistence("testCreatingCarts");
		p.archiveAll();

		CreatingCart<Integer, Trio, String> sc1 = new CreatingCart<>(1, TrioBuilder::new,1, 2); 
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
