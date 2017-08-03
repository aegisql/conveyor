package com.aegisql.conveyor.persistence.mapdb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

public class MapDBPersistence <K> implements Persistence<K> {

	private final static Logger LOG = LoggerFactory.getLogger(MapDBPersistence.class);
	
	/** The db path. */
	private final String dbPath;
	/** The name. */
	private final String name;
	/** The created timestam. */
	private final long createdTimestamp = System.currentTimeMillis();
	//1501791526MMMXXXXXX 
	private final AtomicLong idSource = new AtomicLong( createdTimestamp * 1000000);
	
	private final BTreeMap<Long,Cart<K, ?, ?>> carts;
	private final BTreeMap<K,Boolean> completed;

	private final DB db;
	
	private final String dbName;

	
	public MapDBPersistence(String dbPath, String name) {
		this.dbPath = dbPath;
		this.name   = name;
		this.dbName = this.dbPath+"/"+this.name+"."+this.createdTimestamp+".db";
		db = DBMaker.fileDB(this.dbName).make();
		carts = (BTreeMap<Long, Cart<K, ?, ?>>) db.treeMap("carts").create();
		completed = (BTreeMap<K, Boolean>) db.treeMap("completed").create();
	}
	
	@Override
	public long nextUniquePartId() {
		return idSource.incrementAndGet();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		carts.put(id, cart);
	}

	@Override
	public void savePartId(K key, long partId) {
		((BTreeMap<Long,Boolean>)db.treeMap(key.toString()).createOrOpen()).put(partId, Boolean.TRUE);
	}

	@Override
	public void saveCompletedBuildKey(K key) {
		completed.put(key, Boolean.TRUE);
		
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		return (Cart<K, ?, L>) carts.get(id);
	}

	@Override
	public Collection<Long> getAllPartIds(K key) {
		return ((BTreeMap<Long,Boolean>)db.treeMap(key.toString()).createOrOpen()).keySet();
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		return carts.values();
	}

	@Override
	public Set<K> getCompletedKeys() {
		return new HashSet<K>(completed.values());
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		ids.forEach(key->carts.remove(key));
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		keys.forEach(key->{
			((BTreeMap<Long,Boolean>)db.treeMap(key.toString()).createOrOpen()).close();
		});		
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		keys.forEach(key->completed.remove(key));
		keys.forEach(key->{
			((BTreeMap<Long,Boolean>)db.treeMap(key.toString()).createOrOpen()).close();
		});
	}

	@Override
	public void archiveAll() {
		db.getAll().clear();
	}

}
