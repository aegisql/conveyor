package com.aegisql.conveyor.persistence.jdbc.engine;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.engine.statement_executor.StatementExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: Auto-generated Javadoc

/**
 * The Class GenericEngine.
 *
 * @param <K> the key type
 */
public abstract class GenericEngine <K> implements EngineDepo <K>  {

	/**
	 * The Constant LOG.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(EngineDepo.class);

	/** The Constant LOAD_TYPE_MAX_LENGTH. */
	private static final int LOAD_TYPE_MAX_LENGTH = 15;

	/** The Constant LABEL_MAX_LENGTH. */
	private static final int LABEL_MAX_LENGTH = 100;

	/** The Constant VALUE_CLASS_MAX_LENGTH. */
	private static final int VALUE_CLASS_MAX_LENGTH = 255;

	/**
	 * The key class.
	 */
	protected final Class<K> keyClass;

	/**
	 * The driver.
	 */
	protected String driver;

	/**
	 * The connection url template for init database.
	 */
	protected String connectionUrlTemplateForInitDatabase;

	/**
	 * The connection url template for init schema.
	 */
	protected String connectionUrlTemplateForInitSchema;

	/**
	 * The connection url template for init tables and indexes.
	 */
	protected String connectionUrlTemplateForInitTablesAndIndexes;

	/**
	 * Gets connection url template.
	 *
	 * @return the connection url template
	 */
	protected String getConnectionUrlTemplate() {
		return connectionUrlTemplate;
	}

	/**
	 * The connection url template.
	 */
	protected String connectionUrlTemplate;

	/**
	 * The host.
	 */
	protected String host;

	/**
	 * The port.
	 */
	protected int port;

	/**
	 * The user.
	 */
	protected String user;

	/**
	 * The password.
	 */
	protected String password;

	/**
	 * The properties.
	 */
	protected Properties properties = new Properties();

	/**
	 * The fields.
	 */
	protected final LinkedHashMap<String,String> fields = new LinkedHashMap<>();

	/**
	 * The delete from completed sql.
	 */
	protected String deleteFromCompletedSql;

	/**
	 * The delete all completed sql.
	 */
	protected String deleteAllCompletedSql;

	/**
	 * The delete from parts by id sql.
	 */
	protected String deleteFromPartsByIdSql;

	/**
	 * The delete from parts by cart key sql.
	 */
	protected String deleteFromPartsByCartKeySql;

	/**
	 * The delete expired parts sql.
	 */
	protected String deleteExpiredPartsSql;

	/**
	 * The delete all parts sql.
	 */
	protected String deleteAllPartsSql;

	/**
	 * The update parts by id sql.
	 */
	protected String updatePartsByIdSql;

	/**
	 * The update parts by cart key sql.
	 */
	protected String updatePartsByCartKeySql;

	/**
	 * The update expired parts sql.
	 */
	protected String updateExpiredPartsSql;

	/**
	 * The update all parts sql.
	 */
	protected String updateAllPartsSql;

	/**
	 * The q.
	 */
	protected final String q;

	/**
	 * The key sql type.
	 */
	protected final String keySqlType;

	/**
	 * The save cart query.
	 */
	protected String saveCartQuery;

	/**
	 * The save completed build key query.
	 */
	protected String saveCompletedBuildKeyQuery;

	/**
	 * The get part query.
	 */
	protected String getPartQuery;

	/**
	 * The get expired part query.
	 */
	protected String getExpiredPartQuery;

	/**
	 * The get all part ids query.
	 */
	protected String getAllPartIdsQuery;

	/**
	 * The get all unfinished parts query.
	 */
	protected String getAllUnfinishedPartsQuery;

	/**
	 * The get all completed keys query.
	 */
	protected String getAllCompletedKeysQuery;

	/**
	 * The get all static parts query.
	 */
	protected String getAllStaticPartsQuery;

	/**
	 * The get number of parts query.
	 */
	protected String getNumberOfPartsQuery;

	/**
	 * The sorting order.
	 */
	protected Map<String,String> sortingOrder = new LinkedHashMap<>();

	/**
	 * The additional fields.
	 */
	protected List<Field<?>> additionalFields = new ArrayList<>();

	/**
	 * The Connection factory.
	 */
	protected ConnectionFactory connectionFactory;

	/**
	 * Instantiates a new generic engine.
	 *
	 * @param keyClass          the key class
	 * @param connectionFactory the connection factory
	 */
	protected GenericEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		this.keyClass = keyClass;

		this.connectionFactory = connectionFactory;

		this.keySqlType = getKeySqlType(this.keyClass);
		if(Number.class.isAssignableFrom(keyClass)) {
			this.q = "";
		} else {
			this.q = "'";
		}
		this.fields.putIfAbsent(ID, "BIGINT PRIMARY KEY");
		this.fields.putIfAbsent(LOAD_TYPE, "CHAR(" + LOAD_TYPE_MAX_LENGTH + ")");
		this.fields.putIfAbsent(CART_KEY,this.keySqlType);
		this.fields.putIfAbsent(CART_LABEL, "VARCHAR("+LABEL_MAX_LENGTH +")");
		this.fields.putIfAbsent(CREATION_TIME, "DATETIME NOT NULL");
		this.fields.putIfAbsent(EXPIRATION_TIME, "DATETIME NOT NULL");
		this.fields.putIfAbsent(PRIORITY, "BIGINT NOT NULL DEFAULT 0");
		this.fields.putIfAbsent(CART_VALUE, "BLOB");
		this.fields.putIfAbsent(VALUE_TYPE, "VARCHAR("+VALUE_CLASS_MAX_LENGTH+")");
		this.fields.putIfAbsent(CART_PROPERTIES, "TEXT");
		this.fields.putIfAbsent(ARCHIVED, "SMALLINT NOT NULL DEFAULT 0");
		sortingOrder.putIfAbsent(ID, "ASC");
		init();
	}

	/**
	 * Sets driver.
	 *
	 * @param driver the driver
	 */
	public void setDriver(String driver) {
		connectionFactory.setDriverClassName(driver);
		this.driver = driver;
	}

	/**
	 * Sets connection url template for init database.
	 *
	 * @param connectionUrlTemplateForInitDatabase the connection url template for init database
	 */
	public void setConnectionUrlTemplateForInitDatabase(String connectionUrlTemplateForInitDatabase) {
		this.connectionUrlTemplateForInitDatabase = connectionUrlTemplateForInitDatabase;
	}

	/**
	 * Sets connection url template for init schema.
	 *
	 * @param connectionUrlTemplateForInitSchema the connection url template for init schema
	 */
	public void setConnectionUrlTemplateForInitSchema(String connectionUrlTemplateForInitSchema) {
		this.connectionUrlTemplateForInitSchema = connectionUrlTemplateForInitSchema;
	}

	/**
	 * Sets connection url template for init tables and indexes.
	 *
	 * @param connectionUrlTemplateForInitTablesAndIndexes the connection url template for init tables and indexes
	 */
	public void setConnectionUrlTemplateForInitTablesAndIndexes(String connectionUrlTemplateForInitTablesAndIndexes) {
		this.connectionUrlTemplateForInitTablesAndIndexes = connectionUrlTemplateForInitTablesAndIndexes;
	}

	/**
	 * Sets connection url template.
	 *
	 * @param connectionUrlTemplate the connection url template
	 */
	public void setConnectionUrlTemplate(String connectionUrlTemplate) {
		this.connectionUrlTemplate = connectionUrlTemplate;
		switchUrlTemplae(connectionUrlTemplate);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#databaseExists(java.lang.String)
	 */
	@Override
	public boolean databaseExists(String database) {
		if( ! switchUrlTemplae(connectionUrlTemplateForInitDatabase) ) {
			return true;
		}
		AtomicBoolean res = new AtomicBoolean(true);

		try(StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.meta(meta->{
				try(ResultSet databaseRs = meta.getCatalogs()) {
					boolean databaseExists = false;
					while (databaseRs.next()) {
						String db = databaseRs.getString("TABLE_CAT");
						if (db.equalsIgnoreCase(database)) {
							databaseExists = true;
							break;
						}
					}
					res.set(databaseExists);
				} catch (SQLException e) {
					throw new PersistenceException("Failed detecting database " + database, e);
				}
			});
		}catch (Exception e) {
			throw new PersistenceException("Failed detecting database " + database, e);
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#schemaExists(java.lang.String)
	 */
	@Override
	public boolean schemaExists(String schema) {

		if( ! switchUrlTemplae(connectionUrlTemplateForInitSchema) ) {
			return true;
		}

		AtomicBoolean res = new AtomicBoolean(true);
		if(notEmpty(schema)) {
			try (StatementExecutor se = connectionFactory.getStatementExecutor()) {
				se.meta(meta -> {
					try(ResultSet schemasRs = meta.getSchemas()) {

						boolean schemaExists = false;
						while( schemasRs.next()) {
							String sch = schemasRs.getString("TABLE_SCHEM");
							if(Objects.equals(sch.toLowerCase(), schema.toLowerCase())) {
								schemaExists = true;
								break;
							}
						}
						res.set(schemaExists);
					} catch (SQLException e) {
						throw new PersistenceException("Failed detecting schema " + schema, e);
					}

				});
			} catch (Exception e) {
				throw new PersistenceException("Failed detecting schema " + schema, e);
			}
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#partTableExists(java.lang.String)
	 */
	@Override
	public boolean partTableExists(String partTable) {


		if( ! switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes) ) {
			return true;
		}

		AtomicBoolean res = new AtomicBoolean(true);
		try (StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.meta(meta -> {
				try(ResultSet tables = meta.getTables(connectionFactory.getDatabase(),null,null,null)) {
					boolean partTableFound   = false;
					while(tables.next()) {
						String tableName = tables.getString("TABLE_NAME");
						if(tableName.equalsIgnoreCase(partTable)) {
							partTableFound = true;
							break;
						}
					}
					res.set(partTableFound);
				} catch (SQLException e) {
					throw new PersistenceException("Failed detecting part table " + partTable, e);
				}
			});
		} catch (Exception e) {
			throw new PersistenceException("Failed detecting part table " + partTable, e);
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#partTableIndexExists(java.lang.String)
	 */
	@Override
	public boolean partTableIndexExists(String partTable, String indexName) {

		if( ! switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes) ) {
			return true;
		}

		AtomicBoolean res = new AtomicBoolean(true);
		try (StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.meta(meta -> {
				try(ResultSet indexRs = meta.getIndexInfo(null, connectionFactory.getSchema(), partTable, false, false)) {
					boolean indexExists = false;
					while(indexRs.next()) {
						String idxName = indexRs.getString("INDEX_NAME");
						if(idxName.equalsIgnoreCase(indexName)) {
							indexExists = true;
							break;
						}
					}
					res.set(indexExists);
				} catch (SQLException e) {
					throw new PersistenceException("Failed detecting part table index " + partTable, e);
				}
			});
		} catch (Exception e) {
			throw new PersistenceException("Failed detecting part table " + partTable, e);
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#completedLogTableExists(java.lang.String)
	 */
	@Override
	public boolean completedLogTableExists(String completedLogTable) {

		if( ! switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes) ) {
			return true;
		}

		AtomicBoolean res = new AtomicBoolean(true);
		try (StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.meta(meta -> {
				try(ResultSet tables = meta.getTables(connectionFactory.getDatabase(),null,null,null)) {
					boolean completedLogTableExists   = false;
					while(tables.next()) {
						String tableName = tables.getString("TABLE_NAME");
						if(tableName.equalsIgnoreCase(completedLogTable)) {
							completedLogTableExists = true;
							break;
						}
					}
					res.set(completedLogTableExists);
				} catch (SQLException e) {
					throw new PersistenceException("Failed detecting completedLogTable " + completedLogTable, e);
				}
			});
		} catch (Exception e) {
			throw new PersistenceException("Failed detecting completed log table " + completedLogTable, e);
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createDatabase(java.lang.String)
	 */
	@Override
	public void createDatabase(String database) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitDatabase,getCreateDatabaseSql(database));
		LOG.info("Created database {}",database);
	}

	/**
	 * Gets the creates the database sql.
	 *
	 * @param database the database
	 * @return the creates the database sql
	 */
	protected String getCreateDatabaseSql(String database) {
		return "CREATE DATABASE "+database;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createSchema(java.lang.String)
	 */
	@Override
	public void createSchema(String schema) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitSchema,getCreateSchemaSql(schema));
		LOG.info("Created schema {}",schema);
	}

	/**
	 * Gets the creates the schema sql.
	 *
	 * @param schema the schema
	 * @return the creates the schema sql
	 */
	protected String getCreateSchemaSql(String schema) {
		return "CREATE SCHEMA "+schema;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createPartTable(java.lang.String)
	 */
	@Override
	public void createPartTable(String partTable) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitTablesAndIndexes,getCreatePartTableSql(partTable));
		LOG.info("Created part table {}",partTable);
	}

	/**
	 * Gets the creates the part table sql.
	 *
	 * @param partTable the part table
	 * @return the creates the part table sql
	 */
	protected String getCreatePartTableSql(String partTable) {
		StringBuilder sb = new StringBuilder("CREATE TABLE ")
				.append(partTable)
				.append(" (");
		
		fields.forEach((col,type) -> sb.append(col).append(" ").append(type).append(",") );
		additionalFields.forEach(f->sb.append(sqlFieldType(f)).append(","));
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createPartTableIndex(java.lang.String)
	 */
	@Override
	public void createPartTableIndex(String partTable) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitTablesAndIndexes,getCreatePartTableIndexSql(partTable));
		LOG.info("Created partTable index {}",partTable);
	}

	/**
	 * Gets the creates the part table index sql.
	 *
	 * @param partTable the part table
	 * @return the creates the part table index sql
	 */
	protected String getCreatePartTableIndexSql(String partTable) {
		return "CREATE INDEX "+indexName(partTable)+" ON "+partTable+"("+EngineDepo.CART_KEY+")";
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createUniqPartTableIndex(java.lang.String, java.util.List)
	 */
	@Override
	public void createUniqPartTableIndex(String partTable, List<String> fields) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitTablesAndIndexes,getCreateUniqPartTableIndexSql(partTable,fields));
		LOG.info("Created unique partTable index {} {}",partTable,fields);
	}

	/**
	 * Gets the creates the uniq part table index sql.
	 *
	 * @param partTable the part table
	 * @param fields    the fields
	 * @return the creates the uniq part table index sql
	 */
	protected String getCreateUniqPartTableIndexSql(String partTable, List<String> fields) {
		String indexName = indexName(partTable,fields);
		return "CREATE UNIQUE INDEX "+indexName+" ON "+partTable+"("+String.join(",", fields)+")";
	}

	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createCompletedLogTable(java.lang.String)
	 */
	@Override
	public void createCompletedLogTable(String completedLogTable) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitTablesAndIndexes,getCompletedLogTableSql(completedLogTable));
		LOG.info("Created completedLogTable {}",completedLogTable);
	}

	/**
	 * Gets the completed log table sql.
	 *
	 * @param completedLogTable the completed log table
	 * @return the completed log table sql
	 */
	protected String getCompletedLogTableSql(String completedLogTable) {
		return "CREATE TABLE "
				+completedLogTable+" ("
				+CART_KEY+" "+fields.get(CART_KEY)+" PRIMARY KEY"
				+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
				+")";
	}

	/**
	 * Gets the engine specific expiration time range.
	 *
	 * @return the engine specific expiration time range
	 */
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}
	
	/**
	 * Q marks.
	 *
	 * @param n the n
	 * @return the string
	 */
	private String qMarks(int n) {
		StringBuilder sb = new StringBuilder();
		sb.append("?,".repeat(Math.max(0, n - 1)));
		sb.append("?");
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#buildPartTableQueries(java.lang.String)
	 */
	@Override
	public void buildPartTableQueries(String partTable) {
		//archiving SQL
		this.deleteFromPartsByIdSql      = "DELETE FROM "+partTable + " WHERE ID IN(?)";
		this.deleteFromPartsByCartKeySql = "DELETE FROM "+partTable + " WHERE CART_KEY IN(?)";
		this.deleteExpiredPartsSql       = "DELETE FROM "+partTable + " WHERE "+getEngineSpecificExpirationTimeRange();
		this.deleteAllPartsSql           = "DELETE FROM "+partTable;
		this.updatePartsByIdSql          = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE ID IN(?)";
		this.updatePartsByCartKeySql     = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE CART_KEY IN(?)";		
		this.updateExpiredPartsSql       = "UPDATE "+partTable+" SET ARCHIVED = 1 WHERE "+getEngineSpecificExpirationTimeRange();
		this.updateAllPartsSql           = "UPDATE "+partTable+" SET ARCHIVED = 1";
		//persistence SQL
		this.saveCartQuery = "INSERT INTO " + partTable + "("
				+ID
				+","+LOAD_TYPE
				+","+CART_KEY
				+","+CART_LABEL
				+","+CREATION_TIME
				+","+EXPIRATION_TIME
				+","+CART_VALUE
				+","+CART_PROPERTIES
				+","+VALUE_TYPE					
				+","+PRIORITY
				+ additionalFields.stream().map(f->","+f.getName()).collect(Collectors.joining(""))
				+") VALUES ("+qMarks(10+additionalFields.size())+")"
				;
		this.getPartQuery = "SELECT "
				+CART_KEY
				+","+CART_VALUE
				+","+CART_LABEL
				+","+CREATION_TIME
				+","+EXPIRATION_TIME
				+","+LOAD_TYPE
				+","+CART_PROPERTIES
				+","+VALUE_TYPE
				+","+PRIORITY
				+" FROM " + partTable 
				+" WHERE "+ID+" IN ( ? ) AND "+ARCHIVED+" = 0"
				;
		this.getExpiredPartQuery = "SELECT "
				+CART_KEY
				+","+CART_VALUE
				+","+CART_LABEL
				+","+CREATION_TIME
				+","+EXPIRATION_TIME
				+","+LOAD_TYPE
				+","+CART_PROPERTIES
				+","+VALUE_TYPE
				+","+PRIORITY
				+" FROM " + partTable 
				+" WHERE "+ getEngineSpecificExpirationTimeRange();
		this.getAllPartIdsQuery = "SELECT "+ID
				+" FROM " + partTable
				+" WHERE "+CART_KEY+" = ? AND "+ARCHIVED+" = 0 ORDER BY "+ID+" ASC";
		this.getAllUnfinishedPartsQuery = "SELECT "
				+CART_KEY
				+","+CART_VALUE
				+","+CART_LABEL
				+","+CREATION_TIME
				+","+EXPIRATION_TIME
				+","+LOAD_TYPE
				+","+CART_PROPERTIES
				+","+VALUE_TYPE
				+","+PRIORITY
				+" FROM " + partTable 
				+" WHERE "+ARCHIVED+" = 0  AND "+LOAD_TYPE+" <> 'STATIC_PART' "+getSortingOrder();
		this.getAllStaticPartsQuery = "SELECT "
				+CART_KEY
				+","+CART_VALUE
				+","+CART_LABEL
				+","+CREATION_TIME
				+","+EXPIRATION_TIME
				+","+LOAD_TYPE
				+","+CART_PROPERTIES
				+","+VALUE_TYPE
				+","+PRIORITY
				+" FROM " + partTable 
				+" WHERE "+ARCHIVED+" = 0 AND "+LOAD_TYPE+" = 'STATIC_PART' ORDER BY "+ID+" ASC";
		this.getNumberOfPartsQuery = "SELECT COUNT(*) FROM " + partTable + " WHERE "+ARCHIVED+" = 0";

	}


	/**
	 * Quote.
	 *
	 * @param key the key
	 * @return the string
	 */
	protected String quote(K key) {
		if(key == null) {
			return "NULL";
		} else {
			return q+ key +q;
		}
	}

	/**
	 * Quote.
	 *
	 * @param keys the keys
	 * @return the string
	 */
	protected String quote(Collection<K> keys) {
		StringBuilder sb = new StringBuilder();
		keys.forEach(id -> sb.append(quote(id)).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}

	/**
	 * Quote number.
	 *
	 * @param number the number
	 * @return the string
	 */
	protected String quoteNumber(Number number) {
		if(number == null) {
			return "NULL";
		} else {
			return String.valueOf(number);
		}
	}

	/**
	 * Quote numbers.
	 *
	 * @param numbers the numbers
	 * @return the string
	 */
	protected String quoteNumbers(Collection<? extends Number> numbers) {
		StringBuilder sb = new StringBuilder();
		numbers.forEach(id -> sb.append(quoteNumber(id)).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#buildCompletedLogTableQueries(java.lang.String)
	 */
	@Override
	//TODO: Replace INSERT with UPSERT
	public void buildCompletedLogTableQueries(String completedLogTable) {
		this.deleteFromCompletedSql = "DELETE FROM "+completedLogTable + " WHERE CART_KEY IN(?)";
		this.deleteAllCompletedSql  = "DELETE FROM "+completedLogTable;
		this.saveCompletedBuildKeyQuery = "INSERT INTO " + completedLogTable + "("+CART_KEY+") VALUES( ? )";
		this.getAllCompletedKeysQuery = "SELECT "+CART_KEY+" FROM "+completedLogTable;

	}

	/**
	 * Not empty.
	 *
	 * @param s the s
	 * @return true, if successful
	 */
	protected boolean notEmpty(String s) {
		return s != null && !s.isEmpty();
	}

	/**
	 * Execute.
	 *
	 * @param sql the sql
	 */
	protected void execute(String sql) {
		try(StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.execute(sql);
		} catch (Exception e) {
			throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
		}
	}

	/**
	 * Connect and execute update.
	 *
	 * @param urlTemplate the url template
	 * @param sql         the sql
	 */
	protected void connectAndExecuteUpdate(String urlTemplate, String sql) {
		if(switchUrlTemplae(urlTemplate)) {
			try (StatementExecutor se = connectionFactory.getStatementExecutor()) {
				LOG.debug("Executing {}", sql);
				se.executeUpdate(sql);
			} catch (Exception e) {
				LOG.error("Failed executing to {}", sql, e);
				throw new PersistenceException(e);
			}
		}
	}

	/**
	 * Execute prepared.
	 *
	 * @param sql      the sql
	 * @param consumer the consumer
	 */
	protected void executePrepared(String sql, Consumer<PreparedStatement> consumer) {
		try(StatementExecutor se = connectionFactory.getStatementExecutor()) {
			se.execute(sql,consumer);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Gets the scalar value.
	 *
	 * @param <T>         the generic type
	 * @param sql         the sql
	 * @param consumer    the consumer
	 * @param transformer the transformer
	 * @return the scalar value
	 */
	protected <T> T getScalarValue(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
		try(StatementExecutor se = connectionFactory.getStatementExecutor()) {
			return se.fetchOne(sql,consumer,transformer);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Gets the collection of values.
	 *
	 * @param <T>         the generic type
	 * @param sql         the sql
	 * @param consumer    the consumer
	 * @param transformer the transformer
	 * @return the collection of values
	 */
	protected <T> List<T> getCollectionOfValues(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
		try(StatementExecutor se = connectionFactory.getStatementExecutor()) {
			return se.fetchMany(sql,consumer,transformer);
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Gets the key sql type.
	 *
	 * @param <K>    the key type
	 * @param kClass the k class
	 * @return the key sql type
	 */
	protected static <K> String getKeySqlType(Class<K> kClass) {
		String keyType = null;
		if(kClass == Integer.class) {
			keyType = "INT";
		} else if(kClass == Long.class) {
			keyType = "BIGINT";
		} else if(kClass == UUID.class) {
			keyType = "CHAR(36)";
		} else if(kClass.isEnum()) {
			int maxLength = 0;
			for(Object o:kClass.getEnumConstants()) {
				maxLength = Math.max(maxLength, o.toString().length());
			}
			keyType = "CHAR("+maxLength+")";
		} else {
			keyType = "VARCHAR(255)";
		}
		return keyType;
	}

	/**
	 * Sql field type.
	 *
	 * @param f the f
	 * @return the string
	 */
	protected static String sqlFieldType(Field<?> f) {
		return f.getName()+" "+getFieldType(f.getFieldClass());
	}

	/**
	 * Gets the field type.
	 *
	 * @param <K>    the key type
	 * @param kClass the k class
	 * @return the field type
	 */
	protected static <K> String getFieldType(Class<K> kClass) {
		String keyType = null;
		if(kClass == Integer.class) {
			keyType = "INT NOT NULL";
		} else if(kClass == Long.class) {
			keyType = "BIGINT NOT NULL";
		} else if(kClass == UUID.class) {
			keyType = "CHAR(36) NOT NULL";
		} else if(kClass.isEnum()) {
			int maxLength = 0;
			for(Object o:kClass.getEnumConstants()) {
				maxLength = Math.max(maxLength, o.toString().length());
			}
			keyType = "CHAR("+maxLength+") NOT NULL";
		} else {
			keyType = "VARCHAR(255) NOT NULL";
		}
		return keyType;
	}


	/**
	 * Sets the host.
	 *
	 * @param host the new host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Sets the port.
	 *
	 * @param port the new port
	 */
	public void setPort(int port) {
		connectionFactory.setPort(port);
		this.port = port;
	}

	/**
	 * Sets the database.
	 *
	 * @param database the new database
	 */
	public void setDatabase(String database) {
		connectionFactory.setDatabase(database);
	}

	/**
	 * Sets the schema.
	 *
	 * @param schema the new schema
	 */
	public void setSchema(String schema) {
		this.connectionFactory.setSchema(schema);
	}

	/**
	 * Sets the user.
	 *
	 * @param user the new user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Sets the password.
	 *
	 * @param password the new password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Sets the properties.
	 *
	 * @param properties the new properties
	 */
	public void setProperties(Properties properties) {
		if(properties != null) {
			connectionFactory.setProperties(properties);
			this.properties.putAll(properties);
		}
	}

	/**
	 * Sets the property.
	 *
	 * @param key   the key
	 * @param value the value
	 */
	public void setProperty(String key, String value) {
		connectionFactory.getProperties().put(key,value);
		this.properties.put(key, value);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteFromCompletedLog(java.util.Collection)
	 */
	@Override
	public void deleteFromCompletedLog(Collection<K> keys) {
		execute(deleteFromCompletedSql.replace("?", quote(keys)));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteAllCompletedLog()
	 */
	@Override
	public void deleteAllCompletedLog() {
		execute(deleteAllCompletedSql);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteFromPartsByIds(java.util.Collection)
	 */
	@Override
	public void deleteFromPartsByIds(Collection<? extends Number> ids) {
		execute(deleteFromPartsByIdSql.replace("?", quoteNumbers(ids)));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteFromPartsByCartKeys(java.util.Collection)
	 */
	@Override
	public void deleteFromPartsByCartKeys(Collection<K> keys) {
		execute(deleteFromPartsByCartKeySql.replace("?", quote(keys)));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteExpiredParts()
	 */
	@Override
	public void deleteExpiredParts() {
		execute(deleteExpiredPartsSql);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#deleteAllParts()
	 */
	@Override
	public void deleteAllParts() {
		execute(deleteAllPartsSql);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#updatePartsByIds(java.util.Collection)
	 */
	@Override
	public void updatePartsByIds(Collection<? extends Number> ids) {
		execute(updatePartsByIdSql.replace("?", quoteNumbers(ids)));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#updatePartsByCartKeys(java.util.Collection)
	 */
	@Override
	public void updatePartsByCartKeys(Collection<K> keys) {
		execute(updatePartsByCartKeySql.replace("?", quote(keys)));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#updateExpiredParts()
	 */
	@Override
	public void updateExpiredParts() {
		execute(updateExpiredPartsSql);		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#updateAllParts()
	 */
	@Override
	public void updateAllParts() {
		switchUrlTemplae(connectionUrlTemplate);
		execute(updateAllPartsSql);		
	}

	/**
	 * Sets the field.
	 *
	 * @param field the field
	 * @param type  the type
	 */
	public void setField(String field, String type) {
		fields.put(field, type);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#saveCart(long, java.lang.String, java.lang.Object, java.lang.Object, java.sql.Timestamp, java.sql.Timestamp, java.lang.Object, java.lang.String, java.lang.String, long)
	 */
	@Override
	public void saveCart(long id, String loadType, Object key, Object label, Timestamp creationTime,
			Timestamp expirationTime, Object value, String properties, String hint, long priority, List<Object> more) {
		executePrepared(saveCartQuery, st->{
			try {

				st.setLong(1, id);
				st.setString(2, loadType);
				st.setObject(3, key);
				st.setObject(4, label);
				st.setTimestamp(5, creationTime);
				st.setTimestamp(6, expirationTime);
				st.setObject(7, value);
				st.setString(8, properties);
				st.setString(9, hint);
				st.setLong(10, priority);
				
				if(more != null && more.size() > 0) {
					for(int i = 11; i < 11+more.size(); i++) {
						st.setObject(i, more.get(i-11));
					}
				}
				
			} catch (SQLException e) {
				throw new PersistenceException("Failed saving cart "+key,e);
			}
		});
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#saveCompletedBuildKey(java.lang.Object)
	 */
	@Override
	public void saveCompletedBuildKey(Object key) {
		executePrepared(saveCompletedBuildKeyQuery, st->{
			try {
				st.setObject(1, key);
			} catch (SQLException e) {
				throw new PersistenceException("Failed saving completed key "+key,e);
			}
		});		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getParts(java.util.Collection, java.util.function.Function)
	 */
	@Override
	public <T> List<T> getParts(Collection<Long> ids, Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getPartQuery.replace("?", quoteNumbers(ids)), st->{}, transformer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getExpiredParts(java.util.function.Function)
	 */
	@Override
	public <T> List<T> getExpiredParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getExpiredPartQuery, st->{}, transformer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getAllPartIds(java.lang.Object)
	 */
	@Override
	public List<Long> getAllPartIds(K key) {
		return getCollectionOfValues(getAllPartIdsQuery, st->{
			try {
				st.setObject(1, key);
			} catch (SQLException e1) {
				throw new PersistenceException(e1);
			}
		}, rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				throw new PersistenceException("Expected non null persistence ID");
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getUnfinishedParts(java.util.function.Function)
	 */
	@Override
	public <T> List<T> getUnfinishedParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getAllUnfinishedPartsQuery, st->{}, transformer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getAllCompletedKeys(java.util.function.Function)
	 */
	@Override
	public Set<K> getAllCompletedKeys(Function<ResultSet, K> transformer) {
		return new LinkedHashSet<>(getCollectionOfValues(getAllCompletedKeysQuery, st->{}, transformer)) ;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getStaticParts(java.util.function.Function)
	 */
	@Override
	public <T> List<T> getStaticParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getAllStaticPartsQuery, st->{}, transformer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#getNumberOfParts()
	 */
	@Override
	public long getNumberOfParts() {
		return getScalarValue(getNumberOfPartsQuery, st->{}, rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}) ;
	}
	
	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		connectionFactory.closeConnection();
	}

	/**
	 * Gets the sorting order.
	 *
	 * @return the sorting order
	 */
	protected String getSortingOrder() {
		if(sortingOrder == null || sortingOrder.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder("ORDER BY ");
		sortingOrder.forEach((k,v)->sb.append(k).append(" ").append(v).append(","));
		if(sb.length() > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#setSortingOrder(java.util.LinkedHashMap)
	 */
	public void setSortingOrder(LinkedHashMap<String, String> order) {
		this.sortingOrder = order;
	}

	/**
	 * Sets the additional fields.
	 *
	 * @param additionalFields the new additional fields
	 */
	public void setAdditionalFields(List<Field<?>> additionalFields) {
		this.additionalFields = additionalFields;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [connection=").append(connectionFactory).append(", properties=")
				.append(properties).append("]");
		return builder.toString();
	}

	/**
	 * Init.
	 */
	protected abstract void init();

	/**
	 * Switch url templae boolean.
	 *
	 * @param temlate the temlate
	 * @return the boolean
	 */
	protected boolean switchUrlTemplae(String temlate) {
		if(notEmpty(temlate)) {
			connectionFactory.setUrlTemplate(temlate);
			return true;
		}
		return false;
	}
}
