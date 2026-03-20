package com.aegisql.conveyor.persistence.encryption;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

final class AesGcmPayloadProtector implements PayloadProtector {

	static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static final byte[] MAGIC = new byte[] {'C', 'V', 'E', '1'};
	private static final int IV_LENGTH = 12;
	private static final int SALT_LENGTH = 16;
	private static final int TAG_LENGTH_BITS = 128;
	private static final int PBKDF2_ITERATIONS = 65_536;

	private final char[] passphrase;
	private final SecretKey secretKey;
	private final int keyLengthBytes;
	private final PayloadProtector legacyFallback;
	private final SecureRandom secureRandom = new SecureRandom();

	AesGcmPayloadProtector(String passphrase, SecretKey secretKey, int keyLengthBytes, PayloadProtector legacyFallback) {
		this.passphrase = passphrase == null ? null : passphrase.toCharArray();
		this.secretKey = secretKey;
		this.keyLengthBytes = keyLengthBytes;
		this.legacyFallback = legacyFallback;
	}

	@Override
	public byte[] encrypt(byte[] plainBytes) {
		try {
			byte[] salt = secretKey == null ? randomBytes(SALT_LENGTH) : new byte[0];
			byte[] iv = randomBytes(IV_LENGTH);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, resolveKey(salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainBytes);
			return ByteBuffer.allocate(MAGIC.length + 2 + salt.length + iv.length + encrypted.length)
					.put(MAGIC)
					.put((byte) salt.length)
					.put((byte) iv.length)
					.put(salt)
					.put(iv)
					.put(encrypted)
					.array();
		} catch (Exception e) {
			throw new PersistenceException("Encryption Exception", e);
		}
	}

	@Override
	public byte[] decrypt(byte[] protectedBytes) {
		if (!hasEnvelope(protectedBytes)) {
			if (legacyFallback != null) {
				return legacyFallback.decrypt(protectedBytes);
			}
			throw new PersistenceException("Unknown encrypted payload format");
		}
		try {
			ByteBuffer buffer = ByteBuffer.wrap(protectedBytes);
			buffer.position(MAGIC.length);
			int saltLength = buffer.get() & 0xFF;
			int ivLength = buffer.get() & 0xFF;
			if (protectedBytes.length < MAGIC.length + 2 + saltLength + ivLength + 1) {
				throw new PersistenceException("Corrupted encrypted payload envelope");
			}
			byte[] salt = new byte[saltLength];
			byte[] iv = new byte[ivLength];
			buffer.get(salt);
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, resolveKey(salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return cipher.doFinal(encrypted);
		} catch (PersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new PersistenceException("Decryption Exception", e);
		}
	}

	private SecretKey resolveKey(byte[] salt) throws Exception {
		if (secretKey != null) {
			return secretKey;
		}
		if (passphrase == null) {
			throw new PersistenceException("Encryption secret is not set");
		}
		if (keyLengthBytes != 16 && keyLengthBytes != 24 && keyLengthBytes != 32) {
			throw new PersistenceException("AES/GCM requires key length of 16, 24, or 32 bytes");
		}
		PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, keyLengthBytes * 8);
		try {
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			byte[] encoded = secretKeyFactory.generateSecret(spec).getEncoded();
			return new SecretKeySpec(encoded, "AES");
		} finally {
			spec.clearPassword();
		}
	}

	private byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		secureRandom.nextBytes(bytes);
		return bytes;
	}

	private boolean hasEnvelope(byte[] protectedBytes) {
		if (protectedBytes == null || protectedBytes.length < MAGIC.length + 2) {
			return false;
		}
		return Arrays.equals(MAGIC, Arrays.copyOf(protectedBytes, MAGIC.length));
	}

	@Override
	public String toString() {
		return "AesGcmPayloadProtector[" +
				"usesPassphrase=" + (passphrase != null) +
				", keyLengthBytes=" + keyLengthBytes +
				", hasLegacyFallback=" + (legacyFallback != null) +
				']';
	}
}
