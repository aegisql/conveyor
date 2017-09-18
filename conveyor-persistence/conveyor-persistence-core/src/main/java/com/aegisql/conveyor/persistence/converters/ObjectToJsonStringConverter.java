package com.aegisql.conveyor.persistence.converters;

import java.io.IOException;

import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectToJsonStringConverter <O> implements ObjectConverter<O, String> {

	private final Class<O> valueType;
	
	public ObjectToJsonStringConverter(Class<O> valueType) {
		this.valueType = valueType;
	}
	
	
	@Override
	public String toPersistence(O obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed "+valueType.getCanonicalName()+" -> JSON String conversion: "+obj,e);
		}
	}

	@Override
	public O fromPersistence(String p) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.readValue(p, valueType);
		} catch (IOException e) {
			throw new RuntimeException("Failed JSON String -> "+valueType.getCanonicalName()+" conversion: ",e);
		}
	}


	@Override
	public String conversionHint() {
		return "JSON<"+valueType.getCanonicalName()+">:byte[]";
	}

}
