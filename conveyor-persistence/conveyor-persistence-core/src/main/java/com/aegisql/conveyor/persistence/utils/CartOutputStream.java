package com.aegisql.conveyor.persistence.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;

public class CartOutputStream <K,L> extends FilterOutputStream {
	
	private final CartToBytesConverter<K, ?, L> converter;
	
	private long totalSize = 0;

	public CartOutputStream(CartToBytesConverter<K, ?, L> converter, OutputStream out) {
		super(out);
		this.converter = converter;
	}
	
	public long writeCart(Cart<K,?,L> cart) throws IOException {
		byte[] bytes = converter.toPersistence((Cart)cart);
		write(bytes);
		flush();
		totalSize += bytes.length;
		return bytes.length;
	}
	
	public long getTotalWrittenBytes() {
		return totalSize;
	}

}
