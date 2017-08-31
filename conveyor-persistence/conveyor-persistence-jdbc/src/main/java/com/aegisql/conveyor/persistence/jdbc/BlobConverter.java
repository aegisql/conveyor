package com.aegisql.conveyor.persistence.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class BlobConverter.
 *
 * @param <T> the generic type
 */
public class BlobConverter <T extends Serializable> implements ObjectConverter<T, Blob> {

	
	/** The byte converter. */
	protected final ByteArrayConverter<T> byteConverter = new ByteArrayConverter<>();
	
	/** The conn. */
	protected final Connection conn;

	/**
	 * Instantiates a new blob converter.
	 *
	 * @param conn the conn
	 */
	public BlobConverter(Connection conn) {
		this.conn = conn;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public Blob toPersistence(T obj) {
    	Blob blob       = null;
    	OutputStream os = null;
		try {
			blob = conn.createBlob();
	    	os = blob.setBinaryStream(1);
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception for "+obj,e);
		}
		try {
			os.write( byteConverter.toPersistence(obj));
			return blob;
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public T fromPersistence(Blob blb) {
		try(InputStream in = blb.getBinaryStream(1, blb.length())) {
			return byteConverter.fromPersistence( IOUtils.toByteArray(in) );
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

}
