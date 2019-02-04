package com.aegisql.conveyor.persistence.jdbc.engine;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.aegisql.conveyor.cart.Cart;

public interface EngineDepo <K> {
	
	public static final String ID="ID";
	public static final String LOAD_TYPE="LOAD_TYPE";
	public static final String CART_KEY="CART_KEY";
	public static final String CART_LABEL="CART_LABEL";
	public static final String CREATION_TIME="CREATION_TIME";
	public static final String EXPIRATION_TIME="EXPIRATION_TIME";
	public static final String PRIORITY="PRIORITY";
	public static final String CART_VALUE="CART_VALUE";
	public static final String VALUE_TYPE="VALUE_TYPE";
	public static final String CART_PROPERTIES="CART_PROPERTIES";
	public static final String ARCHIVED="ARCHIVED";
	
	public boolean databaseExists(String database);
	public boolean schemaExists(String schema);
	public boolean partTableExists(String partTable);
	public boolean partTableIndexExists(String partTableIndex);
	public boolean completedLogTableExists(String completedLogTable);
	
	public void createDatabase(String database);
	public void createSchema(String schema);
	public void createPartTable(String partTable);
	public void createPartTableIndex(String partTable);
	public void createCompletedLogTable(String completedLogTable);
	
	public void buildPartTableQueries(String partTable);
	public void buildCompletedLogTableQueries(String completedLogTable);
	
	//Archiver commands
	public void deleteFromCompletedLog(Collection<K> keys);
	public void deleteAllCompletedLog();
	public void deleteFromPartsByIds(Collection<? extends Number> ids);
	public void deleteFromPartsByCartKeys(Collection<K> keys);
	public void deleteExpiredParts();
	public void deleteAllParts();
	public void updatePartsByIds(Collection<? extends Number> ids);
	public void updatePartsByCartKeys(Collection<K> keys);
	public void updateExpiredParts();
	public void updateAllParts();
	
	//Persistence commands
	public void saveCart(
			  long id
			, String loadType
			, Object key
			, Object label
			, Timestamp creationTime
			, Timestamp expirationTime
			, Object value
			, String properties
			, String hint
			, long priority
			);
	public void saveCompletedBuildKey(Object key);
	public <T> List<T> getParts(Collection<Long> ids,Function<ResultSet,T> transformer);
	public <T> List<T> getExpiredParts(Function<ResultSet,T> transformer);
	public <T> List<T> getStaticParts(Function<ResultSet,T> transformer);
	public List<Long> getAllPartIds(K key);
	public <T> List<T> getUnfinishedParts(Function<ResultSet,T> transformer);
	public Set<K> getAllCompletedKeys(Function<ResultSet,K> transformer);
	public long getNumberOfParts();
}
