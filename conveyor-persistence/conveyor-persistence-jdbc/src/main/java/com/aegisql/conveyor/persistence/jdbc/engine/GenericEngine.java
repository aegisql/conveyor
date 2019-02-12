package com.aegisql.conveyor.persistence.jdbc.engine;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.core.PersistenceException;

// TODO: Auto-generated Javadoc
/**
 * The Class GenericEngine.
 *
 * @param <K> the key type
 */
public abstract class GenericEngine <K> implements EngineDepo <K>  {
	
	/** The Constant LOG. */
	protected static final Logger LOG = LoggerFactory.getLogger(EngineDepo.class);

	/** The Constant LOAD_TYPE_MAX_LENGTH. */
	private static final int LOAD_TYPE_MAX_LENGTH = 15;

	/** The Constant LABEL_MAX_LENGTH. */
	private static final int LABEL_MAX_LENGTH = 100;

	/** The Constant VALUE_CLASS_MAX_LENGTH. */
	private static final int VALUE_CLASS_MAX_LENGTH = 255;
	
	/** The key class. */
	protected final Class<K> keyClass;

	/** The driver. */
	protected final String driver;
	
	/** The connection url template for init database. */
	protected final String connectionUrlTemplateForInitDatabase;
	
	/** The connection url template for init schema. */
	protected final String connectionUrlTemplateForInitSchema;
	
	/** The connection url template for init tables and indexes. */
	protected final String connectionUrlTemplateForInitTablesAndIndexes;
	
	/** The connection url template. */
	protected final String connectionUrlTemplate;

	/** The connection. */
	protected Connection connection; 
	
	/** The host. */
	protected String host;
	
	/** The port. */
	protected int port;
	
	/** The database. */
	protected String database;
	
	/** The schema. */
	protected String schema;
	
	/** The user. */
	protected String user;
	
	/** The password. */
	protected String password;
	
	/** The properties. */
	protected Properties properties = new Properties();
	
	/** The fields. */
	protected final LinkedHashMap<String,String> fields = new LinkedHashMap<>();
	
	/** The delete from completed sql. */
	protected String deleteFromCompletedSql;
	
	/** The delete all completed sql. */
	protected String deleteAllCompletedSql;
	
	/** The delete from parts by id sql. */
	protected String deleteFromPartsByIdSql;
	
	/** The delete from parts by cart key sql. */
	protected String deleteFromPartsByCartKeySql;
	
	/** The delete expired parts sql. */
	protected String deleteExpiredPartsSql;
	
	/** The delete all parts sql. */
	protected String deleteAllPartsSql;
	
	/** The update parts by id sql. */
	protected String updatePartsByIdSql;
	
	/** The update parts by cart key sql. */
	protected String updatePartsByCartKeySql;
	
	/** The update expired parts sql. */
	protected String updateExpiredPartsSql;
	
	/** The update all parts sql. */
	protected String updateAllPartsSql;

	/** The q. */
	protected final String q;

	/** The key sql type. */
	protected final String keySqlType;

	/** The save cart query. */
	private String saveCartQuery;
	
	/** The save completed build key query. */
	private String saveCompletedBuildKeyQuery;
	
	/** The get part query. */
	private String getPartQuery;
	
	/** The get expired part query. */
	private String getExpiredPartQuery;
	
	/** The get all part ids query. */
	private String getAllPartIdsQuery;
	
	/** The get all unfinished parts query. */
	private String getAllUnfinishedPartsQuery;
	
	/** The get all completed keys query. */
	private String getAllCompletedKeysQuery;
	
	/** The get all static parts query. */
	private String getAllStaticPartsQuery;
	
	/** The get number of parts query. */
	private String getNumberOfPartsQuery;
	
	private Map<String,String> sortingOrder = new LinkedHashMap<>();

	/**
	 * Instantiates a new generic engine.
	 *
	 * @param keyClass the key class
	 * @param driver the driver
	 * @param connectionUrlTemplateForInitDatabase the connection url template for init database
	 * @param connectionUrlTemplateForInitSchema the connection url template for init schema
	 * @param connectionUrlTemplateForInitTablesAndIndexes the connection url template for init tables and indexes
	 * @param connectionUrlTemplate the connection url template
	 */
	protected GenericEngine(Class<K> keyClass, String driver, String connectionUrlTemplateForInitDatabase,
			String connectionUrlTemplateForInitSchema, String connectionUrlTemplateForInitTablesAndIndexes,
			String connectionUrlTemplate) {
		this.keyClass = keyClass;
		this.driver = driver;
		this.connectionUrlTemplateForInitDatabase = connectionUrlTemplateForInitDatabase;
		this.connectionUrlTemplateForInitSchema = connectionUrlTemplateForInitSchema;
		this.connectionUrlTemplateForInitTablesAndIndexes = connectionUrlTemplateForInitTablesAndIndexes;
		this.connectionUrlTemplate = connectionUrlTemplate;
		this.keySqlType = getKeySqlType(this.keyClass);
		if(Number.class.isAssignableFrom(keyClass)) {
			this.q = "";
		} else {
			this.q = "'";
		}
		if(this.fields.size() == 0) {
			this.fields.put(ID, "BIGINT PRIMARY KEY");
			this.fields.put(LOAD_TYPE, "CHAR("+LOAD_TYPE_MAX_LENGTH+")");
			this.fields.put(CART_KEY,this.keySqlType);
			this.fields.put(CART_LABEL, "VARCHAR("+LABEL_MAX_LENGTH +")");
			this.fields.put(CREATION_TIME, "DATETIME NOT NULL");
			this.fields.put(EXPIRATION_TIME, "DATETIME NOT NULL");
			this.fields.put(PRIORITY, "BIGINT NOT NULL DEFAULT 0");
			this.fields.put(CART_VALUE, "BLOB");
			this.fields.put(VALUE_TYPE, "VARCHAR("+VALUE_CLASS_MAX_LENGTH+")");
			this.fields.put(CART_PROPERTIES, "TEXT");
			this.fields.put(ARCHIVED, "SMALLINT NOT NULL DEFAULT 0");
		}
		sortingOrder.put(ID, "ASC");
		try {
			if (notEmpty(driver)) {
				Class.forName(driver);
			}
		} catch (ClassNotFoundException e) {
			throw new PersistenceException("Driver not found: " + driver, e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#databaseExists(java.lang.String)
	 */
	@Override
	public boolean databaseExists(String database) {
		AtomicBoolean res = new AtomicBoolean(true);
		connectAndDo(connectionUrlTemplateForInitDatabase, con -> {
			try {
				DatabaseMetaData meta = con.getMetaData();
				ResultSet databaseRs = meta.getCatalogs();
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
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#schemaExists(java.lang.String)
	 */
	@Override
	public boolean schemaExists(String schema) {
		AtomicBoolean res = new AtomicBoolean(true);
		if (notEmpty(schema)) {
			connectAndDo(connectionUrlTemplateForInitSchema, con -> {
				try {
					DatabaseMetaData meta = con.getMetaData();
					ResultSet schemasRs = meta.getSchemas();
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
		}
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#partTableExists(java.lang.String)
	 */
	@Override
	public boolean partTableExists(String partTable) {
		AtomicBoolean res = new AtomicBoolean(true);
		connectAndDo(connectionUrlTemplateForInitTablesAndIndexes, con -> {
			try {
				DatabaseMetaData meta = con.getMetaData();
				ResultSet tables = meta.getTables(database,null,null,null);
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
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#partTableIndexExists(java.lang.String)
	 */
	@Override
	public boolean partTableIndexExists(String partTable) {
		AtomicBoolean res = new AtomicBoolean(true);
		connectAndDo(connectionUrlTemplateForInitTablesAndIndexes, con -> {
			try {
				DatabaseMetaData meta = con.getMetaData();
				ResultSet indexRs = meta.getIndexInfo(null, schema, partTable, false, false);
				boolean indexExists = false;
				while(indexRs.next()) {
					String idxName = indexRs.getString("INDEX_NAME");
					if(idxName.equalsIgnoreCase(partTable+"_IDX")) {
						indexExists = true;
						break;
					}
				}
				res.set(indexExists);
			} catch (SQLException e) {
				throw new PersistenceException("Failed detecting part table index " + partTable, e);
			}
		});
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#completedLogTableExists(java.lang.String)
	 */
	@Override
	public boolean completedLogTableExists(String completedLogTable) {
		AtomicBoolean res = new AtomicBoolean(true);
		connectAndDo(connectionUrlTemplateForInitTablesAndIndexes, con -> {
			try {
				DatabaseMetaData meta = con.getMetaData();
				ResultSet tables = meta.getTables(database,null,null,null);
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
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createDatabase(java.lang.String)
	 */
	@Override
	public void createDatabase(String database) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitDatabase, getCreateDatabaseSql(database));
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
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitSchema), getCreateSchemaSql(schema));
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
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCreatePartTableSql(partTable));
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
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createPartTableIndex(java.lang.String)
	 */
	@Override
	public void createPartTableIndex(String partTable) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCreatePartTableIndexSql(partTable));
		LOG.info("Created partTable index {}",partTable);
	}

	/**
	 * Gets the creates the part table index sql.
	 *
	 * @param partTable the part table
	 * @return the creates the part table index sql
	 */
	protected String getCreatePartTableIndexSql(String partTable) {
		return "CREATE INDEX "+partTable+"_IDX ON "+partTable+"("+EngineDepo.CART_KEY+")";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#createCompletedLogTable(java.lang.String)
	 */
	@Override
	public void createCompletedLogTable(String completedLogTable) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCompletedLogTableSql(completedLogTable));
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
				+") VALUES (?,?,?,?,?,?,?,?,?,?)"
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
				+" WHERE "+ARCHIVED+" = 0  AND "+LOAD_TYPE+" <> 'STATIC_PART' ORDER BY "+getSortingOrder();
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
			return q+key.toString()+q;
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
	public void buildCompletedLogTableQueries(String completedLogTable) {
		this.deleteFromCompletedSql = "DELETE FROM "+completedLogTable + " WHERE CART_KEY IN(?)";
		this.deleteAllCompletedSql  = "DELETE FROM "+completedLogTable;
		this.saveCompletedBuildKeyQuery = "INSERT INTO " + completedLogTable + "("+CART_KEY+") VALUES( ? )";
		this.getAllCompletedKeysQuery = "SELECT "+CART_KEY+" FROM "+completedLogTable;

	}

	/**
	 * To connection url.
	 *
	 * @param template the template
	 * @return the string
	 */
	protected String toConnectionUrl(String template) {
		if (template == null) {
			return null;
		} else {
			return template.replace("{host}", host == null ? "" : host).replace("{port}", "" + port)
					.replace("{database}", database == null ? "" : database)
					.replace("{schema}", schema == null ? "" : schema).replace("{user}", user == null ? "" : user)
					.replace("{password}", password == null ? "" : password);
		}
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
	 * Can use database url.
	 *
	 * @param database the database
	 * @return true, if successful
	 */
	protected boolean canUseDatabaseUrl(String database) {
		return notEmpty(connectionUrlTemplateForInitDatabase) && notEmpty(database);
	}

	/**
	 * Can use schema url.
	 *
	 * @param schema the schema
	 * @return true, if successful
	 */
	protected boolean canUseSchemaUrl(String schema) {
		return notEmpty(connectionUrlTemplateForInitSchema) && notEmpty(schema);
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
	public Connection getConnection() {
		if(connection == null) {
			String connectionUrl = toConnectionUrl(connectionUrlTemplate);
			try {
				connection = DriverManager.getConnection(connectionUrl, properties);
				LOG.info("Connection created: {}",this);
			} catch (SQLException e) {
				throw new PersistenceException("Failed connection to URL " + connectionUrl + " " + properties, e);
			}
		}
		return connection;
	}

	/**
	 * Execute.
	 *
	 * @param sql the sql
	 */
	protected void execute(String sql) {
		try(Statement statement = getConnection().createStatement()) {
			statement.execute(sql);
		} catch (SQLException e) {
			throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
		}
	}
	
	/**
	 * Connect and do.
	 *
	 * @param url the url
	 * @param connectionConsumer the connection consumer
	 */
	protected void connectAndDo(String url, Consumer<Connection> connectionConsumer) {
		String connectionUrl = toConnectionUrl(url);
		LOG.debug("Connecting to {}",connectionUrl);
		if (notEmpty(connectionUrl)) {
			try (Connection con = DriverManager.getConnection(connectionUrl, properties)) {
				connectionConsumer.accept(con);
			} catch (SQLException e) {
				LOG.error("Failed connection to {}",connectionUrl,e);
				throw new PersistenceException("Failed connection to URL " + url + " " + properties, e);
			}
		}
	}

	/**
	 * Connect and do.
	 *
	 * @param connectionConsumer the connection consumer
	 */
	protected void connectAndDo(Consumer<Connection> connectionConsumer) {
		Connection con = getConnection();
		connectionConsumer.accept(con);
	}

	/**
	 * Connect and do.
	 *
	 * @param <T> the generic type
	 * @param connectionConsumer the connection consumer
	 * @return the t
	 */
	protected <T> T connectAndDo(Function<Connection,T> connectionConsumer) {
		Connection con = getConnection();
		return connectionConsumer.apply(con);
	}

	/**
	 * Connect and execute update.
	 *
	 * @param url the url
	 * @param sql the sql
	 */
	protected void connectAndExecuteUpdate(String url, String sql) {
		connectAndDo(url, con->{
			try(Statement statement = con.createStatement()) {
				LOG.debug("Executing {}",sql);
				statement.executeUpdate(sql);
			} catch (SQLException e) {
				LOG.error("Failed executing to {}",sql,e);
				throw new PersistenceException(e);
			}
		});
	}

	/**
	 * Execute.
	 *
	 * @param url the url
	 * @param sql the sql
	 */
	protected void execute(String url, String sql) {
		connectAndDo(url, con->{
			try(Statement statement = con.createStatement()) {
				statement.execute(sql);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}
	
	/**
	 * Execute prepared.
	 *
	 * @param sql the sql
	 * @param consumer the consumer
	 */
	protected void executePrepared(String sql, Consumer<PreparedStatement> consumer) {
		connectAndDo(con->{
			try(PreparedStatement statement = con.prepareStatement(sql)) {
				consumer.accept(statement);
				statement.execute();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}
	
	/**
	 * Gets the scalar value.
	 *
	 * @param <T> the generic type
	 * @param url the url
	 * @param sql the sql
	 * @param consumer the consumer
	 * @param transformer the transformer
	 * @return the scalar value
	 */
	protected <T> T getScalarValue(String url, String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
		return connectAndDo(con->{
			try(PreparedStatement statement = con.prepareStatement(sql)) {
				T t = null;
				consumer.accept(statement);
				ResultSet rs =statement.executeQuery();
				while(rs.next()) {
					if(t != null) {
						throw new PersistenceException("Expected single object for "+t);
					}
					t = transformer.apply(rs);
				}
				return t;
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}


	/**
	 * Gets the scalar value.
	 *
	 * @param <T> the generic type
	 * @param sql the sql
	 * @param consumer the consumer
	 * @param transformer the transformer
	 * @return the scalar value
	 */
	protected <T> T getScalarValue(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
		return connectAndDo(con->{
			try(PreparedStatement statement = con.prepareStatement(sql)) {
				T t = null;
				consumer.accept(statement);
				ResultSet rs =statement.executeQuery();
				while(rs.next()) {
					if(t != null) {
						throw new PersistenceException("Expected single object for "+t);
					}
					t = transformer.apply(rs);
				}
				return t;
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}

	/**
	 * Gets the collection of values.
	 *
	 * @param <T> the generic type
	 * @param sql the sql
	 * @param consumer the consumer
	 * @param transformer the transformer
	 * @return the collection of values
	 */
	protected <T> List<T> getCollectionOfValues(String sql, Consumer<PreparedStatement> consumer, Function<ResultSet,T> transformer) {
		return connectAndDo(con->{
			try(PreparedStatement statement = con.prepareStatement(sql)) {
				List<T> res = new LinkedList<>();
				consumer.accept(statement);
				ResultSet rs =statement.executeQuery();
				while(rs.next()) {
					res.add(transformer.apply(rs));
				}
				return res;
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}
	
	/**
	 * Gets the key sql type.
	 *
	 * @param <K> the key type
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
		this.port = port;
	}

	/**
	 * Sets the database.
	 *
	 * @param database the new database
	 */
	public void setDatabase(String database) {
		this.database = database;
	}

	/**
	 * Sets the schema.
	 *
	 * @param schema the new schema
	 */
	public void setSchema(String schema) {
		this.schema = schema;
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
			this.properties.putAll(properties);
		}
	}

	/**
	 * Sets the property.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void setProperty(String key, String value) {
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
		execute(updateAllPartsSql);		
	}

	/**
	 * Sets the field.
	 *
	 * @param field the field
	 * @param type the type
	 */
	public void setField(String field, String type) {
		fields.put(field, type);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo#saveCart(long, java.lang.String, java.lang.Object, java.lang.Object, java.sql.Timestamp, java.sql.Timestamp, java.lang.Object, java.lang.String, java.lang.String, long)
	 */
	@Override
	public void saveCart(long id, String loadType, Object key, Object label, Timestamp creationTime,
			Timestamp expirationTime, Object value, String properties, String hint, long priority) {
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
	public void close() throws IOException {
		try {
			getConnection().close();
			connection = null;
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	protected String getSortingOrder() {
		StringBuilder sb = new StringBuilder();
		sortingOrder.forEach((k,v)->sb.append(k).append(" ").append(v).append(","));
		if(sb.length() > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}
	
	public void setSortingOrder(LinkedHashMap<String, String> order) {
		Objects.requireNonNull(order,"Sorting order must not be null");
		this.sortingOrder = order;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [connectionUrl=").append(toConnectionUrl(connectionUrlTemplate)).append(", properties=")
				.append(properties).append("]");
		return builder.toString();
	}
	
	

}
