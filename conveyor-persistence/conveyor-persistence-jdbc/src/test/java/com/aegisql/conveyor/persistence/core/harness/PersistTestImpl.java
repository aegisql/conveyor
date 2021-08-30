package com.aegisql.conveyor.persistence.core.harness;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class PersistTestImpl implements Persistence<Integer>{
	
	public class Engine implements EngineDepo<Integer> {

		@Override
		public boolean databaseExists(String database) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean schemaExists(String schema) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean partTableExists(String partTable) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean partTableIndexExists(String partTableIndex, String indexName) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean completedLogTableExists(String completedLogTable) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void createDatabase(String database) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createSchema(String schema) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createPartTable(String partTable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createPartTableIndex(String partTable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createCompletedLogTable(String completedLogTable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void buildPartTableQueries(String partTable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void buildCompletedLogTableQueries(String completedLogTable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteFromCompletedLog(Collection<Integer> keys) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteAllCompletedLog() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteFromPartsByIds(Collection<? extends Number> ids) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteFromPartsByCartKeys(Collection<Integer> keys) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteExpiredParts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deleteAllParts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updatePartsByIds(Collection<? extends Number> ids) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updatePartsByCartKeys(Collection<Integer> keys) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateExpiredParts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateAllParts() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void saveCart(long id, String loadType, Object key, Object label, Timestamp creationTime,
				Timestamp expirationTime, Object value, String properties, String hint, long priority, List<Object> fields) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void saveCompletedBuildKey(Object key) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public <T> List<T> getParts(Collection<Long> ids, Function<ResultSet, T> transformer) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> List<T> getExpiredParts(Function<ResultSet, T> transformer) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> List<T> getStaticParts(Function<ResultSet, T> transformer) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<Long> getAllPartIds(Integer key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> List<T> getUnfinishedParts(Function<ResultSet, T> transformer) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<Integer> getAllCompletedKeys(Function<ResultSet, Integer> transformer) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getNumberOfParts() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setSortingOrder(LinkedHashMap<String, String> order) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createUniqPartTableIndex(String partTable, List<String> fields) {
			// TODO Auto-generated method stub
			
		}
		
	}

	ConcurrentHashMap<Long,Cart<Integer,?,?>> carts = new ConcurrentHashMap<>();
	ConcurrentHashMap<Long,Cart<Integer,?,?>> expiredCarts = new ConcurrentHashMap<>();
	ConcurrentHashMap<Integer,List<Long>> cartIds   = new ConcurrentHashMap<>();
	Set<Integer> completed = new HashSet<>();
	
	AtomicInteger idGen = new AtomicInteger(0);
	private int maxBatchSize  = 3;
	private long maxBatchTime = 1000;
	
	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

	public void setMaxBatchTime(long maxBatchTime) {
		this.maxBatchTime = maxBatchTime;
	}

	public PersistTestImpl() {
		
	}
	
	public PersistTestImpl(PersistTestImpl old, PersistTestImpl... more) {

		old.archiveCompleteKeys(old.getCompletedKeys());
		absorb(old);
		old.archiveAll();
		
		old.archiveKeys(this.cartIds.keySet());
		this.cartIds.forEach((id,list)->old.archiveParts(list));
		
		completed.addAll(old.completed);
		if(more != null) {
			for(PersistTestImpl p:more) {
				p.archiveCompleteKeys(p.getCompletedKeys());
				absorb(p);
				p.archiveAll();
			}
		}
	}
	

	@Override
	public long nextUniquePartId() {
		return idGen.incrementAndGet();
	}

	@Override
	public <L> void savePart(long id, Cart<Integer, ?, L> cart) {
		if(cart.expired()) {
			expiredCarts.put(id, cart);
		}
		carts.put(id, cart);
		savePartId(cart.getKey(),id);
	}

	@Override
	public void savePartId(Integer key, long cartId) {
		getAllPartIds(key).add(cartId);
	}

	@Override
	public void saveCompletedBuildKey(Integer key) {
		completed.add(key);
	}

	@Override
	public Collection<Long> getAllPartIds(Integer key) {
		ArrayList<Long> newList = new ArrayList<>();
		
		List<Long> ids = cartIds.putIfAbsent(key, newList);
		if(ids == null) {
			ids = newList;
		}
		return ids;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		ids.forEach(id->carts.remove(id));
		ids.forEach(id->expiredCarts.remove(id));
	}

	@Override
	public void archiveKeys(Collection<Integer> key) {
		key.forEach(k->cartIds.remove(k));
	}

	@Override
	public void archiveCompleteKeys(Collection<Integer> keys) {
		completed.removeAll(keys);
		keys.forEach(key->{
			Collection<Long> ids = cartIds.remove(key);
			if(ids != null) {
				ids.forEach(id->carts.remove(id));
			}
		});
	}

	@Override
	public String toString() {
		return "Persistence [carts=" + carts + ", cartIds=" + cartIds + ", ack=" + completed + "]";
	}

	@Override
	public Collection<Cart<Integer, ?, ?>> getAllParts() {
		return carts.values();
	}

	public boolean isEmpty() {
		return carts.isEmpty() && cartIds.isEmpty() && completed.isEmpty();
	}

	@Override
	public Set<Integer> getCompletedKeys() {
		return new HashSet<Integer>(completed);
	}

	@Override
	public void archiveAll() {
		cartIds.clear();
		carts.clear();
		completed.clear();
	}

	@Override
	public <L> Collection<Cart<Integer, ?, L>> getAllStaticParts() {
		return new ArrayList<>();
	}

	@Override
	public Persistence<Integer> copy() {
		return this;
	}

	@Override
	public void close() throws IOException {
		
	}
	
	@Override
	public void archiveExpiredParts() {
		List<Long> expList = new ArrayList<>();
		carts.forEach((id,cart)->{
			long expTime = cart.getExpirationTime();
			if(expTime != 0 && expTime < System.currentTimeMillis() ) {
				expList.add(id);
			}
		});
		expList.forEach(id->carts.remove(id));
	}
	
	@Override
	public int getMaxArchiveBatchSize() {
		return maxBatchSize;
	}

	@Override
	public long getMaxArchiveBatchTime() {
		return maxBatchTime;
	}
	
	@Override
	public long getNumberOfParts() {
		return cartIds.size();
	}

	@Override
	public boolean isPersistentProperty(String property) {
		return true;
	}
	@Override
	public <L> Collection<Cart<Integer, ?, L>> getParts(Collection<Long> ids) {
		List<Cart<Integer, ?, L>> cartsList = new ArrayList<>();
		ids.forEach(id->{
			cartsList.add((Cart<Integer, ?, L>) carts.get(id));
		});
		return cartsList;
	}

	@Override
	public <L> Collection<Cart<Integer, ?, L>> getExpiredParts() {
		List<Cart<Integer, ?, L>> cartsList = new ArrayList<>();
		cartsList.addAll((Collection) expiredCarts.values());
		return cartsList;
	}

	@Override
	public int getMinCompactSize() {
		return 0;
	}

	public EngineDepo<Integer> getEngine() {
		return new Engine();
	}
	
}
