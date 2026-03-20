package com.aegisql.conveyor.persistence.encryption;

import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.function.Supplier;

public class EncryptingConverterBuilder implements Supplier<EncryptingConverter> {

	private static final String LEGACY_DEFAULT_ALGORITHM = "AES";
	private static final String LEGACY_DEFAULT_TRANSFORMATION = "AES/ECB/PKCS5Padding";
	private static final int LEGACY_DEFAULT_KEY_LENGTH = 16;
	
	/** The encryption secret. */
	private final String encryptionSecret;
	
	/** The secret key. */
	private final SecretKey secretKey;
	
	/** The encryption algorithm. */
	private final String encryptionAlgorithm;
	
	/** The encryption transformation. */
	private final String encryptionTransformation;
	
	/** The encryption key length. */
	private final int encryptionKeyLength;

	public EncryptingConverterBuilder() {
		this(
			null
			,null
			,"AES"
			,AesGcmPayloadProtector.TRANSFORMATION
			,32);
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
		if (isManagedModernMode()) {
			return new EncryptingConverter(new AesGcmPayloadProtector(
					encryptionSecret,
					secretKey,
					encryptionKeyLength,
					buildLegacyDefaultFallback()
			));
		}
		return new EncryptingConverter(buildRawProtector(encryptionAlgorithm, encryptionTransformation, encryptionKeyLength));
	}

	private boolean isManagedModernMode() {
		return LEGACY_DEFAULT_ALGORITHM.equalsIgnoreCase(encryptionAlgorithm)
				&& AesGcmPayloadProtector.TRANSFORMATION.equalsIgnoreCase(encryptionTransformation);
	}

	private PayloadProtector buildLegacyDefaultFallback() {
		return buildRawProtector(LEGACY_DEFAULT_ALGORITHM, LEGACY_DEFAULT_TRANSFORMATION, LEGACY_DEFAULT_KEY_LENGTH);
	}

	private PayloadProtector buildRawProtector(String algorithm, String transformation, int keyLength) {
		try {
			return new RawCipherPayloadProtector(resolveLegacyKey(algorithm, keyLength), transformation);
		} catch (Exception e) {
			throw new PersistenceException("Encryption Builder error", e);
		}
	}

	private SecretKey resolveLegacyKey(String algorithm, int keyLength) throws Exception {
		if (secretKey != null) {
			return secretKey;
		}
		byte[] key = encryptionSecret.getBytes(StandardCharsets.UTF_8);
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		key = sha.digest(key);
		key = Arrays.copyOf(key, keyLength);
		return new SecretKeySpec(key, algorithm);
	}

}
