package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptingConverterBuilderTest {

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
