package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;

// TODO: Auto-generated Javadoc
/**
 * The Class LogResult.
 *
 * @param <K> the key type
 * @param <E> the element type
 */
public class LogResult <K,E> implements ResultConsumer<K,E> {

	@Serial
    private static final long serialVersionUID = 1L;

	/**
	 * The Enum Level.
	 */
	public enum Level {
		
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
	
	/** The Constant stdout. */
	private final static ResultConsumer <?,?> stdout = bin->System.out.println(""+bin);

	
	/** The Constant stderr. */
	private final static ResultConsumer <?,?> stderr = bin->System.err.println(""+bin);
	
	/** The consumer. */
	private final ResultConsumer<?,?>  consumer;
	
	/**
	 * Instantiates a new log result.
	 */
	public LogResult() {
		consumer = bin->Conveyor.LOG.debug("{}",bin);
	}

	/**
	 * Instantiates a new log result.
	 *
	 * @param log the log
	 * @param level the level
	 */
	public LogResult(Logger log, Level level) {
		this.consumer = switch (level) {
			case TRACE -> bin -> log.trace("{}", bin);
			case DEBUG -> bin -> log.debug("{}", bin);
			case INFO -> bin -> log.info("{}", bin);
			case WARN -> bin -> log.warn("{}", bin);
			case ERROR -> bin -> log.error("{}", bin);
			case STDOUT -> stdout;
			case STDERR -> stderr;
		};
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, E> t) {
		consumer.accept((ProductBin)t);
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
		return new LogResult<>(Conveyor.LOG,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> trace(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogResult<>(logger,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> trace(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> trace(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.TRACE);
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
		return new LogResult<>(Conveyor.LOG,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> debug(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogResult<>(logger,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> debug(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> debug(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.DEBUG);
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
		return new LogResult<>(Conveyor.LOG,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> info(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogResult<>(logger,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> info(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> info(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.INFO);
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
		return new LogResult<>(Conveyor.LOG,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> warn(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogResult<>(logger,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> warn(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> warn(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.WARN);
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
		return new LogResult<>(Conveyor.LOG,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> error(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogResult<>(logger,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> error(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log result
	 */
	public static <K,E> LogResult<K,E> error(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogResult<>(LoggerFactory.getLogger(logger),Level.ERROR);
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
		return new LogResult<>(Conveyor.LOG,Level.STDOUT);
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
		return new LogResult<>(Conveyor.LOG,Level.STDERR);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("LogResult{}");
		return sb.toString();
	}
}
