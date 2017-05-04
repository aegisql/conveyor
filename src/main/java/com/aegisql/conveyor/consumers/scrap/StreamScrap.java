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

public class StreamScrap <K> implements Consumer<ScrapBin<K,?>>, Closeable {

	private final ObjectOutputStream os;
	
	public StreamScrap(OutputStream os) throws IOException {
		this.os = new ObjectOutputStream(os);
	}

	@Override
	public void accept(ScrapBin<K,?> bin) {
		try {
			os.writeObject(bin.scrap);
			os.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error streaming scrap object",e);
		}
	}

	@Override
	public void close() throws IOException {
		os.close();
	}
	
	public ObjectOutputStream getPrintStream() {
		return os;
	}

	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, OutputStream os) throws IOException {
		return new StreamScrap<>(os);
	}

	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, File file) throws IOException {
		return of(conv,new FileOutputStream(file));
	}

	public static <K> StreamScrap<K> of(Conveyor<K, ?, ?> conv, String file) throws IOException {
		return of(conv,new File(file));
	}

}
