package com.aegisql.conveyor.persistence.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class BlobConverter <T extends Serializable> implements ObjectConverter<T, Blob> {

	private final Connection conn;

	public BlobConverter(Connection conn) {
		this.conn = conn;
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
			oos = new ObjectOutputStream(os);
	    	oos.writeObject(obj);
			return blob;
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

	@Override
	public T fromPersistence(Blob blb) {
		InputStream in = null;
		try {
			in = blb.getBinaryStream(1, blb.length());
			ObjectInputStream ois = new ObjectInputStream(in);
			return (T) ois.readObject();
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("ClassNotFound Runtime Exception",e);
		}
	}

}
