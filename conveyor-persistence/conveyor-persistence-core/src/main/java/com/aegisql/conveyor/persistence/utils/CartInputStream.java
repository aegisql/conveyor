package com.aegisql.conveyor.persistence.utils;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class CartInputStream <K,L> extends FilterInputStream {

	private final CartToBytesConverter<K, ?, L> converter;
	
	public CartInputStream(CartToBytesConverter<K, ?, L> converter, InputStream inputStream) {
		super(inputStream);
		this.converter   = converter;
	}
	

	public Cart<K,?,L> readCart() throws IOException {
		
		byte[] intBytes = new byte[4];
		
		int read = read(intBytes);
	
		if(read != 4) {
			return null;
		}
		
		ByteBuffer bb = ByteBuffer.wrap(intBytes);
		
		int cartSize = bb.getInt();
		
		byte[] remainBytes = new byte[cartSize-4];
		read = read(remainBytes);
		if(read != cartSize-4) {
			return null;
		}
		
		byte[] buff = new byte[cartSize];
		int pos = 0;
		for(int i = 0; i < 4; i++) {
			buff[pos++] = intBytes[i];
		}
		for (byte remainByte : remainBytes) {
			buff[pos++] = remainByte;
		}
		
		return converter.fromPersistence(buff);		
	}

}
