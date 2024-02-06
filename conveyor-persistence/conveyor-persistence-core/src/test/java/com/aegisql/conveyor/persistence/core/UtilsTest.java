package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.utils.PersistUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

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
	public void testTmp() throws IOException {
		String tmp = PersistUtils.getTempDirectory();
		System.out.println(tmp);
		assertNotNull(tmp);
		FileUtils.deleteDirectory(new File(tmp));
		assertTrue(PersistUtils.createTempDirectory());
		PersistUtils.cleanTempDirectory();
	}

	@Test
	public void testSaveAndReadCarts() throws IOException, ClassNotFoundException {
		Cart<Integer,String,String> c1 = new ShoppingCart<>(1, "test", "label");
		System.out.println("C1="+c1);
		String cartFile = "./test.cart";
		FileUtils.deleteQuietly(new File(cartFile));
		System.out.println(cartFile);
		PersistUtils.saveCart(cartFile, c1);
		Cart<Integer,?,String> c2 = PersistUtils.readCart(cartFile);
		System.out.println("C2="+c2);
		assertEquals(c1.getKey(),c2.getKey());
		assertEquals(c1.getLabel(),c2.getLabel());
		assertEquals(c1.getValue(),c2.getValue());
		assertEquals(c1.getExpirationTime(),c2.getExpirationTime());

	}
	
	@Test
	public void testZip() throws Exception {
		FileUtils.deleteQuietly(new File("testZip"));
		File test = new File("testZip");
		assertTrue(test.mkdirs());
		Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "test1", "label1");
		Cart<Integer,String,String> c2;
		c2 = new ShoppingCart<>(2, "test2", "label2");
		String cartFile1 = "./testZip/test1.cart";
		PersistUtils.saveCart(cartFile1, c1);
		String cartFile2 = "./testZip/test2.cart";
		PersistUtils.saveCart(cartFile2, c2);
		PersistUtils.zipDirectory("testZip", "test.zip");
	}
	
	@Test
	public void testListBalancer() {
		Collection<Long> col = new ArrayList<>();
		
		for(long i = 0; i < 10_000; i++) {
			col.add(i);
		}
		
		Collection<Collection<Long>> b1 = PersistUtils.balanceIdList(col,1000);
		assertEquals(10, b1.size());
		col.add(0L);
		Collection<Collection<Long>> b2 = PersistUtils.balanceIdList(col,1000);
		assertEquals(11, b2.size());
	}
	
}
