package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class EnumToBytesConverter.
 *
 * @param <E> the element type
 */
public class EnumToBytesConverter <E extends Enum<E>> implements ObjectConverter<E, byte[]> {

	/** The map. */
	private final Class<E> en;
	/**
	 * Instantiates a new enum converter.
	 *
	 * @param en the en
	 */
	public EnumToBytesConverter(Class<E> en) {
		this.en = en;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(E obj) {
		if(obj == null) {
			return null;
		}
		int order = obj.ordinal();
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(order);
		return bytes;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public E fromPersistence(byte[] p) {
		if(p==null || p.length == 0) {
			return null;
		}
		return en.getEnumConstants()[ByteBuffer.wrap(p).getInt()];
	}

	@Override
	public String conversionHint() {
		return suggestHint(en);
	}

	public static <E extends Enum<E>> String suggestHint(Class<E> en) {
		return en.getSimpleName()+":byte[]";
	}
	
}
