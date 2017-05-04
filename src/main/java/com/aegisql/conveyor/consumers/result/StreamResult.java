package com.aegisql.conveyor.consumers.result;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class StreamResult <K,V> implements Consumer<ProductBin<K,V>>, Closeable {

	private final ObjectOutputStream os;
	
	private final Function<ProductBin<K,V>,Object> transform;
	
	public StreamResult(OutputStream os) throws IOException {
		this(os,bin->bin.product);
	}

	public StreamResult(OutputStream os, Function<ProductBin<K,V>,Object> transform) throws IOException {
		this.os = new ObjectOutputStream(os);
		this.transform = transform;
	}

	@Override
	public void accept(ProductBin<K, V> bin) {
		try {
			os.writeObject( transform.apply(bin) );
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error streaming result object",e);
		}
	}

	@Override
	public void close() throws IOException {
		os.close();
	}
	
	public ObjectOutputStream getPrintStream() {
		return os;
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return new StreamResult<>(os,transform);
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os) throws IOException {
		return of(conv,os,bin->bin.product);
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, File file, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return of(conv,new FileOutputStream(file),transform);
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, File file) throws IOException {
		return of(conv,file,bin->bin.product);
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, String file, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return of(conv,new File(file),transform);
	}

	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, String file) throws IOException {
		return of(conv,file,bin->bin.product);
	}

}
