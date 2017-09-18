package com.aegisql.conveyor.persistence.converters.arrays;

public class BooleansToBytesConverter implements ObjectArrayToByteArrayConverter<Boolean> {

	@Override
	public byte[] toPersistence(Boolean[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[obj.length];
		
		
		for(int i = 0; i < obj.length; i++) {
			res[i] = (byte) (obj[i].booleanValue() ? 1:0);
		}
		
		return res;
	}

	@Override
	public Boolean[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Boolean[] res = new Boolean[p.length];
		
		for(int i = 0; i < res.length; i++) {
			res[i] = p[i] != 0;
		}

		return res;
	}
	
	@Override
	public String conversionHint() {
		return "Boolean[]:byte[]";
	}


}
