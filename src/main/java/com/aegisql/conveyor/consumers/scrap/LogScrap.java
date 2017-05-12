package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LogScrap.
 */
public class LogScrap <K> implements Consumer<ScrapBin<K,?>> {

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
	private final static Consumer<ScrapBin<?,?>> trace = bin->{
		Conveyor.LOG.trace("{}",bin);		
	};
	
	/** The Constant debug. */
	private final static Consumer<ScrapBin<?,?>> debug = bin->{
		Conveyor.LOG.debug("{}",bin);		
	};
	
	/** The Constant info. */
	private final static Consumer<ScrapBin<?,?>> info = bin->{
		Conveyor.LOG.info("{}",bin);		
	};
	
	/** The Constant warn. */
	private final static Consumer<ScrapBin<?,?>> warn = bin->{
		Conveyor.LOG.warn("{}",bin);		
	};
	
	/** The Constant error. */
	private final static Consumer<ScrapBin<?,?>> error = bin->{
		Conveyor.LOG.error("{}",bin);		
	};
	
	/** The Constant stdout. */
	private final static Consumer<ScrapBin<?,?>> stdout = bin->{
		System.out.println(""+bin);		
	};
	
	/** The Constant stderr. */
	private final static Consumer<ScrapBin<?,?>> stderr = bin->{
		System.err.println(""+bin);		
	};
	
	/** The consumer. */
	private final Consumer<ScrapBin<?,?>> consumer;
	
	/**
	 * Instantiates a new log scrap.
	 */
	public LogScrap() {
		consumer = debug;
	}

	/**
	 * Instantiates a new log scrap.
	 *
	 * @param level the level
	 */
	public LogScrap(Level level) {
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
	public void accept(ScrapBin<K, ?> t) {
		consumer.accept(t);
	}

	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> trace(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.TRACE);
	}

	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> debug(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.DEBUG);
	}

	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> info(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.INFO);
	}

	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> warn(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.WARN);
	}

	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> error(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.ERROR);
	}

	/**
	 * Std out.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> stdOut(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.STDOUT);
	}

	/**
	 * Std err.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the log scrap
	 */
	public static <K> LogScrap<K> stdErr(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.STDERR);
	}

}
