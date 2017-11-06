package com.aegisql.conveyor.persistence.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;

public class CartOutputStream <K,L> extends FilterOutputStream {
	
	private final CartToBytesConverter<K, ?, L> converter;
	
	private BigDecimal totalSize = BigDecimal.ZERO;

	public CartOutputStream(CartToBytesConverter<K, ?, L> converter, OutputStream out) {
		super(out);
		this.converter = converter;
	}
	
	public void writeCart(Cart<K,?,L> cart) throws IOException {
		byte[] bytes = converter.toPersistence((Cart)cart);
		write(bytes);
		flush();
		totalSize = totalSize.add(BigDecimal.valueOf( bytes.length ));
	}
	
	public BigDecimal getTotalWrittenBytes() {
		return totalSize;
	}

}
