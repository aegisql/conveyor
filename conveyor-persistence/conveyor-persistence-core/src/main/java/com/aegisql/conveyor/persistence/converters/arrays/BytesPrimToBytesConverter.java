package com.aegisql.conveyor.persistence.converters.arrays;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class BytesPrimToBytesConverter implements ObjectConverter<byte[], byte[]> {

	@Override
	public byte[] toPersistence(byte[] obj) {
		return obj;
	}

	@Override
	public byte[] fromPersistence(byte[] p) {
		return p;
	}

}
