package com.aegisql.conveyor.persistence.converters.arrays.sql;

import java.nio.ByteBuffer;
import java.sql.Timestamp;

import com.aegisql.conveyor.persistence.converters.arrays.ObjectArrayToByteArrayConverter;

public class SqlTimestampsToBytesConverter implements ObjectArrayToByteArrayConverter<Timestamp> {

	@Override
	public byte[] toPersistence(Timestamp[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putLong(8*i, obj[i].getTime());
		}
		
		return res;
	}

	@Override
	public Timestamp[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Timestamp[] res = new Timestamp[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = new Timestamp(bb.getLong(8*i));
		}

		return res;
	}

	@Override
	public String conversionHint() {
		return "java.sql.Timestamp[]:byte[]";
	}

}
