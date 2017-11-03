package com.aegisql.conveyor.persistence.jdbc.converters;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.aegisql.conveyor.persistence.converters.ObjectToJsonStringConverter;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;

// TODO: Auto-generated Javadoc
/**
 * The Class BlobConverter.
 *
 */
public class MapToClobConverter implements ObjectConverter<Map<String,Object>, Clob> {

	
	/** The byte converter. */
	protected final ObjectToJsonStringConverter<Map> byteConverter = new ObjectToJsonStringConverter<>(Map.class);
	
	/** The conn. */
	protected final Connection conn;

	/**
	 * Instantiates a new blob converter.
	 *
	 * @param conn the conn
	 */
	public MapToClobConverter(Connection conn) {
		this.conn = conn;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public Clob toPersistence(Map<String,Object> obj) {
		try {
			Clob blob = conn.createClob();
			blob.setString(1, byteConverter.toPersistence(obj));
			return blob;
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception for "+obj,e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Map<String,Object> fromPersistence(Clob blb) {
		try {
			return byteConverter.fromPersistence( blb.getSubString(1, (int) blb.length()) );
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception",e);
		}
	}

	@Override
	public String conversionHint() {
		return "T:Clob";
	}

}
