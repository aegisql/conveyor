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

public class PrintStreamScrap implements Consumer<ScrapBin<?,?>>, Closeable {

	private final PrintStream os;
	
	private final Function<Object,String> toString;
	
	public PrintStreamScrap(OutputStream os) {
		this.os = new PrintStream(os);
		this.toString = v->""+v;
	}

	public PrintStreamScrap(OutputStream os, Function<Object,String> toString) {
		this.os = new PrintStream(os);
		this.toString = toString;
	}

	@Override
	public void accept(ScrapBin<?, ?> bin) {
		os.println(toString.apply(bin.scrap));
		os.flush();
	}

	@Override
	public void close() throws IOException {
		os.close();
	}
	
	public PrintStream getPrintStream() {
		return os;
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, OutputStream os, Function<Object,String> toString) {
		return new PrintStreamScrap(os,toString);
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, OutputStream os) {
		return of(conv,os,v->""+v);
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, File file, Function<Object,String> toString) throws FileNotFoundException {
		return of(conv,new FileOutputStream(file),toString);
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, File file) throws FileNotFoundException {
		return of(conv,file,v->""+v);
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, String file, Function<Object,String> toString) throws FileNotFoundException {
		return of(conv,new File(file),toString);
	}

	public static <K> PrintStreamScrap of(Conveyor<K, ?, ?> conv, String file) throws FileNotFoundException {
		return of(conv,file,v->""+v);
	}
	
}
