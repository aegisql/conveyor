package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.utils.PersistFiles;

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
		String tmp = PersistFiles.getTempDirectory();
		System.out.println(tmp);
		assertNotNull(tmp);
		FileUtils.deleteDirectory(new File(tmp));
		assertTrue(PersistFiles.createTempDirectory());
		PersistFiles.cleanTempDirectory();
	}

	@Test
	public void testSaveAndReadCarts() throws IOException, ClassNotFoundException {
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "test", "label");
		System.out.println("C1="+c1);
		String cartFile = "./test.cart";
		FileUtils.deleteQuietly(new File(cartFile));
		System.out.println(cartFile);
		PersistFiles.saveCart(cartFile, c1);
		Cart<Integer,?,String> c2 = PersistFiles.readCart(cartFile);
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
		PersistFiles.saveCart(cartFile1, c1);
		String cartFile2 = "./testZip/test2.cart";
		PersistFiles.saveCart(cartFile2, c2);
		PersistFiles.zipDirectory("testZip", "test.zip");
	}
	
}
