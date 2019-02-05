package com.aegisql.conveyor.persistence.jdbc.engine;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class GenericEngine <K> implements EngineDepo <K>  {
	
	protected static final Logger LOG = LoggerFactory.getLogger(EngineDepo.class);

	private static final int LOAD_TYPE_MAX_LENGTH = 15;

	private static final int LABEL_MAX_LENGTH = 100;

	private static final int VALUE_CLASS_MAX_LENGTH = 255;
	
	protected final Class<K> keyClass;

	protected final String driver;
	protected final String connectionUrlTemplateForInitDatabase;
	protected final String connectionUrlTemplateForInitSchema;
	protected final String connectionUrlTemplateForInitTablesAndIndexes;
	protected final String connectionUrlTemplate;

	protected Connection connection; 
	
	protected String host;
	protected int port;
	protected String database;
	protected String schema;
	protected String user;
	protected String password;
	protected Properties properties = new Properties();
	
	protected final LinkedHashMap<String,String> fields = new LinkedHashMap<>();
	
	protected String deleteFromCompletedSql;
	protected String deleteAllCompletedSql;
	protected String deleteFromPartsByIdSql;
	protected String deleteFromPartsByCartKeySql;
	protected String deleteExpiredPartsSql;
	protected String deleteAllPartsSql;
	protected String updatePartsByIdSql;
	protected String updatePartsByCartKeySql;
	protected String updateExpiredPartsSql;
	protected String updateAllPartsSql;

	protected final String q;

	protected final String keySqlType;

	private String saveCartQuery;
	private String saveCompletedBuildKeyQuery;
	private String getPartQuery;
	private String getExpiredPartQuery;
	private String getAllPartIdsQuery;
	private String getAllUnfinishedPartsQuery;
	private String getAllCompletedKeysQuery;
	private String getAllStaticPartsQuery;
	private String getNumberOfPartsQuery;

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
		try {
			if (notEmpty(driver)) {
				Class.forName(driver);
			}
		} catch (ClassNotFoundException e) {
			throw new PersistenceException("Driver not found: " + driver, e);
		}
	}

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

	@Override
	public void createDatabase(String database) {
		connectAndExecuteUpdate(connectionUrlTemplateForInitDatabase, getCreateDatabaseSql(database));
	}

	protected String getCreateDatabaseSql(String database) {
		return "CREATE DATABASE "+database;
	}

	@Override
	public void createSchema(String schema) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitSchema), getCreateSchemaSql(schema));
	}

	protected String getCreateSchemaSql(String schema) {
		return "CREATE SCHEMA "+schema;
	}

	@Override
	public void createPartTable(String partTable) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCreatePartTableSql(partTable));
	}

	protected String getCreatePartTableSql(String partTable) {
		StringBuilder sb = new StringBuilder("CREATE TABLE ")
				.append(partTable)
				.append(" (");
		
		fields.forEach((col,type) -> sb.append(col).append(" ").append(type).append(",") );
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");
		return sb.toString();
	}

	@Override
	public void createPartTableIndex(String partTable) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCreatePartTableIndexSql(partTable));
	}

	protected String getCreatePartTableIndexSql(String partTable) {
		return "CREATE INDEX "+partTable+"_IDX ON "+partTable+"("+EngineDepo.CART_KEY+")";
	}

	@Override
	public void createCompletedLogTable(String completedLogTable) {
		connectAndExecuteUpdate(toConnectionUrl(connectionUrlTemplateForInitTablesAndIndexes), getCompletedLogTableSql(completedLogTable));
	}

	protected String getCompletedLogTableSql(String completedLogTable) {
		return "CREATE TABLE "
				+completedLogTable+" ("
				+CART_KEY+" "+fields.get(CART_KEY)+" PRIMARY KEY"
				+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
				+")";
	}

	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}
	
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
				+" WHERE "+ARCHIVED+" = 0  AND "+LOAD_TYPE+" <> 'STATIC_PART' ORDER BY "+ID+" ASC";
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


	protected String quote(K key) {
		if(key == null) {
			return "NULL";
		} else {
			return q+key.toString()+q;
		}
	}

	protected String quote(Collection<K> keys) {
		StringBuilder sb = new StringBuilder();
		keys.forEach(id -> sb.append(quote(id)).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}
	
	protected String quoteNumber(Number number) {
		if(number == null) {
			return "NULL";
		} else {
			return String.valueOf(number);
		}
	}

	protected String quoteNumbers(Collection<? extends Number> numbers) {
		StringBuilder sb = new StringBuilder();
		numbers.forEach(id -> sb.append(quoteNumber(id)).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}
	
	@Override
	public void buildCompletedLogTableQueries(String completedLogTable) {
		this.deleteFromCompletedSql = "DELETE FROM "+completedLogTable + " WHERE CART_KEY IN(?)";
		this.deleteAllCompletedSql  = "DELETE FROM "+completedLogTable;
		this.saveCompletedBuildKeyQuery = "INSERT INTO " + completedLogTable + "("+CART_KEY+") VALUES( ? )";
		this.getAllCompletedKeysQuery = "SELECT "+CART_KEY+" FROM "+completedLogTable;

	}

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

	protected boolean notEmpty(String s) {
		return s != null && !s.isEmpty();
	}

	protected boolean canUseDatabaseUrl(String database) {
		return notEmpty(connectionUrlTemplateForInitDatabase) && notEmpty(database);
	}

	protected boolean canUseSchemaUrl(String schema) {
		return notEmpty(connectionUrlTemplateForInitSchema) && notEmpty(schema);
	}

	public Connection getConnection() {
		if(connection == null) {
			String connectionUrl = toConnectionUrl(connectionUrlTemplate);
			try {
				connection = DriverManager.getConnection(connectionUrl, properties);
			} catch (SQLException e) {
				throw new PersistenceException("Failed connection to URL " + connectionUrl + " " + properties, e);
			}
		}
		return connection;
	}

	protected void execute(String sql) {
		try(Statement statement = getConnection().createStatement()) {
			statement.execute(sql);
		} catch (SQLException e) {
			throw new PersistenceException("Failed executing "+sql+"; "+e.getMessage(),e);
		}
	}
	
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

	protected void connectAndDo(Consumer<Connection> connectionConsumer) {
		Connection con = getConnection();
		connectionConsumer.accept(con);
	}

	protected <T> T connectAndDo(Function<Connection,T> connectionConsumer) {
		Connection con = getConnection();
		return connectionConsumer.apply(con);
	}

	protected void connectAndExecuteUpdate(String url, String sql) {
		connectAndDo(url, con->{
			try(Statement statement = con.createStatement()) {
				LOG.debug("Executing{}",sql);
				statement.executeUpdate(sql);
			} catch (SQLException e) {
				LOG.error("Failed executing to {}",sql,e);
				throw new PersistenceException(e);
			}
		});
	}

	protected void execute(String url, String sql) {
		connectAndDo(url, con->{
			try(Statement statement = con.createStatement()) {
				statement.execute(sql);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
	}
	
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


	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setProperties(Properties properties) {
		if(properties != null) {
			this.properties.putAll(properties);
		}
	}

	public void setProperty(String key, String value) {
		this.properties.put(key, value);
	}

	@Override
	public void deleteFromCompletedLog(Collection<K> keys) {
		execute(deleteFromCompletedSql.replace("?", quote(keys)));
	}

	@Override
	public void deleteAllCompletedLog() {
		execute(deleteAllCompletedSql);
	}

	@Override
	public void deleteFromPartsByIds(Collection<? extends Number> ids) {
		execute(deleteFromPartsByIdSql.replace("?", quoteNumbers(ids)));
	}

	@Override
	public void deleteFromPartsByCartKeys(Collection<K> keys) {
		execute(deleteFromPartsByCartKeySql.replace("?", quote(keys)));
	}

	@Override
	public void deleteExpiredParts() {
		execute(deleteExpiredPartsSql);
	}

	@Override
	public void deleteAllParts() {
		execute(deleteAllPartsSql);
	}

	@Override
	public void updatePartsByIds(Collection<? extends Number> ids) {
		execute(updatePartsByIdSql.replace("?", quoteNumbers(ids)));
	}

	@Override
	public void updatePartsByCartKeys(Collection<K> keys) {
		execute(updatePartsByCartKeySql.replace("?", quote(keys)));
	}

	@Override
	public void updateExpiredParts() {
		execute(updateExpiredPartsSql);		
	}

	@Override
	public void updateAllParts() {
		execute(updateAllPartsSql);		
	}

	public void setField(String field, String type) {
		fields.put(field, type);
	}

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

	@Override
	public <T> List<T> getParts(Collection<Long> ids, Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getPartQuery.replace("?", quoteNumbers(ids)), st->{}, transformer);
	}

	@Override
	public <T> List<T> getExpiredParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getExpiredPartQuery, st->{}, transformer);
	}

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

	@Override
	public <T> List<T> getUnfinishedParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getAllUnfinishedPartsQuery, st->{}, transformer);
	}

	@Override
	public Set<K> getAllCompletedKeys(Function<ResultSet, K> transformer) {
		return new LinkedHashSet<>(getCollectionOfValues(getAllCompletedKeysQuery, st->{}, transformer)) ;
	}

	@Override
	public <T> List<T> getStaticParts(Function<ResultSet, T> transformer) {
		return getCollectionOfValues(getAllStaticPartsQuery, st->{}, transformer);
	}

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
}
