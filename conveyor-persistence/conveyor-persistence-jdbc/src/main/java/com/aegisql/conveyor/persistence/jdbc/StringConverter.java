package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class StringConverter.
 *
 * @param <O> the generic type
 */
public abstract class StringConverter <O> implements ObjectConverter<O, String> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public String toPersistence(O obj) {
		if(obj == null) return null;
		return ""+obj;
	}

}
