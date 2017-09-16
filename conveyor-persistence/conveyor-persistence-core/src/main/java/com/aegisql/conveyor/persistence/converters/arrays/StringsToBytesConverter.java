package com.aegisql.conveyor.persistence.converters.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.aegisql.conveyor.persistence.converters.StringToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

public class StringsToBytesConverter implements ObjectArrayToByteArrayConverter<String> {
	
	CollectionToByteArrayConverter<String> cc = new CollectionToByteArrayConverter<>(ArrayList::new, new StringToBytesConverter());

	@Override
	public byte[] toPersistence(String[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	@Override
	public String[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<String> c = cc.fromPersistence(p);
		return c.toArray(new String[0]);
	}

}
