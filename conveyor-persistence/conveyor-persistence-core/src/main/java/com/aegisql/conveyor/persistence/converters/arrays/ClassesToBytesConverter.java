package com.aegisql.conveyor.persistence.converters.arrays;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class ClassesToBytesConverter implements ObjectArrayToByteArrayConverter<Class>{

	StringsToBytesConverter sc = new StringsToBytesConverter();
	
	public ClassesToBytesConverter() {
	}

	@Override
	public byte[] toPersistence(Class[] obj) {
		String[] names = new String[obj.length];
		for(int i = 0; i < obj.length; i++) {
			names[i] = obj[i].getName();
		}
		return sc.toPersistence(names);
	}

	@Override
	public Class[] fromPersistence(byte[] p) {
		String[] names = sc.fromPersistence(p);
		Class[] classes = new Class[names.length];
		
		for(int i = 0; i < names.length; i++) {
			String name = names[i];
			try {
				classes[i] = Class.forName(name);
			} catch (ClassNotFoundException e) {
				throw new PersistenceException(conversionHint()+" conversion error when converting '"+name+"'",e);
			}
		}
		
		return classes;
	}

	@Override
	public String conversionHint() {
		return "Class[]:byte[]";
	}

}
