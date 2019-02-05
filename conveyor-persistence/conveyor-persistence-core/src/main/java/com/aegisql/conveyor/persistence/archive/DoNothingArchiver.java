package com.aegisql.conveyor.persistence.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.core.Persistence;

public class DoNothingArchiver<K> implements Archiver<K> {

	public DoNothingArchiver() {
	}

	@Override
	public void archiveParts(Collection<Long> ids) {}

	@Override
	public void archiveKeys( Collection<K> keys) {}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {}

	@Override
	public void archiveExpiredParts() {}

	@Override
	public void archiveAll() {}

	@Override
	public void setPersistence(Persistence<K> persistence) {}

	@Override
	public String toString() {
		return "DoNothingArchiver: no action on archive commands";
	}
	
	

}
