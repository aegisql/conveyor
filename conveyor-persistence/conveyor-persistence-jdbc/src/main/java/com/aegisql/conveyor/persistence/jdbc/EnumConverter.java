package com.aegisql.conveyor.persistence.jdbc;

import java.util.HashMap;
import java.util.Map;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class EnumConverter.
 *
 * @param <E> the element type
 */
public class EnumConverter <E extends Enum<E>> implements ObjectConverter<E, String> {

	/** The map. */
	private Map<String,E> map = new HashMap<>();
	
	/**
	 * Instantiates a new enum converter.
	 *
	 * @param en the en
	 */
	public EnumConverter(Class<E> en) {
		for(E el:en.getEnumConstants()) {
			map.put(""+el, el);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public String toPersistence(E obj) {
		if(obj == null) return null;
		return obj.toString();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public E fromPersistence(String p) {
		return map.get(p);
	}

}
