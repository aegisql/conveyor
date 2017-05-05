package com.aegisql.conveyor.consumers.result;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LogResult.
 *
 * @param <K> the key type
 * @param <E> the element type
 */
public class LogResult <K,E> implements Consumer<ProductBin<K,E>> {

	/**
	 * The Enum Level.
	 */
	public static enum Level {
		
		/** The trace. */
		TRACE,
		
		/** The debug. */
		DEBUG,
		
		/** The info. */
		INFO,
		
		/** The warn. */
		WARN,
		
		/** The error. */
		ERROR,
		
		/** The stdout. */
		STDOUT,
		
		/** The stderr. */
		STDERR
	}
	
	/** The Constant trace. */
	private final static Consumer<ProductBin<?,?>> trace = bin->{
		Conveyor.LOG.trace("{}",bin);		
	};
	
	/** The Constant debug. */
	private final static Consumer<ProductBin<?,?>> debug = bin->{
		Conveyor.LOG.debug("{}",bin);		
	};
	
	/** The Constant info. */
	private final static Consumer<ProductBin<?,?>> info = bin->{
		Conveyor.LOG.info("{}",bin);		
	};
	
	/** The Constant warn. */
	private final static Consumer<ProductBin<?,?>> warn = bin->{
		Conveyor.LOG.warn("{}",bin);		
	};
	
	/** The Constant error. */
	private final static Consumer<ProductBin<?,?>> error = bin->{
		Conveyor.LOG.error("{}",bin);		
	};
	
	/** The Constant stdout. */
	private final static Consumer<ProductBin<?,?>> stdout = bin->{
		System.out.println(""+bin);		
	};
	
	/** The Constant stderr. */
	private final static Consumer<ProductBin<?,?>> stderr = bin->{
		System.err.println(""+bin);		
	};
	
	/** The consumer. */
	private final Consumer<ProductBin<?,?>> consumer;
	
	/**
	 * Instantiates a new log result.
	 */
	public LogResult() {
		consumer = debug;
	}

	/**
	 * Instantiates a new log result.
	 *
	 * @param level the level
	 */
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

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, E> t) {
		consumer.accept(t);
	}

	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> trace(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.TRACE);
	}

	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> debug(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.DEBUG);
	}

	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> info(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.INFO);
	}

	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> warn(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.WARN);
	}

	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> error(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.ERROR);
	}

	/**
	 * Std out.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> stdOut(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.STDOUT);
	}

	/**
	 * Std err.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> stdErr(Conveyor<K, ?, E> conveyor) {
		return new LogResult<>(Level.STDERR);
	}

}
