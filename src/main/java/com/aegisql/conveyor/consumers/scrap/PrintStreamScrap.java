package com.aegisql.conveyor.consumers.scrap;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class PrintStreamScrap.
 */
public class PrintStreamScrap implements Consumer<ScrapBin<?,?>>, Closeable {

	/** The os. */
	private final PrintStream os;
	
	/** The to string. */
	private final Function<Object,String> toString;
	
	/**
	 * Instantiates a new prints the stream scrap.
	 *
	 * @param os the os
	 */
	public PrintStreamScrap(OutputStream os) {
		this.os = new PrintStream(os);
		this.toString = v->""+v;
	}

	/**
	 * Instantiates a new prints the stream scrap.
	 *
	 * @param os the os
	 * @param toString the to string
	 */
	public PrintStreamScrap(OutputStream os, Function<Object,String> toString) {
		this.os = new PrintStream(os);
		this.toString = toString;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<?, ?> bin) {
		os.println(toString.apply(bin.scrap));
		os.flush();
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
	public PrintStream getPrintStream() {
		return os;
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param os the os
	 * @param toString the to string
	 * @return the prints the stream scrap
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, OutputStream os, Function<Object,String> toString) {
		return new PrintStreamScrap(os,toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param os the os
	 * @return the prints the stream scrap
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, OutputStream os) {
		return of(conv,os,v->""+v);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @param toString the to string
	 * @return the prints the stream scrap
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, File file, Function<Object,String> toString) throws FileNotFoundException {
		return of(conv,new FileOutputStream(file),toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @return the prints the stream scrap
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, File file) throws FileNotFoundException {
		return of(conv,file,v->""+v);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @param toString the to string
	 * @return the prints the stream scrap
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, String file, Function<Object,String> toString) throws FileNotFoundException {
		return of(conv,new File(file),toString);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @return the prints the stream scrap
	 * @throws FileNotFoundException the file not found exception
	 */
	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, String file) throws FileNotFoundException {
		return of(conv,file,v->""+v);
	}
	
}
