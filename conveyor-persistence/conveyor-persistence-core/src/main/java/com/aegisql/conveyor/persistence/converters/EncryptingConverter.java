package com.aegisql.conveyor.persistence.converters;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class EncryptingConverter.
 */
public class EncryptingConverter implements ObjectConverter<byte[], byte[]> {

	/** The key. */
	private final SecretKey key;
	
	/** The cipher. */
	private final Cipher cipher;
	
	/**
	 * Instantiates a new encrypting converter.
	 *
	 * @param key the key
	 * @param cipher the cipher
	 */
	public EncryptingConverter(SecretKey key, Cipher cipher) {
		this.key    = key;
		this.cipher = cipher;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(byte[] obj) {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal( obj );
		} catch (Exception e) {
			throw new RuntimeException("Encryption Exception",e);
		}

	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public byte[] fromPersistence(byte[] p) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(p);
		} catch (Exception e) {
			throw new RuntimeException("Decryption Exception",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "byte[]:Encrypted(byte[])";
	}

}
