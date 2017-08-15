package com.aegisql.conveyor.persistence.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.apache.commons.io.output.ByteArrayOutputStream;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class EncryptingBlobConverter <T extends Serializable> extends BlobConverter<T> {

//	private final static Logger LOG = LoggerFactory.getLogger(EncryptingBlobConverter.class);
	
	private final SecretKey key;
	private final Cipher cipher;
	
	public EncryptingBlobConverter(Connection conn, SecretKey key, Cipher cipher) {
		super(conn);
		this.key = key;
		this.cipher = cipher;
	}
	
	@Override
	public Blob toPersistence(T obj) {
    	Blob blob       = null;
    	OutputStream os = null;
		try {
			blob = conn.createBlob();
	    	os = blob.setBinaryStream(1);
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		}
    	ObjectOutputStream oos;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			cipher.init(Cipher.ENCRYPT_MODE, key);
	    	oos.writeObject(obj);
	    	bos.close();
			byte[] encrypted = cipher.doFinal( bos.toByteArray() );
			os.write(encrypted);
			return blob;
		} catch (Exception e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

	@Override
	public T fromPersistence(Blob blb) {
		InputStream in = null;
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			in = blb.getBinaryStream(1, blb.length());
			byte[] bytes = IOUtils.toByteArray(in);
			bytes = cipher.doFinal(bytes);
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bis);
			T res = (T) ois.readObject();
			return res;
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ClassNotFound Runtime Exception",e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("InvalidKeyException Runntime Exception",e);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException("IllegalBlockSizeException Runntime Exception",e);
		} catch (BadPaddingException e) {
			throw new RuntimeException("BadPaddingException Runntime Exception",e);
		}
	}

}
