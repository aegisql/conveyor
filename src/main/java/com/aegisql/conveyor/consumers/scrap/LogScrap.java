package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class LogScrap <K> implements Consumer<ScrapBin<?,?>> {

	public static enum Level {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		STDOUT,
		STDERR
	}
	
	private final static Consumer<ScrapBin<?,?>> trace = bin->{
		Conveyor.LOG.trace("{}",bin);		
	};
	private final static Consumer<ScrapBin<?,?>> debug = bin->{
		Conveyor.LOG.debug("{}",bin);		
	};
	private final static Consumer<ScrapBin<?,?>> info = bin->{
		Conveyor.LOG.info("{}",bin);		
	};
	private final static Consumer<ScrapBin<?,?>> warn = bin->{
		Conveyor.LOG.warn("{}",bin);		
	};
	private final static Consumer<ScrapBin<?,?>> error = bin->{
		Conveyor.LOG.error("{}",bin);		
	};
	private final static Consumer<ScrapBin<?,?>> stdout = bin->{
		System.out.println(""+bin);		
	};
	private final static Consumer<ScrapBin<?,?>> stderr = bin->{
		System.err.println(""+bin);		
	};
	
	private final Consumer<ScrapBin<?,?>> consumer;
	
	public LogScrap() {
		consumer = debug;
	}

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

	@Override
	public void accept(ScrapBin<?, ?> t) {
		consumer.accept(t);
	}

	public static <K> LogScrap<K> trace(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.TRACE);
	}

	public static <K> LogScrap<K> debug(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.DEBUG);
	}

	public static <K> LogScrap<K> info(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.INFO);
	}

	public static <K> LogScrap<K> warn(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.WARN);
	}

	public static <K> LogScrap<K> error(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.ERROR);
	}

	public static <K> LogScrap<K> stdOut(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.STDOUT);
	}

	public static <K> LogScrap<K> stdErr(Conveyor<K, ?, ?> conveyor) {
		return new LogScrap<>(Level.STDERR);
	}

}
