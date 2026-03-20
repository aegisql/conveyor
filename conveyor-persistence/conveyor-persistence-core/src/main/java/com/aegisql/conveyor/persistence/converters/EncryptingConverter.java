package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.encryption.PayloadProtector;
import com.aegisql.conveyor.persistence.encryption.RawCipherPayloadProtector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.util.Objects;

// TODO: Auto-generated Javadoc
/**
 * The Class EncryptingConverter.
 */
public class EncryptingConverter implements ObjectConverter<byte[], byte[]> {

	private final PayloadProtector protector;
	
	public EncryptingConverter(SecretKey key, Cipher cipher) {
		this(new RawCipherPayloadProtector(key, cipher.getAlgorithm()));
	}

	public EncryptingConverter(PayloadProtector protector) {
		this.protector = Objects.requireNonNull(protector, "protector");
	}
	
	@Override
	public byte[] toPersistence(byte[] obj) {
		return protector.encrypt(obj);
	}

	@Override
	public byte[] fromPersistence(byte[] p) {
		return protector.decrypt(p);
	}

	@Override
	public String conversionHint() {
		return "byte[]:Encrypted(byte[])";
	}

}
