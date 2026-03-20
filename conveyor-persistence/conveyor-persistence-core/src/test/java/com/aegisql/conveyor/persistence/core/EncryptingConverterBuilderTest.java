package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class EncryptingConverterBuilderTest {

	@Test
	void returnsNullWhenEncryptionIsNotConfigured() {
		assertNull(new EncryptingConverterBuilder().get());
	}

	@Test
	void encryptsAndDecryptsWithModernDefaultsFromSecret() {
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.encryptionSecret("long secret word")
				.get();

		assertNotNull(encryptor);

		String source = "TEST STRING";
		byte[] encrypted = encryptor.toPersistence(source.getBytes(StandardCharsets.UTF_8));
		String restored = new String(encryptor.fromPersistence(encrypted), StandardCharsets.UTF_8);

		assertNotEquals(source, new String(encrypted, StandardCharsets.ISO_8859_1));
		assertEquals(source, restored);
	}

	@Test
	void encryptsAndDecryptsWithModernDefaultsFromSecretKey() {
		SecretKey secretKey = new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.secretKey(secretKey)
				.get();

		byte[] encrypted = encryptor.toPersistence("payload".getBytes(StandardCharsets.UTF_8));

		assertEquals("payload", new String(encryptor.fromPersistence(encrypted), StandardCharsets.UTF_8));
	}

	@Test
	void rejectsTamperedModernCiphertext() {
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.encryptionSecret("tamper secret")
				.get();

		byte[] encrypted = encryptor.toPersistence("payload".getBytes(StandardCharsets.UTF_8));
		encrypted[encrypted.length - 1] ^= 0x01;

		PersistenceException error = assertThrows(PersistenceException.class, () -> encryptor.fromPersistence(encrypted));
		assertTrue(error.getMessage().contains("Decryption Exception"));
	}

	@Test
	void rejectsWrongSecretForModernCiphertext() {
		EncryptingConverter writer = new EncryptingConverterBuilder()
				.encryptionSecret("right secret")
				.get();
		EncryptingConverter reader = new EncryptingConverterBuilder()
				.encryptionSecret("wrong secret")
				.get();

		byte[] encrypted = writer.toPersistence("payload".getBytes(StandardCharsets.UTF_8));

		assertThrows(PersistenceException.class, () -> reader.fromPersistence(encrypted));
	}

	@Test
	void supportsLegacyExplicitCipherConfiguration() {
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.encryptionSecret("legacy secret")
				.encryptionAlgorithm("AES")
				.encryptionTransformation("AES/ECB/PKCS5Padding")
				.encryptionKeyLength(16)
				.get();

		byte[] encrypted = encryptor.toPersistence("legacy".getBytes(StandardCharsets.UTF_8));

		assertEquals("legacy", new String(encryptor.fromPersistence(encrypted), StandardCharsets.UTF_8));
	}

	@Test
	void modernDefaultDecryptsHistoricallyDefaultCiphertext() {
		EncryptingConverter legacyWriter = new EncryptingConverterBuilder()
				.encryptionSecret("compatible secret")
				.encryptionAlgorithm("AES")
				.encryptionTransformation("AES/ECB/PKCS5Padding")
				.encryptionKeyLength(16)
				.get();
		EncryptingConverter modernReader = new EncryptingConverterBuilder()
				.encryptionSecret("compatible secret")
				.get();

		byte[] encrypted = legacyWriter.toPersistence("legacy payload".getBytes(StandardCharsets.UTF_8));

		assertEquals("legacy payload", new String(modernReader.fromPersistence(encrypted), StandardCharsets.UTF_8));
	}

	@Test
	void failsFastOnInvalidModernKeyLength() {
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.encryptionSecret("invalid key length")
				.encryptionKeyLength(20)
				.get();

		PersistenceException error = assertThrows(PersistenceException.class,
				() -> encryptor.toPersistence("payload".getBytes(StandardCharsets.UTF_8)));
		assertTrue(error.getMessage().contains("Encryption Exception"));
	}

	@Test
	void sharedConverterIsSafeUnderConcurrentUse() throws Exception {
		EncryptingConverter encryptor = new EncryptingConverterBuilder()
				.encryptionSecret("parallel secret")
				.get();

		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Future<String>> futures = new ArrayList<>();
			for (int i = 0; i < 100; i++) {
				final int idx = i;
				futures.add(executor.submit(() -> {
					String payload = "payload-" + idx;
					byte[] encrypted = encryptor.toPersistence(payload.getBytes(StandardCharsets.UTF_8));
					return new String(encryptor.fromPersistence(encrypted), StandardCharsets.UTF_8);
				}));
			}
			for (int i = 0; i < futures.size(); i++) {
				assertEquals("payload-" + i, futures.get(i).get(30, TimeUnit.SECONDS));
			}
		} finally {
			executor.shutdownNow();
		}
	}
}
