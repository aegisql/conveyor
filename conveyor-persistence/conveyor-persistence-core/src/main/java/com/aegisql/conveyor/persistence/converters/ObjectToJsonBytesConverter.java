package com.aegisql.conveyor.persistence.converters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectToJsonBytesConverter <O> implements ObjectToByteArrayConverter<O> {

	private final Class<O> valueType;
	
	public ObjectToJsonBytesConverter(Class<O> valueType) {
		this.valueType = valueType;
	}
	
	
	@Override
	public byte[] toPersistence(O obj) {
		ObjectMapper om = new ObjectMapper();
		try( ByteArrayOutputStream os = new ByteArrayOutputStream() ) {
			om.writeValue(os, obj);
			return os.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed "+valueType.getCanonicalName()+" -> JSON conversion: "+obj,e);
		}
	}

	@Override
	public O fromPersistence(byte[] p) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.readValue(p, valueType);
		} catch (IOException e) {
			throw new RuntimeException("Failed JSON -> "+valueType.getCanonicalName()+" conversion: ",e);
		}
	}

}
