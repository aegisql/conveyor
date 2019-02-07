package com.aegisql.conveyor.persistence.jdbc.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

public abstract class AbstractJdbcArchiver <K> implements Archiver<K> {

	protected static final Logger LOG = LoggerFactory.getLogger(DeleteArchiver.class);
	
	protected final EngineDepo<K> engine;
	
	protected Persistence<K> persistence;

	public AbstractJdbcArchiver(EngineDepo<K> engine) {
		this.engine = engine;
	}
	
	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [engine=").append(engine).append("]");
		return builder.toString();
	}
	
}
