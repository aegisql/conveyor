package com.aegisql.conveyor.persistence.converters.arrays.sql;

import java.nio.ByteBuffer;
import java.sql.Time;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class SqlTimesToBytesConverter implements ObjectConverter<Time[], byte[]> {

	@Override
	public byte[] toPersistence(Time[] obj) {
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
	public Time[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Time[] res = new Time[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = new Time(bb.getLong(8*i));
		}

		return res;
	}

}
