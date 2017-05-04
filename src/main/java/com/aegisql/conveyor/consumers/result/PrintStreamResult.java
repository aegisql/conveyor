package com.aegisql.conveyor.consumers.result;

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
import com.aegisql.conveyor.ProductBin;

public class PrintStreamResult <K,V> implements Consumer<ProductBin<K,V>>, Closeable {

	private final PrintStream os;
	
	private final Function<V,String> toString;
	
	public PrintStreamResult(OutputStream os) {
		this.os = new PrintStream(os);
		this.toString = v->""+v;
	}

	public PrintStreamResult(OutputStream os, Function<V,String> toString) {
		this.os = new PrintStream(os);
		this.toString = toString;
	}
	
	@Override
	public void accept(ProductBin<K, V> bin) {
		os.println(toString.apply(bin.product));
		os.flush();
	}

	@Override
	public void close() throws IOException {
		os.close();
	}
	
	public PrintStream getPrintStream() {
		return os;
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os, Function<V,String> toString) {
		return new PrintStreamResult<>(os,toString);
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, OutputStream os) {
		return of(conv,os,v->""+v);
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, File file, Function<V,String> toString) throws FileNotFoundException {
		FileOutputStream os = new FileOutputStream(file);
		return of(conv,os,toString);
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, File file) throws FileNotFoundException {
		return of(conv,new FileOutputStream(file));
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, String file, Function<V,String> toString) throws FileNotFoundException {
		return of(conv,new File(file),toString);
	}

	public static <K,V> PrintStreamResult<K,V> of(Conveyor<K, ?, V> conv, String file) throws FileNotFoundException {
		return of(conv,new File(file));
	}

}
