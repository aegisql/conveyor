package com.aegisql.conveyor.persistence.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;

public class CartInputStream <K,L> extends InputStream {

	private final InputStream inputStream;
	private final CartToBytesConverter<K, ?, L> converter;
	
	public CartInputStream(CartToBytesConverter<K, ?, L> converter, InputStream inputStream) {
		this.converter   = converter;
		this.inputStream = inputStream;
	}
	
	@Override
	public int read() throws IOException {
		return inputStream.read();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return inputStream.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return inputStream.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return inputStream.skip(n);
	}

	@Override
	public int available() throws IOException {
		return inputStream.available();
	}

	@Override
	public void close() throws IOException {
		inputStream.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}

	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}

	public Cart<K,?,L> getCart() throws IOException {
		
		byte[] intBytes = new byte[4];
		
		int read = inputStream.read(intBytes);
	
		if(read != 4) {
			return null;
		}
		
		ByteBuffer bb = ByteBuffer.wrap(intBytes);
		
		int cartSize = bb.getInt();
		
		byte[] remainBytes = new byte[cartSize-4];
		read = inputStream.read(remainBytes);
		if(read != cartSize-4) {
			return null;
		}
		
		byte[] buff = new byte[cartSize];
		int pos = 0;
		for(int i = 0; i < 4; i++) {
			buff[pos++] = intBytes[i];
		}
		for(int i = 0; i < remainBytes.length; i++) {
			buff[pos++] = remainBytes[i];
		}
		
		return converter.fromPersistence(buff);		
	}

}
