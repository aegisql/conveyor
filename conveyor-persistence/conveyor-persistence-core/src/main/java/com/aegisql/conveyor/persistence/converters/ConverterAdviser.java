package com.aegisql.conveyor.persistence.converters;

import java.util.HashMap;
import java.util.Map;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class ConverterAdviser <L> {

	private final Map<String,ObjectConverter<?, byte[]>> primeConverters = new HashMap<>();
	private final Map<L,ObjectConverter<?, byte[]>> labelConverters = new HashMap<>();
	
	private ObjectConverter<?, byte[]> defaultConverter = new SerializableToBytesConverter<>();
	
	public ConverterAdviser() {
		
	}
	
	public void addConverter(Class<?> clas,ObjectConverter<?, byte[]> conv) {
		primeConverters.put(clas.getCanonicalName(), conv);
		primeConverters.put(conv.getClass().getCanonicalName(), conv);
	}

	public void addConverter(L label,ObjectConverter<?, byte[]> conv) {
		labelConverters.put(label, conv);
		primeConverters.put(conv.getClass().getCanonicalName(), conv);
	}

	public ObjectConverter<?, byte[]> getConverter(L label, String name) {
		
		if(labelConverters.containsKey(label)) {
			return labelConverters.get(label);
		}
		
		ObjectConverter<?, byte[]> conv = primeConverters.get(name);
		if(conv != null) {
			return null;
		} else {
			return defaultConverter;
		}
	}

	public ObjectConverter<?, byte[]> getConverter(L label, Class<?> clas) {
		return getConverter(label,clas.getCanonicalName());
	}

	public ObjectConverter<?, byte[]> getDefaultConverter() {
		return defaultConverter;
	}

	public void setDefaultConverter(ObjectConverter<?, byte[]> defaultConverter) {
		this.defaultConverter = defaultConverter;
	}
	
	

}
