package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.converters.arrays.LongPrimToBytesConverter;
import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class CartToBytesConverter <K,V,L> implements ObjectConverter<Cart<K,V,L>, byte[]> {

	/*
	  
Cart record format

	  4b - total cart record size
	  1b - cart type (enum order)
	  8b - creation time
	  8b - expiration time
	  
	  1b - key type length
	  kb - key type (string)
	  4b - key length
	  Kb - key
	  
	  1b - label type length
	  lb - label type (string)
	  4b - label length
	  Lb - label (can be enum order?)

	  1b - value type length
	  vb - valuel type (string)
	  4b - Value length
	  Vb - Value

	  4b - properties length
	  Pb - properties (JSON string)
	  
	 */
	
	private final LongPrimToBytesConverter longConverter           = new LongPrimToBytesConverter();
	private final StringToBytesConverter   stringConverter         = new StringToBytesConverter();
	private final EnumToBytesConverter<LoadType> loadTypeConverter = new EnumToBytesConverter<>(LoadType.class);
		
	private final ConverterAdviser<L> adviser;
	
	public CartToBytesConverter(ConverterAdviser<L> adviser) {
		this.adviser = adviser;
	}
	
	@Override
	public byte[] toPersistence(Cart<K, V, L> obj) {
		return null;
	}

	@Override
	public Cart<K, V, L> fromPersistence(byte[] p) {
		return null;
	}

	@Override
	public String conversionHint() {
		return null;
	}

}
