package com.aegisql.conveyor.persistence.jdbc;

import java.util.HashMap;
import java.util.Map;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class EnumConverter <E extends Enum<E>> implements ObjectConverter<E, String> {

	private Map<String,E> map = new HashMap<>();
	
	public EnumConverter(Class<E> en) {
		for(E el:en.getEnumConstants()) {
			map.put(""+el, el);
		}
	}
	
	@Override
	public String toPersistence(E obj) {
		return obj.toString();
	}

	@Override
	public E fromPersistence(String p) {
		return map.get(p);
	}

}
