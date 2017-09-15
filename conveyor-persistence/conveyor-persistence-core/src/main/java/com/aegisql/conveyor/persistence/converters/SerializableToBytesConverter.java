package com.aegisql.conveyor.persistence.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public O fromPersistence(byte[] p) {
		ByteArrayInputStream bis  = new ByteArrayInputStream(p);
		try(ObjectInputStream ois = new ObjectInputStream(bis) ) {
			return (O) ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
