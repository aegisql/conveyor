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

// TODO: Auto-generated Javadoc
/**
 * The Class StreamResult.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class StreamResult <K,V> implements Consumer<ProductBin<K,V>>, Closeable {

	/** The os. */
	private final ObjectOutputStream os;
	
	/** The transform. */
	private final Function<ProductBin<K,V>,Object> transform;
	
	/**
	 * Instantiates a new stream result.
	 *
	 * @param os the os
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public StreamResult(OutputStream os) throws IOException {
		this(os,bin->bin.product);
	}

	/**
	 * Instantiates a new stream result.
	 *
	 * @param os the os
	 * @param transform the transform
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public StreamResult(OutputStream os, Function<ProductBin<K,V>,Object> transform) throws IOException {
		this.os = new ObjectOutputStream(os);
		this.transform = transform;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		try {
			os.writeObject( transform.apply(bin) );
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error streaming result object",e);
		}
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		os.close();
	}
	
	/**
	 * Gets the prints the stream.
	 *
	 * @return the prints the stream
	 */
	public ObjectOutputStream getPrintStream() {
		return os;
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param os the os
	 * @param transform the transform
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return new StreamResult<>(os,transform);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param os the os
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os) throws IOException {
		return of(conv,os,bin->bin.product);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @param transform the transform
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, File file, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return of(conv,new FileOutputStream(file),transform);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, File file) throws IOException {
		return of(conv,file,bin->bin.product);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @param transform the transform
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, String file, Function<ProductBin<K,V>,Object> transform) throws IOException {
		return of(conv,new File(file),transform);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @return the stream result
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K,V> StreamResult<K,V> of(Conveyor<K, ?, V> conv, String file) throws IOException {
		return of(conv,file,bin->bin.product);
	}

}
