package com.aegisql.conveyor.persistence.jdbc.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractJdbcArchiver.
 *
 * @param <K> the key type
 */
public abstract class AbstractJdbcArchiver <K> implements Archiver<K> {

	/** The Constant LOG. */
	protected static final Logger LOG = LoggerFactory.getLogger(DeleteArchiver.class);
	
	/** The engine. */
	protected final EngineDepo<K> engine;
	
	/** The persistence. */
	protected Persistence<K> persistence;

	/**
	 * Instantiates a new abstract jdbc archiver.
	 *
	 * @param engine the engine
	 */
	public AbstractJdbcArchiver(EngineDepo<K> engine) {
		this.engine = engine;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#setPersistence(com.aegisql.conveyor.persistence.core.Persistence)
	 */
	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [engine=").append(engine).append("]");
		return builder.toString();
	}
	
}
