package com.aegisql.conveyor.persistence.converters;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class EncryptingConverter implements ObjectConverter<byte[], byte[]> {

	private final SecretKey key;
	
	/** The cipher. */
	private final Cipher cipher;
	
	public EncryptingConverter(SecretKey key, Cipher cipher) {
		this.key    = key;
		this.cipher = cipher;
	}
	
	@Override
	public byte[] toPersistence(byte[] obj) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal( obj );
		} catch (Exception e) {
			throw new RuntimeException("Encryption Exception",e);
		}

	}

	@Override
	public byte[] fromPersistence(byte[] p) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(p);
		} catch (Exception e) {
			throw new RuntimeException("Decryption Exception",e);
		}
	}

	@Override
	public String conversionHint() {
		return "byte[]:Encrypted(byte[])";
	}

}
