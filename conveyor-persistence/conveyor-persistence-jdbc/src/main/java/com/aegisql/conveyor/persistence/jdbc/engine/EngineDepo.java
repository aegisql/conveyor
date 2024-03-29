package com.aegisql.conveyor.persistence.jdbc.engine;

import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionDefaults;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

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
public interface EngineDepo <K> extends Closeable, ConnectionDefaults {

	/**
	 * The Constant ID.
	 */
	String ID="ID";

	/**
	 * The Constant LOAD_TYPE.
	 */
	String LOAD_TYPE="LOAD_TYPE";

	/**
	 * The Constant CART_KEY.
	 */
	String CART_KEY="CART_KEY";

	/**
	 * The Constant CART_LABEL.
	 */
	String CART_LABEL="CART_LABEL";

	/**
	 * The Constant CREATION_TIME.
	 */
	String CREATION_TIME="CREATION_TIME";

	/**
	 * The Constant EXPIRATION_TIME.
	 */
	String EXPIRATION_TIME="EXPIRATION_TIME";

	/**
	 * The Constant PRIORITY.
	 */
	String PRIORITY="PRIORITY";

	/**
	 * The Constant CART_VALUE.
	 */
	String CART_VALUE="CART_VALUE";

	/**
	 * The Constant VALUE_TYPE.
	 */
	String VALUE_TYPE="VALUE_TYPE";

	/**
	 * The Constant CART_PROPERTIES.
	 */
	String CART_PROPERTIES="CART_PROPERTIES";

	/**
	 * The Constant ARCHIVED.
	 */
	String ARCHIVED="ARCHIVED";

	void setConnectionFactory(ConnectionFactory connectionFactory);

	/**
	 * Database exists.
	 *
	 * @param database the database
	 * @return true, if successful
	 */
	boolean databaseExists(String database);

	/**
	 * Schema exists.
	 *
	 * @param schema the schema
	 * @return true, if successful
	 */
	boolean schemaExists(String schema);

	/**
	 * Part table exists.
	 *
	 * @param partTable the part table
	 * @return true, if successful
	 */
	boolean partTableExists(String partTable);

	/**
	 * Part table index exists.
	 *
	 * @param partTable the part table
	 * @param indexName the index name
	 * @return true, if successful
	 */
	boolean partTableIndexExists(String partTable, String indexName);

	/**
	 * Completed log table exists.
	 *
	 * @param completedLogTable the completed log table
	 * @return true, if successful
	 */
	boolean completedLogTableExists(String completedLogTable);

	/**
	 * Creates the database.
	 *
	 * @param database the database
	 */
	void createDatabase(String database);

	/**
	 * Create database if not exists.
	 *
	 * @param database the database
	 */
	default void createDatabaseIfNotExists(String database) {
		if( ! databaseExists(database)) {
			createDatabase(database);
		}
	}

	/**
	 * Creates the schema.
	 *
	 * @param schema the schema
	 */
	void createSchema(String schema);

	/**
	 * Create schema if not exists.
	 *
	 * @param schema the schema
	 */
	default void createSchemaIfNotExists(String schema) {
		if( ! schemaExists(schema)) {
			createSchema(schema);
		}
	}

	/**
	 * Creates the part table.
	 *
	 * @param partTable the part table
	 */
	void createPartTable(String partTable);

	/**
	 * Create part table if not exists.
	 *
	 * @param partTable the part table
	 */
	default void createPartTableIfNotExists(String partTable) {
		if( ! partTableExists(partTable)) {
			createPartTable(partTable);
		}
	}

	/**
	 * Index name string.
	 *
	 * @param table the table
	 * @return the string
	 */
	default String indexName(String table) {
		return table+"_IDX";
	}

	/**
	 * Index name string.
	 *
	 * @param table  the table
	 * @param fields the fields
	 * @return the string
	 */
	default String indexName(String table, String... fields) {
		if(fields==null || fields.length==0) {
			return indexName(table);
		}
		return table+"_"+String.join("_",fields)+"_IDX";
	}

	/**
	 * Index name string.
	 *
	 * @param table  the table
	 * @param fields the fields
	 * @return the string
	 */
	default String indexName(String table, List<String> fields) {
		if(fields==null) {
			return indexName(table);
		}
		return indexName(table, fields.toArray(new String[]{}));
	}

	/**
	 * Creates the part table index.
	 *
	 * @param partTable the part table
	 */
	void createPartTableIndex(String partTable);

	/**
	 * Create part table index if not exists.
	 *
	 * @param partTable the part table
	 */
	default void createPartTableIndexIfNotExists(String partTable) {
		if( ! partTableIndexExists(partTable,indexName(partTable))) {
			createPartTableIndex(partTable);
		}
	}

	/**
	 * Creates the uniq part table index.
	 *
	 * @param partTable the part table
	 * @param fields    the fields
	 */
	void createUniqPartTableIndex(String partTable,List<String> fields);

	/**
	 * Create uniq part table index if not exists.
	 *
	 * @param partTable the part table
	 * @param fields    the fields
	 */
	default void createUniqPartTableIndexIfNotExists(String partTable,List<String> fields) {
		if( ! partTableIndexExists(partTable,indexName(partTable,fields))) {
			createUniqPartTableIndex(partTable,fields);
		}
	}

	/**
	 * Creates the completed log table.
	 *
	 * @param completedLogTable the completed log table
	 */
	void createCompletedLogTable(String completedLogTable);

	/**
	 * Create completed log table if not exists.
	 *
	 * @param completedLogTable the completed log table
	 */
	default void createCompletedLogTableIfNotExists(String completedLogTable) {
		if( ! completedLogTableExists(completedLogTable)) {
			createCompletedLogTable(completedLogTable);
		}
	}

	/**
	 * Sets the sorting order.
	 *
	 * @param order the order
	 */
	void setSortingOrder(LinkedHashMap<String, String> order);

	/**
	 * Builds the part table queries.
	 *
	 * @param partTable the part table
	 */
	void buildPartTableQueries(String partTable);

	/**
	 * Builds the completed log table queries.
	 *
	 * @param completedLogTable the completed log table
	 */
	void buildCompletedLogTableQueries(String completedLogTable);

	/**
	 * Delete from completed log.
	 *
	 * @param keys the keys
	 */
//Archiver commands
	void deleteFromCompletedLog(Collection<K> keys);

	/**
	 * Delete all completed log.
	 */
	void deleteAllCompletedLog();

	/**
	 * Delete from parts by ids.
	 *
	 * @param ids the ids
	 */
	void deleteFromPartsByIds(Collection<? extends Number> ids);

	/**
	 * Delete from parts by cart keys.
	 *
	 * @param keys the keys
	 */
	void deleteFromPartsByCartKeys(Collection<K> keys);

	/**
	 * Delete expired parts.
	 */
	void deleteExpiredParts();

	/**
	 * Delete all parts.
	 */
	void deleteAllParts();

	/**
	 * Update parts by ids.
	 *
	 * @param ids the ids
	 */
	void updatePartsByIds(Collection<? extends Number> ids);

	/**
	 * Update parts by cart keys.
	 *
	 * @param keys the keys
	 */
	void updatePartsByCartKeys(Collection<K> keys);

	/**
	 * Update expired parts.
	 */
	void updateExpiredParts();

	/**
	 * Update all parts.
	 */
	void updateAllParts();

	/**
	 * Save cart.
	 *
	 * @param id               the id
	 * @param loadType         the load type
	 * @param key              the key
	 * @param label            the label
	 * @param creationTime     the creation time
	 * @param expirationTime   the expiration time
	 * @param value            the value
	 * @param properties       the properties
	 * @param hint             the hint
	 * @param priority         the priority
	 * @param additionalFields the additional fields
	 */
//Persistence commands
	void saveCart(
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
	void saveCompletedBuildKey(Object key);

	/**
	 * Gets the parts.
	 *
	 * @param <T>         the generic type
	 * @param ids         the ids
	 * @param transformer the transformer
	 * @return the parts
	 */
	<T> List<T> getParts(Collection<Long> ids,Function<ResultSet,T> transformer);

	/**
	 * Gets the expired parts.
	 *
	 * @param <T>         the generic type
	 * @param transformer the transformer
	 * @return the expired parts
	 */
	<T> List<T> getExpiredParts(Function<ResultSet,T> transformer);

	/**
	 * Gets the static parts.
	 *
	 * @param <T>         the generic type
	 * @param transformer the transformer
	 * @return the static parts
	 */
	<T> List<T> getStaticParts(Function<ResultSet,T> transformer);

	/**
	 * Gets the all part ids.
	 *
	 * @param key the key
	 * @return the all part ids
	 */
	List<Long> getAllPartIds(K key);

	/**
	 * Gets the unfinished parts.
	 *
	 * @param <T>         the generic type
	 * @param transformer the transformer
	 * @return the unfinished parts
	 */
	<T> List<T> getUnfinishedParts(Function<ResultSet,T> transformer);

	/**
	 * Gets the all completed keys.
	 *
	 * @param transformer the transformer
	 * @return the all completed keys
	 */
	Set<K> getAllCompletedKeys(Function<ResultSet,K> transformer);

	/**
	 * Gets the number of parts.
	 *
	 * @return the number of parts
	 */
	long getNumberOfParts();

	void setAdditionalFields(List<Field<?>> additionalFields);

	ConnectionFactory<?> getConnectionFactory();

}
