package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.utils.PersistUtils;

public class UtilsTest {

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
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "test", "label");
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
		test.mkdirs();
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "test1", "label1");
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(2, "test2", "label2");
		String cartFile1 = "./testZip/test1.cart";
		PersistUtils.saveCart(cartFile1, c1);
		String cartFile2 = "./testZip/test2.cart";
		PersistUtils.saveCart(cartFile2, c2);
		PersistUtils.zipDirectory("testZip", "test.zip");
	}
	
	@Test
	public void testListBalancer() {
		Collection<Long> col = new ArrayList<>();
		
		for(int i = 0; i < 10_000; i++) {
			col.add(new Long(i));
		}
		
		Collection<Collection<Long>> b1 = PersistUtils.balanceIdList(col,1000);
		assertEquals(10, b1.size());
		col.add(new Long(0));
		Collection<Collection<Long>> b2 = PersistUtils.balanceIdList(col,1000);
		assertEquals(11, b2.size());
	}
	
}
