package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;

public class EncryptingConverterBuilderTest {

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
	public void test() {

		EncryptingConverterBuilder ecb = new EncryptingConverterBuilder();
		
		EncryptingConverter ec1 = ecb.get();
		assertNull(ec1);

		EncryptingConverter ec2 = ecb
				.encryptionSecret("long secret word")
				.encryptionAlgorithm("AES")
				.encryptionTransformation("AES/ECB/PKCS5Padding")
				.encryptionKeyLength(16)
				.get();
		assertNotNull(ec2);
		
		String src = "TEST STRING";
		byte[] bytes = ec2.toPersistence(src.getBytes());
		String dest = new String(ec2.fromPersistence(bytes));
		assertNotEquals(src.getBytes(), bytes);
		assertEquals(src, dest);
	}

}
