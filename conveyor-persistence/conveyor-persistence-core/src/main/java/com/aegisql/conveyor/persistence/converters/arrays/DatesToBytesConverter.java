package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;
import java.util.Date;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class DatesToBytesConverter implements ObjectConverter<Date[], byte[]> {

	@Override
	public byte[] toPersistence(Date[] obj) {
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
	public Date[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Date[] res = new Date[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = new Date(bb.getLong(8*i));
		}

		return res;
	}

}
