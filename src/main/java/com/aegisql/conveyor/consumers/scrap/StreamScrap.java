package com.aegisql.conveyor.consumers.scrap;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class StreamScrap.
 *
 * @param <K> the key type
 */
public class StreamScrap<K> implements Consumer<ScrapBin<K,?>>, Closeable {

	/** The os. */
	private final ObjectOutputStream os;
	
	/**
	 * Instantiates a new stream scrap.
	 *
	 * @param os the os
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public StreamScrap(OutputStream os) throws IOException {
		this.os = new ObjectOutputStream(os);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,?> bin) {
		try {
			os.writeObject(bin.scrap);
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error streaming scrap object",e);
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
	 * @param conv the conv
	 * @param os the os
	 * @return the stream scrap
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, OutputStream os) throws IOException {
		return new StreamScrap<>(os);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @return the stream scrap
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, File file) throws IOException {
		return of(conv,new FileOutputStream(file));
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param file the file
	 * @return the stream scrap
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, String file) throws IOException {
		return of(conv,new File(file));
	}

}
