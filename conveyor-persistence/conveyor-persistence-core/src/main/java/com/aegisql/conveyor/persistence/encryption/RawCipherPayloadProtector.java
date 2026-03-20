package com.aegisql.conveyor.persistence.encryption;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.util.Objects;

public final class RawCipherPayloadProtector implements PayloadProtector {

	private final SecretKey key;
	private final String transformation;

	public RawCipherPayloadProtector(SecretKey key, String transformation) {
		this.key = Objects.requireNonNull(key, "key");
		this.transformation = Objects.requireNonNull(transformation, "transformation");
	}

	@Override
	public byte[] encrypt(byte[] plainBytes) {
		return apply(Cipher.ENCRYPT_MODE, plainBytes, "Encryption Exception");
	}

	@Override
	public byte[] decrypt(byte[] protectedBytes) {
		return apply(Cipher.DECRYPT_MODE, protectedBytes, "Decryption Exception");
	}

	private byte[] apply(int mode, byte[] bytes, String message) {
		try {
			Cipher cipher = Cipher.getInstance(transformation);
			cipher.init(mode, key);
			return cipher.doFinal(bytes);
		} catch (Exception e) {
			throw new PersistenceException(message, e);
		}
	}

}
