package com.aegisql.conveyor.persistence.jdbc.engine;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

// TODO: Auto-generated Javadoc
/**
 * The Interface EngineDepo.
 *
 * @param <K> the key type
 */
public interface EngineDepo <K> extends Closeable {
	
	/** The Constant ID. */
	public static final String ID="ID";
	
	/** The Constant LOAD_TYPE. */
	public static final String LOAD_TYPE="LOAD_TYPE";
	
	/** The Constant CART_KEY. */
	public static final String CART_KEY="CART_KEY";
	
	/** The Constant CART_LABEL. */
	public static final String CART_LABEL="CART_LABEL";
	
	/** The Constant CREATION_TIME. */
	public static final String CREATION_TIME="CREATION_TIME";
	
	/** The Constant EXPIRATION_TIME. */
	public static final String EXPIRATION_TIME="EXPIRATION_TIME";
	
	/** The Constant PRIORITY. */
	public static final String PRIORITY="PRIORITY";
	
	/** The Constant CART_VALUE. */
	public static final String CART_VALUE="CART_VALUE";
	
	/** The Constant VALUE_TYPE. */
	public static final String VALUE_TYPE="VALUE_TYPE";
	
	/** The Constant CART_PROPERTIES. */
	public static final String CART_PROPERTIES="CART_PROPERTIES";
	
	/** The Constant ARCHIVED. */
	public static final String ARCHIVED="ARCHIVED";
	
	/**
	 * Database exists.
	 *
	 * @param database the database
	 * @return true, if successful
	 */
	public boolean databaseExists(String database);
	
	/**
	 * Schema exists.
	 *
	 * @param schema the schema
	 * @return true, if successful
	 */
	public boolean schemaExists(String schema);
	
	/**
	 * Part table exists.
	 *
	 * @param partTable the part table
	 * @return true, if successful
	 */
	public boolean partTableExists(String partTable);
	
	/**
	 * Part table index exists.
	 *
	 * @param partTable the part table
	 * @param indexName the index name
	 * @return true, if successful
	 */
	public boolean partTableIndexExists(String partTable, String indexName);
	
	/**
	 * Completed log table exists.
	 *
	 * @param completedLogTable the completed log table
	 * @return true, if successful
	 */
	public boolean completedLogTableExists(String completedLogTable);
	
	/**
	 * Creates the database.
	 *
	 * @param database the database
	 */
	public void createDatabase(String database);
	
	/**
	 * Creates the schema.
	 *
	 * @param schema the schema
	 */
	public void createSchema(String schema);
	
	/**
	 * Creates the part table.
	 *
	 * @param partTable the part table
	 */
	public void createPartTable(String partTable);
	
	/**
	 * Creates the part table index.
	 *
	 * @param partTable the part table
	 */
	public void createPartTableIndex(String partTable);

	/**
	 * Creates the uniq part table index.
	 *
	 * @param partTable the part table
	 * @param fields the fields
	 */
	public void createUniqPartTableIndex(String partTable,List<String> fields);

	/**
	 * Creates the completed log table.
	 *
	 * @param completedLogTable the completed log table
	 */
	public void createCompletedLogTable(String completedLogTable);
	
	/**
	 * Sets the sorting order.
	 *
	 * @param order the order
	 */
	public void setSortingOrder(LinkedHashMap<String, String> order);
	
	/**
	 * Builds the part table queries.
	 *
	 * @param partTable the part table
	 */
	public void buildPartTableQueries(String partTable);
	
	/**
	 * Builds the completed log table queries.
	 *
	 * @param completedLogTable the completed log table
	 */
	public void buildCompletedLogTableQueries(String completedLogTable);
	
	/**
	 * Delete from completed log.
	 *
	 * @param keys the keys
	 */
	//Archiver commands
	public void deleteFromCompletedLog(Collection<K> keys);
	
	/**
	 * Delete all completed log.
	 */
	public void deleteAllCompletedLog();
	
	/**
	 * Delete from parts by ids.
	 *
	 * @param ids the ids
	 */
	public void deleteFromPartsByIds(Collection<? extends Number> ids);
	
	/**
	 * Delete from parts by cart keys.
	 *
	 * @param keys the keys
	 */
	public void deleteFromPartsByCartKeys(Collection<K> keys);
	
	/**
	 * Delete expired parts.
	 */
	public void deleteExpiredParts();
	
	/**
	 * Delete all parts.
	 */
	public void deleteAllParts();
	
	/**
	 * Update parts by ids.
	 *
	 * @param ids the ids
	 */
	public void updatePartsByIds(Collection<? extends Number> ids);
	
	/**
	 * Update parts by cart keys.
	 *
	 * @param keys the keys
	 */
	public void updatePartsByCartKeys(Collection<K> keys);
	
	/**
	 * Update expired parts.
	 */
	public void updateExpiredParts();
	
	/**
	 * Update all parts.
	 */
	public void updateAllParts();
	
	/**
	 * Save cart.
	 *
	 * @param id the id
	 * @param loadType the load type
	 * @param key the key
	 * @param label the label
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param value the value
	 * @param properties the properties
	 * @param hint the hint
	 * @param priority the priority
	 * @param additionalFields the additional fields
	 */
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
			, List<Object> additionalFields
			);
	
	/**
	 * Save completed build key.
	 *
	 * @param key the key
	 */
	public void saveCompletedBuildKey(Object key);
	
	/**
	 * Gets the parts.
	 *
	 * @param <T> the generic type
	 * @param ids the ids
	 * @param transformer the transformer
	 * @return the parts
	 */
	public <T> List<T> getParts(Collection<Long> ids,Function<ResultSet,T> transformer);
	
	/**
	 * Gets the expired parts.
	 *
	 * @param <T> the generic type
	 * @param transformer the transformer
	 * @return the expired parts
	 */
	public <T> List<T> getExpiredParts(Function<ResultSet,T> transformer);
	
	/**
	 * Gets the static parts.
	 *
	 * @param <T> the generic type
	 * @param transformer the transformer
	 * @return the static parts
	 */
	public <T> List<T> getStaticParts(Function<ResultSet,T> transformer);
	
	/**
	 * Gets the all part ids.
	 *
	 * @param key the key
	 * @return the all part ids
	 */
	public List<Long> getAllPartIds(K key);
	
	/**
	 * Gets the unfinished parts.
	 *
	 * @param <T> the generic type
	 * @param transformer the transformer
	 * @return the unfinished parts
	 */
	public <T> List<T> getUnfinishedParts(Function<ResultSet,T> transformer);
	
	/**
	 * Gets the all completed keys.
	 *
	 * @param transformer the transformer
	 * @return the all completed keys
	 */
	public Set<K> getAllCompletedKeys(Function<ResultSet,K> transformer);
	
	/**
	 * Gets the number of parts.
	 *
	 * @return the number of parts
	 */
	public long getNumberOfParts();
}
