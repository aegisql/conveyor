package com.aegisql.conveyor.persistence.encryption;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class EncryptingConverterBuilder implements Supplier<EncryptingConverter> {
	
	/** The encryption secret. */
	private final String encryptionSecret;
	
	/** The secret key. */
	private final SecretKey secretKey;
	
	/** The encryption algorithm. */
	private final String encryptionAlgorithm;
	
	/** The encryption transformation. */
	private final String encryptionTransformation;
	
	/** The encryption key length. */
	private int encryptionKeyLength;

	public EncryptingConverterBuilder() {
		this(
			null
			,null
			,"AES"
			,"AES/ECB/PKCS5Padding"
			,16);
	}
	
	private EncryptingConverterBuilder(String encryptionSecret, SecretKey secretKey, String encryptionAlgorithm,
			String encryptionTransformation, int encryptionKeyLength) {
		this.encryptionSecret = encryptionSecret;
		this.secretKey = secretKey;
		this.encryptionAlgorithm = encryptionAlgorithm;
		this.encryptionTransformation = encryptionTransformation;
		this.encryptionKeyLength = encryptionKeyLength;
	}
	
	public EncryptingConverterBuilder encryptionSecret(String secret) {
		return new EncryptingConverterBuilder(secret,secretKey,encryptionAlgorithm,encryptionTransformation,encryptionKeyLength);
	}

	public EncryptingConverterBuilder secretKey(SecretKey secret) {
		return new EncryptingConverterBuilder(encryptionSecret,secret,encryptionAlgorithm,encryptionTransformation,encryptionKeyLength);
	}

	public EncryptingConverterBuilder encryptionAlgorithm(String algorithm) {
		return new EncryptingConverterBuilder(encryptionSecret,secretKey,algorithm,encryptionTransformation,encryptionKeyLength);
	}

	public EncryptingConverterBuilder encryptionTransformation(String transformation) {
		return new EncryptingConverterBuilder(encryptionSecret,secretKey,encryptionAlgorithm,transformation,encryptionKeyLength);
	}

	public EncryptingConverterBuilder encryptionKeyLength(int length) {
		return new EncryptingConverterBuilder(encryptionSecret,secretKey,encryptionAlgorithm,encryptionTransformation,length);
	}

	@Override
	public EncryptingConverter get() {
		if(encryptionSecret == null && secretKey == null) {
			return null;
		}
		try {
			Cipher encryptionCipher = Cipher.getInstance(encryptionTransformation);
			if(secretKey == null) {
				byte[] key = encryptionSecret.getBytes("UTF-8");
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				key = sha.digest(key);
				key = Arrays.copyOf(key, encryptionKeyLength);
				return new EncryptingConverter(new SecretKeySpec(key, encryptionAlgorithm), encryptionCipher);
			} else {
				return new EncryptingConverter(secretKey, encryptionCipher);
			}
		} catch (Exception e) {
			throw new PersistenceException("Encryption Builder error",e);
		}
	}

}
