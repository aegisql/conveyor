package com.aegisql.conveyor.persistence.converters;

import java.io.IOException;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: Auto-generated Javadoc
/**
 * The Class ObjectToJsonBytesConverter.
 *
 * @param <O> the generic type
 */
public class ObjectToJsonBytesConverter <O> implements ObjectToByteArrayConverter<O> {

	/** The value type. */
	private final Class<O> valueType;
	
	/**
	 * Instantiates a new object to json bytes converter.
	 *
	 * @param valueType the value type
	 */
	public ObjectToJsonBytesConverter(Class<O> valueType) {
		this.valueType = valueType;
	}
	
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(O obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.writeValueAsBytes(obj);
		} catch (JsonProcessingException e) {
			throw new PersistenceException("Failed "+valueType.getCanonicalName()+" -> JSON conversion: "+obj,e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public O fromPersistence(byte[] p) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.readValue(p, valueType);
		} catch (IOException e) {
			throw new PersistenceException("Failed JSON -> "+valueType.getCanonicalName()+" conversion: ",e);
		}
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "JSON<"+valueType.getCanonicalName()+">:byte[]";
	}

}
