package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.io.*;

// TODO: Auto-generated Javadoc
/**
 * The Class SerializableToBytesConverter.
 *
 * @param <O> the generic type
 */
public class SerializableToBytesConverter<O extends Serializable> implements ObjectToByteArrayConverter<O> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(O obj) {
		ByteArrayOutputStream bos  = new ByteArrayOutputStream();
		try(ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(obj);
			return bos.toByteArray();
		} catch (Exception e) {
			throw new PersistenceException("toPersistence failed serializing "+(obj==null?"null":obj.getClass().getName()),e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public O fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		ByteArrayInputStream bis  = new ByteArrayInputStream(p);
		try(ObjectInputStream ois = new ObjectInputStream(bis) ) {
			return (O) ois.readObject();
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Serializable:byte[]";
	}

}
