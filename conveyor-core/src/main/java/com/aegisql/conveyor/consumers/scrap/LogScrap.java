package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class LogResult.
 *
 * @param <K> the key type
 */
public class LogScrap <K> implements ScrapConsumer<K,Object> {

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
	private final static ScrapConsumer<?,?> stdout = bin-> System.out.println(""+bin);
	
	/** The Constant stderr. */
	private final static ScrapConsumer<?,?> stderr = bin-> System.err.println(""+bin);
	
	/** The consumer. */
	private final ScrapConsumer<?,?> consumer;
	
	/**
	 * Instantiates a new log result.
	 */
	public LogScrap() {
		consumer = bin->Conveyor.LOG.debug("{}",bin);
	}

	/**
	 * Instantiates a new log result.
	 *
	 * @param log the log
	 * @param level the level
	 */
	public LogScrap(Logger log, Level level) {
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
	public void accept(ScrapBin<K,Object> t) {
		consumer.accept((ScrapBin) t);
	}

	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.TRACE);
	}

	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.DEBUG);
	}

	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.INFO);
	}

	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.WARN);
	}

	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.ERROR);
	}

	/**
	 * Std out.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> stdOut(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.STDOUT);
	}

	/**
	 * Std err.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> stdErr(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.STDERR);
	}

}
