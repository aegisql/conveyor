package com.aegisql.conveyor.persistence.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import com.aegisql.conveyor.persistence.jdbc.archive.FileArchiver;
import com.aegisql.conveyor.persistence.utils.CartInputStream;
import com.aegisql.conveyor.persistence.utils.DataSize;

public class FileArchiverTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			File dir = new File("./");
			
			Arrays.stream(dir.listFiles()).map(f->f.getName()).filter(f->(f.endsWith(".blog")||f.endsWith(".blog.zip"))).forEach(f->new File(f).delete());
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void binaryLogConfigTest() {

	BinaryLogConfiguration blc = BinaryLogConfiguration.builder()
			.bucketSize(1000)
			.maxFileSize(1, DataSize.KB)
			.partTableName("table")
			.path("."+File.separator+"test")
			.moveToPath("."+File.separator+"archive")
			.zipFile(true)
			.build();
	
	assertNotNull(blc);
	assertTrue(blc.isZipFile());
	assertEquals(1000, blc.getBucketSize());
	assertEquals(1024, blc.getMaxSize());
	assertEquals("."+File.separator+"test"+File.separator, blc.getPath());
	assertEquals("."+File.separator+"test"+File.separator+"table.blog", blc.getFilePath());
	assertTrue(blc.getStampedFilePath().startsWith("."+File.separator+"archive"+File.separator+"table.20"));
	assertTrue(blc.getStampedFilePath().endsWith(".blog"));
	
	}

	@Test
	public void binaryLogConfigWithDefaultsTest() {

	BinaryLogConfiguration blc = BinaryLogConfiguration.builder()
			.build();
	
	assertNotNull(blc);
	assertFalse(blc.isZipFile());
	assertEquals(100, blc.getBucketSize());
	assertEquals(Long.MAX_VALUE, blc.getMaxSize());
	assertEquals("."+File.separator, blc.getPath());
	assertEquals("."+File.separator+"part.blog", blc.getFilePath());
	assertTrue(blc.getStampedFilePath().startsWith("."+File.separator+"part.20"));
	assertTrue(blc.getStampedFilePath().endsWith(".blog"));
	
	}

	@Test
	public void FileArchiverTest() throws Exception {
		PersistTestImpl p = new PersistTestImpl();
		BinaryLogConfiguration blc = BinaryLogConfiguration.builder()
				.maxFileSize("200")
				.zipFile(true)
				.build();
		ConverterAdviser<?> adviser = new ConverterAdviser<>();
		FileArchiver<Integer> a = new FileArchiver<>( p.getEngine(),blc);
		a.setPersistence(p);
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "v1", "l1");
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(1, "v2", "l1");
		Cart<Integer,String,String> c3 = new ShoppingCart<Integer, String, String>(1, "v3", "l1");
		Cart<Integer,String,String> c4 = new ShoppingCart<Integer, String, String>(1, "v4", "l1",System.currentTimeMillis()-1);

		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);
		p.savePart(p.nextUniquePartId(), c3);
		p.savePart(p.nextUniquePartId(), c4);
		
		Collection<Long> ids = p.getAllPartIds(1);
		assertNotNull(ids);
		assertEquals(4, ids.size());
		
		a.archiveParts(ids);
		
		File f1 = new File("./part.blog");
		assertTrue(f1.exists());
		assertTrue(f1.length()<=200);
		
		FileInputStream fis = new FileInputStream(f1);
		CartInputStream<Integer, ?> cis = new CartInputStream<>(new CartToBytesConverter<>(adviser),fis);
		
		ArrayList<Cart> carts = new ArrayList<>();
		Cart c = null;
		do {
			c = cis.readCart();
			System.out.println("read cart: "+c);
			carts.add(c);
		} while(c != null);
		
		assertNotNull(carts.get(0));
		
		f1.delete();
		
	}

	@Test
	public void FileArchiverExpiredTest() throws Exception {
		PersistTestImpl p = new PersistTestImpl();
		BinaryLogConfiguration blc = BinaryLogConfiguration.builder()
				.partTableName("expired")
				.maxFileSize("180")
				.zipFile(false)
				.build();
		ConverterAdviser<?> adviser = new ConverterAdviser<>();
		FileArchiver<Integer> a = new FileArchiver<>( p.getEngine(),blc);
		a.setPersistence(p);
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "v1", "l1");
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(1, "v2", "l1");
		Cart<Integer,String,String> c3 = new ShoppingCart<Integer, String, String>(1, "v3", "l1");
		Cart<Integer,String,String> c4 = new ShoppingCart<Integer, String, String>(1, "v4", "l1",System.currentTimeMillis()-1);

		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);
		p.savePart(p.nextUniquePartId(), c3);
		p.savePart(p.nextUniquePartId(), c4);
		
		Collection<Long> ids = p.getAllPartIds(1);
		assertNotNull(ids);
		assertEquals(4, ids.size());
		
		a.archiveExpiredParts();
		
		File f1 = new File("./expired.blog");
		assertTrue(f1.exists());
		assertTrue(f1.length()<=180);
		
		FileInputStream fis = new FileInputStream(f1);
		CartInputStream<Integer, ?> cis = new CartInputStream<>(new CartToBytesConverter<>(adviser),fis);
		
		ArrayList<Cart> carts = new ArrayList<>();
		Cart c = null;
		do {
			c = cis.readCart();
			System.out.println("read cart: "+c);
			carts.add(c);
		} while(c != null);
		
		assertNotNull(carts.get(0));
		
		f1.delete();
		
	}

}
