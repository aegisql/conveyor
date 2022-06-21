package com.aegisql.conveyor.consumers.result;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Function;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class PrintStreamResult.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class PrintStreamResult <K,V> implements ResultConsumer<K,V>, Closeable {

	/** The os. */
	private final PrintStream os;
	
	/** The to string. */
	private final Function<V,String> toString;
	
	/**
	 * Instantiates a new prints the stream result.
	 *
	 * @param os the os
	 */
	public PrintStreamResult(OutputStream os) {
		this.os = new PrintStream(os);
		this.toString = v->""+v;
	}

	/**
	 * Instantiates a new prints the stream result.
	 *
	 * @param os the os
	 * @param toString the to string
	 */
	public PrintStreamResult(OutputStream os, Function<V,String> toString) {
		this.os = new PrintStream(os);
		this.toString = toString;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		os.println(toString.apply(bin.product));
		os.flush();
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		os.close();
	}
	
	/**
	 * Gets the prints the stream.
	 *
	 * @return the prints the stream
	 */
	public PrintStream getPrintStream() {
		return os;
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param os the os
	 * @param toString the to string
	 * @return the prints the stream result
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os, Function<V,String> toString) {
		return new PrintStreamResult<>(os,toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param os the os
	 * @return the prints the stream result
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os) {
		return of(conv,os,v->""+v);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @param toString the to string
	 * @return the prints the stream result
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, File file, Function<V,String> toString) throws FileNotFoundException {
		FileOutputStream os = new FileOutputStream(file);
		return of(conv,os,toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @return the prints the stream result
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, File file) throws FileNotFoundException {
		return of(conv,new FileOutputStream(file));
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @param toString the to string
	 * @return the prints the stream result
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, String file, Function<V,String> toString) throws FileNotFoundException {
		return of(conv,new File(file),toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param file the file
	 * @return the prints the stream result
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, String file) throws FileNotFoundException {
		return of(conv,new File(file));
	}

}
