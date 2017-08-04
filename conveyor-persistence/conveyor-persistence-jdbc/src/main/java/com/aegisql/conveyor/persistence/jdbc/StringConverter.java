package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public abstract class StringConverter <O> implements ObjectConverter<O, String> {

	@Override
	public String toPersistence(O obj) {
		return ""+obj;
	}

}
