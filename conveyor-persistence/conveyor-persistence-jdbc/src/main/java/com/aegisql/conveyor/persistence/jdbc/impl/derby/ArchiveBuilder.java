package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.core.Persistence;

/**
 * The Class ArchiveBuilder.
 *
 * @param <K> the key type
 */
@Deprecated
public class ArchiveBuilder<K> {
	
	/** The dpb. */
	private final DerbyPersistenceBuilder<K> dpb;
	
	/**
	 * Instantiates a new archive builder.
	 *
	 * @param dpb the dpb
	 */
	public ArchiveBuilder(DerbyPersistenceBuilder<K> dpb) {
		this.dpb = dpb;
	}
	
	/**
	 * Do nothing.
	 *
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> doNothing() {
		dpb.archiveStrategy = ArchiveStrategy.NO_ACTION;
		return dpb;
	}
	
	/**
	 * Delete.
	 *
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> delete() {
		dpb.archiveStrategy = ArchiveStrategy.DELETE;
		return dpb;
	}
	
	/**
	 * Mark archived.
	 *
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> markArchived() {
		dpb.archiveStrategy = ArchiveStrategy.SET_ARCHIVED;
		return dpb;
	}
	
	/**
	 * Custom strategy.
	 *
	 * @param archiver the archiver
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> customStrategy(Archiver<K> archiver) {
		dpb.archiveStrategy = ArchiveStrategy.CUSTOM;
		dpb.archiver = archiver;
		return dpb;
	}
	
	/**
	 * Move to table.
	 *
	 * @param p the p
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> moveToOtherPersistence(Persistence<K> p) {
		dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_PERSISTENCE;
		dpb.archivePersistence = p;
		return dpb;
	}
	
	/**
	 * Move to file.
	 *
	 * @param bLogConf the b log conf
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> moveToFile(BinaryLogConfiguration bLogConf) {
		dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_FILE;
		dpb.bLogConf = bLogConf;
		return dpb;
	}
	
}