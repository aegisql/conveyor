package com.aegisql.conveyor.persistence.loaders;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.StringConverter;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;

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

		ShoppingCart<Integer, String, String> sc1 = new ShoppingCart<Integer, String, String>(1, "sc1", "CART"); 
		
		p.savePart(p.nextUniquePartId(), sc1);
		
	}

}
