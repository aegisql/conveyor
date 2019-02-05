package com.aegisql.conveyor.persistence.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class UnimplementedArchiver<K> implements Archiver<K> {

	public UnimplementedArchiver() {
	}

	@Override
	public void archiveParts(Collection<Long> ids) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveKeys(Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveExpiredParts() {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveAll() {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void setPersistence(Persistence<K> persistence) {}

	@Override
	public String toString() {
		return "UnimplementedArchiver: throws exception on any method call";
	}
	
	

}
