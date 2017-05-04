package com.aegisql.conveyor.consumers.result;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class LogResult <K,E> implements Consumer<ProductBin<K,E>> {

	public static enum Level {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		STDOUT,
		STDERR
	}
	
	private final static Consumer<ProductBin<?,?>> trace = bin->{
		Conveyor.LOG.trace("{}",bin);		
	};
	private final static Consumer<ProductBin<?,?>> debug = bin->{
		Conveyor.LOG.debug("{}",bin);		
	};
	private final static Consumer<ProductBin<?,?>> info = bin->{
		Conveyor.LOG.info("{}",bin);		
	};
	private final static Consumer<ProductBin<?,?>> warn = bin->{
		Conveyor.LOG.warn("{}",bin);		
	};
	private final static Consumer<ProductBin<?,?>> error = bin->{
		Conveyor.LOG.error("{}",bin);		
	};
	private final static Consumer<ProductBin<?,?>> stdout = bin->{
		System.out.println(""+bin);		
	};
	private final static Consumer<ProductBin<?,?>> stderr = bin->{
		System.err.println(""+bin);		
	};
	
	private final Consumer<ProductBin<?,?>> consumer;
	
	public LogResult() {
		consumer = debug;
	}

	public LogResult(Level level) {
		switch (level) {
		case TRACE:
			consumer = trace;
			break;
		case DEBUG:
			consumer = debug;
			break;
		case INFO:
			consumer = info;
			break;
		case WARN:
			consumer = warn;
			break;
		case ERROR:
			consumer = error;
			break;
		case STDOUT:
			consumer = stdout;
			break;
		case STDERR:
			consumer = stderr;
			break;
		default:
			consumer = stdout;
			break;
		}
	}

	@Override
	public void accept(ProductBin<K, E> t) {
		consumer.accept(t);
	}

	public static <K,E> LogResult<K,E> trace(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.TRACE);
	}

	public static <K,E> LogResult<K,E> debug(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.DEBUG);
	}

	public static <K,E> LogResult<K,E> info(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.INFO);
	}

	public static <K,E> LogResult<K,E> warn(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.WARN);
	}

	public static <K,E> LogResult<K,E> error(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.ERROR);
	}

	public static <K,E> LogResult<K,E> stdOut(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.STDOUT);
	}

	public static <K,E> LogResult<K,E> stdErr(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.STDERR);
	}

}
