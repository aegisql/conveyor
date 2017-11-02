package com.aegisql.conveyor.persistence.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import org.apache.commons.io.IOUtils;

import com.aegisql.conveyor.persistence.core.PersistenceException;

// TODO: Auto-generated Javadoc
/**
 * The Class EncryptingBlobConverter.
 *
 * @param <T> the generic type
 */
public class EncryptingBlobConverter <T extends Serializable> extends BlobConverter<T> {

//	private final static Logger LOG = LoggerFactory.getLogger(EncryptingBlobConverter.class);
	
	/** The key. */
private final SecretKey key;
	
	/** The cipher. */
	private final Cipher cipher;
	
	/**
	 * Instantiates a new encrypting blob converter.
	 *
	 * @param conn the conn
	 * @param key the key
	 * @param cipher the cipher
	 */
	public EncryptingBlobConverter(Connection conn, SecretKey key, Cipher cipher) {
		super(conn);
		this.key = key;
		this.cipher = cipher;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.BlobConverter#toPersistence(java.io.Serializable)
	 */
	@Override
	public Blob toPersistence(T obj) {
    	Blob blob       = null;
    	OutputStream os = null;
		try {
			blob = conn.createBlob();
	    	os = blob.setBinaryStream(1);
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception",e);
		}
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encrypted = cipher.doFinal( byteConverter.toPersistence(obj) );
			os.write(encrypted);
			return blob;
		} catch (Exception e) {
			throw new PersistenceException("IO Runntime Exception",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.BlobConverter#fromPersistence(java.sql.Blob)
	 */
	@Override
	public T fromPersistence(Blob blb) {
		
		try(InputStream in = blb.getBinaryStream(1, blb.length())) {
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] bytes = IOUtils.toByteArray(in);
			bytes = cipher.doFinal(bytes);
			return byteConverter.fromPersistence(bytes);
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new PersistenceException("IO Runntime Exception",e);
		} catch (InvalidKeyException e) {
			throw new PersistenceException("InvalidKeyException Runntime Exception",e);
		} catch (IllegalBlockSizeException e) {
			throw new PersistenceException("IllegalBlockSizeException Runntime Exception",e);
		} catch (BadPaddingException e) {
			throw new PersistenceException("BadPaddingException Runntime Exception",e);
		}
	}

}
