package com.aegisql.conveyor.persistence.converters.arrays;

public class BytesToBytesConverter implements ObjectArrayToByteArrayConverter<Byte> {

	@Override
	public byte[] toPersistence(Byte[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[obj.length];
		for(int i=0; i< obj.length; i++){
			res[i] = obj[i].byteValue();
		}
		return res;
	}

	@Override
	public Byte[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Byte[] res = new Byte[p.length];
		for(int i=0; i< p.length; i++){
			res[i] = p[i];
		}
		return res;
	}
	
	@Override
	public String conversionHint() {
		return "Byte[]:byte[]";
	}


}
