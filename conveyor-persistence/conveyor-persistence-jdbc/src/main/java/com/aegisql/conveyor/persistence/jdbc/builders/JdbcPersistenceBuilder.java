package com.aegisql.conveyor.persistence.jdbc.builders;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import javax.crypto.SecretKey;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.archive.DoNothingArchiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import com.aegisql.conveyor.persistence.jdbc.JdbcPersistence;
import com.aegisql.conveyor.persistence.jdbc.JdbcPersistenceMBean;
import com.aegisql.conveyor.persistence.jdbc.archive.DeleteArchiver;
import com.aegisql.conveyor.persistence.jdbc.archive.FileArchiver;
import com.aegisql.conveyor.persistence.jdbc.archive.PersistenceArchiver;
import com.aegisql.conveyor.persistence.jdbc.archive.SetArchivedArchiver;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.converters.StringLabelConverter;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;

public class JdbcPersistenceBuilder<K> {
	
	private static final Logger LOG = LoggerFactory.getLogger(JdbcPersistenceBuilder.class);
	
	private static final int LOAD_TYPE_MAX_LENGTH = 15;
	private static final int LABEL_MAX_LENGTH = 100;
	private static final int VALUE_CLASS_MAX_LENGTH = 255;

	private static final String ID="ID";
	private static final String LOAD_TYPE="LOAD_TYPE";
	private static final String CART_KEY="CART_KEY";
	private static final String CART_LABEL="CART_LABEL";
	private static final String CREATION_TIME="CREATION_TIME";
	private static final String EXPIRATION_TIME="EXPIRATION_TIME";
	private static final String PRIORITY="PRIORITY";
	private static final String CART_VALUE="CART_VALUE";
	private static final String VALUE_TYPE="VALUE_TYPE";
	private static final String CART_PROPERTIES="CART_PROPERTIES";
	private static final String ARCHIVED="ARCHIVED";

	private static final LinkedHashSet<String> MAIN_FIELDS = new LinkedHashSet<>();
	static {
		MAIN_FIELDS.add(ID);
		MAIN_FIELDS.add(LOAD_TYPE);
		MAIN_FIELDS.add(CART_KEY);
		MAIN_FIELDS.add(CART_LABEL);
		MAIN_FIELDS.add(CREATION_TIME);
		MAIN_FIELDS.add(EXPIRATION_TIME);
		MAIN_FIELDS.add(PRIORITY);
		MAIN_FIELDS.add(CART_VALUE);
		MAIN_FIELDS.add(VALUE_TYPE);
		MAIN_FIELDS.add(CART_PROPERTIES);
		MAIN_FIELDS.add(ARCHIVED);
	}

	public static final ConnectionTemplate DERBY_EMBEDDED_URL_TEMPLATE = new ConnectionTemplate(
			"org.apache.derby.jdbc.EmbeddedDriver",
			"",
			"jdbc:derby:{schema};create=true",
			"jdbc:derby:{schema};",
			"jdbc:derby:{schema};"
			);

	public static final ConnectionTemplate DERBY_CLIENT_URL_TEMPLATE = new ConnectionTemplate(
			"org.apache.derby.jdbc.ClientDriver",
			"",
			"",
			"jdbc:derby://{host}:{port}/{schema};create=true",
			"jdbc:derby://{host}:{port}/{schema};"
			);

	public static final ConnectionTemplate MYSQL_URL_TEMPLATE = new ConnectionTemplate(
			"com.mysql.cj.jdbc.Driver",
			"",
			"jdbc:mysql://{host}:{port}/",
			"jdbc:mysql://{host}:{port}/{schema}",
			"jdbc:mysql://{host}:{port}/{schema}"
			);
	
	public static final ConnectionTemplate SQLITE_EMBEDDED_URL_TEMPLATE = new ConnectionTemplate(
			"org.sqlite.JDBC",
			"",
			"",
			"jdbc:sqlite:{schema}",
			"jdbc:sqlite:{schema}"
			);

	public static final ConnectionTemplate POSTGRES_EMBEDDED_URL_TEMPLATE = new ConnectionTemplate(
			"org.postgresql.Driver",
			"jdbc:postgresql://{host}:{port}/",
			"jdbc:postgresql://{host}:{port}/{database}",
			"jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}",
			"jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}"
			);

	public static class ConnectionTemplate {
		private final String driver;
		private final String connectionUrlTemplateForInitDatabase;
		private final String connectionUrlTemplateForInitSchema;
		private final String connectionUrlTemplateForInitTablesAndIndexes;
		private final String connectionUrlTemplate;
		private ConnectionTemplate(
				String driver,
				String connectionUrlTemplateForInitDatabase,
				String connectionUrlTemplateForInitSchema, String connectionUrlTemplateForInitTablesAndIndexes,
				String connectionUrlTemplate) {
			this.driver = driver;
			this.connectionUrlTemplateForInitDatabase = connectionUrlTemplateForInitDatabase;
			this.connectionUrlTemplateForInitSchema = connectionUrlTemplateForInitSchema;
			this.connectionUrlTemplateForInitTablesAndIndexes = connectionUrlTemplateForInitTablesAndIndexes;
			this.connectionUrlTemplate = connectionUrlTemplate;
		}
		public String getConnectionUrlTemplateForInitDatabase() {
			return connectionUrlTemplateForInitDatabase;
		}
		public String getConnectionUrlTemplateForInitSchema() {
			return connectionUrlTemplateForInitSchema;
		}
		public String getConnectionUrlTemplateForInitTablesAndIndexes() {
			return connectionUrlTemplateForInitTablesAndIndexes;
		}
		public String getConnectionUrlTemplate() {
			return connectionUrlTemplate;
		}
		public String getDriver() {
			return driver;
		}
		
	}
	
	private String createPartTableSql;
	private String completedLogTableSql;
	private String createPartTableIndexSql;
	private StringBuilder infoBuilder = new StringBuilder();
	private ConverterAdviser converterAdviser = new ConverterAdviser<>();


	//FINAL FIELDS
	private final boolean autoInit;
	private final Class<K> keyClass;
	private final String keySqlType;
	private final String engineType;
	private final String driver;
	private final String host;
	private final int port;
	private final String database;
	private final String schema;
	private final String partTable;
	private final String completedLogTable;
	private final String user;
	private final String password;
	private final Properties properties;
	private final LinkedHashMap<String,String> fields;
	private final LinkedHashSet<String> additionalFields = new LinkedHashSet<>(); //TODO: add support
	private final ConnectionTemplate connectionUrlTemplate;
	private final LongSupplier idSupplier;
	private final EncryptingConverterBuilder encryptionBuilder;
	
	//archiving settings
	private final Archiver<K> customArchiver;
	private final ArchiveStrategy archiveStrategy;
	private final Persistence<K> archivingPersistence;
	private final BinaryLogConfiguration bLogConf;
	private final ObjectConverter<?,String> labelConverter;
	
	private final int minCompactSize;
	private final int maxBatchSize;
	private final long maxBatchTime;
	private final Set<String> nonPersistentProperties;
	
	public JdbcPersistenceBuilder(Class<K> keyClass) {
		this(TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000)::getId, false, keyClass, null, null, null, 0, null, null, null, null, null, null, new Properties(), new LinkedHashMap<>(), null,
				ArchiveStrategy.DELETE,null,null,null,new StringLabelConverter(), new EncryptingConverterBuilder(),
				0,100,60_000,new HashSet<>());
	}	
	
	private JdbcPersistenceBuilder(LongSupplier idSupplier, boolean autoInit, Class<K> keyClass, String type, String driver, String host, int port,
			String database, String schema, String partTable, String completedLogTable, String user, String password,
			Properties properties, LinkedHashMap<String, String> fields, ConnectionTemplate connectionUrlTemplate,
			ArchiveStrategy archiveStrategy, Archiver<K> customArchiver, Persistence<K> archivingPersistence, BinaryLogConfiguration bLogConf,
			ObjectConverter<?,String> labelConverter, EncryptingConverterBuilder encryptionBuilder,
			int minCompactSize, int maxBatchSize, long maxBatchTime, Set<String> nonPersistentProperties
			) {
		Objects.requireNonNull(keyClass,"key class must not be null");
		this.idSupplier = idSupplier;
		this.autoInit = autoInit;
		this.keyClass = keyClass;
		this.engineType = type;
		this.driver = driver;
		this.host = host;
		this.port = port;
		this.database = database;
		this.schema = schema;
		this.partTable = partTable;
		this.completedLogTable = completedLogTable;
		this.user = user;
		this.password = password;
		this.properties = properties;
		this.fields = fields;
		this.connectionUrlTemplate = connectionUrlTemplate;
		this.keySqlType = getKeySqlType(this.keyClass);
		this.archiveStrategy = archiveStrategy;
		this.customArchiver = customArchiver;
		this.archivingPersistence = archivingPersistence;
		this.bLogConf = bLogConf;
		this.labelConverter = labelConverter;
		this.encryptionBuilder = encryptionBuilder;
		this.minCompactSize = minCompactSize;
		this.maxBatchSize = maxBatchSize;
		this.maxBatchTime = maxBatchTime;
		this.nonPersistentProperties = nonPersistentProperties;
		
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
		} else {
			this.additionalFields.addAll(this.fields.keySet());
			this.additionalFields.removeAll(MAIN_FIELDS);
		}
		
	}
	
	public void preInit() {
		Objects.requireNonNull(connectionUrlTemplate, "Required connectionUrlTemplate");
		try {
			Class.forName(connectionUrlTemplate.getDriver());
		} catch (ClassNotFoundException e) {
			throw new PersistenceException("Driver not found: "+connectionUrlTemplate.getDriver(), e);
		}
	}
	
	public void initDatabase() {
		if(database != null && ! database.isEmpty()) {
			try(Connection con = getConnection(connectionUrlTemplate::getConnectionUrlTemplateForInitDatabase)) {
				LOG.debug("Connected!");
				DatabaseMetaData meta = con.getMetaData();
				ResultSet databaseRs = meta.getCatalogs();
				boolean databaseExists = false;
				while( databaseRs.next()) {
					String db = databaseRs.getString("TABLE_CAT");
					if(Objects.equals(db.toLowerCase(), database.toLowerCase())) {
						databaseExists = true;
						break;
					}
				}
				if( ! databaseExists ) {
					try(Statement statement = con.createStatement()) {
						statement.executeUpdate(createSchemaSql());
					}
				}
				try(Statement statement = con.createStatement()) {
					statement.executeUpdate(createDatabaseSql());
				}
			} catch (SQLException e) {
				throw new PersistenceException("Failed creation of database "+database+" "+connectionUrlTemplate.getConnectionUrlTemplateForInitDatabase(), e);
			}
		}
	}
	
	public void initSchema() {
		if(schema != null && ! schema.isEmpty()) {
			try(Connection con = getConnection(connectionUrlTemplate::getConnectionUrlTemplateForInitSchema)) {
				LOG.debug("Connected!");
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
				if( ! schemaExists ) {
					try(Statement statement = con.createStatement()) {
						statement.executeUpdate(createSchemaSql());
					}
				}
			} catch (SQLException e) {
				throw new PersistenceException("Failed creation of schema "+schema+" "+connectionUrlTemplate.getConnectionUrlTemplateForInitSchema(), e);
			}			
		}
	}
	
	public void initTables() {
		try(Connection con = getConnection(connectionUrlTemplate::getConnectionUrlTemplateForInitTablesAndIndexes)) {
			LOG.debug("Connected!");
			DatabaseMetaData meta = con.getMetaData();
			
			ResultSet tables = meta.getTables(null,null,null,null);
			boolean partTableFound   = false;
			boolean keyLogTableFound = false;
			while(tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				if(tableName.equalsIgnoreCase(partTable)) {
					partTableFound = true;
				}
				if(tableName.equalsIgnoreCase(completedLogTable)) {
					keyLogTableFound = true;
				}
			}

			try(Statement statement = con.createStatement()) {
				if( ! partTableFound ) {
					statement.executeUpdate(createPartTableSql());
				}
				if( ! keyLogTableFound ) {
					statement.executeUpdate(createCompletedLogTableSql());
				}
			}			
		} catch (SQLException e) {
			throw new PersistenceException("Failed creation of tables "+connectionUrlTemplate.getConnectionUrlTemplateForInitTablesAndIndexes(), e);
		}			
	}
	
	public void initIndexes() {
		try(Connection con = getConnection(connectionUrlTemplate::getConnectionUrlTemplateForInitTablesAndIndexes)) {
			LOG.debug("Connected!");
			DatabaseMetaData meta = con.getMetaData();
			ResultSet indexRs = meta.getIndexInfo(null, schema, partTable, false, false);
			boolean indexExists = false;
			while(indexRs.next()) {
				String idxName = indexRs.getString("INDEX_NAME");
				if(Objects.equals(idxName, partTable+"_IDX")) {
					indexExists = true;
					break;
				}
			}
			if( ! indexExists) {
				try(Statement statement = con.createStatement()) {
					statement.executeUpdate(createPartTableIndexSql());
				}			
			}
		} catch (SQLException e) {
			throw new PersistenceException("Failed creation of indexes "+connectionUrlTemplate.getConnectionUrlTemplateForInitTablesAndIndexes(), e);
		}			
	}

	private Connection getConnection(Supplier<String> urlSupplier) {
		String url = convertTemplate(urlSupplier.get());
		try {
			return DriverManager.getConnection(url, properties);
		} catch (SQLException e) {
			throw new PersistenceException("Connection to "+url+" "+properties+" failed.", e);
		}
	}
	
	public Connection getConnection() {
		return getConnection(connectionUrlTemplate::getConnectionUrlTemplate);
	}
	
	private String convertTemplate(String template) {
		if(template==null) {
			return null;
		} else {
			return template
					.replace("{host}", host == null ? "":host)
					.replace("{port}", ""+port)
					.replace("{database}", database == null ? "":database)
					.replace("{schema}", schema == null ? "":schema)
					.replace("{user}", user == null ? "":user)
					.replace("{password}", password == null ? "":password)
					;
		}
	}

	public JdbcPersistenceBuilder<K> idSupplier(LongSupplier sup) {
		return new JdbcPersistenceBuilder<>(sup, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}
	
	public JdbcPersistenceBuilder<K> autoInit(boolean flag) {
		return new JdbcPersistenceBuilder<>(idSupplier, flag, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> keyClass(Class<K> keyCls) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyCls, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties).
				setField(CART_KEY, getKeySqlType(keyCls));
	}

	public JdbcPersistenceBuilder<K> engineType(String eType) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, eType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> driver(String drv) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, drv, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> host(String hst) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, hst, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> port(int p) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, p,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> database(String db) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				db, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> schema(String sch) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, sch, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> partTable(String partTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTbl, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> completedLogTable(String completedLogTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTbl, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> user(String usr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, usr, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> password(String pwd) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, pwd,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> properties(Properties pr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(pr), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> setProperty(String key, String value) {
		Properties p = new Properties(properties);
		p.put(key, value);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(p), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> fields(Map<String, String> f) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> setField(String field, String type) {
		LinkedHashMap<String, String> f = new LinkedHashMap<>(fields);
		f.put(field, type);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> connectionUrlTemplate(ConnectionTemplate connectionUrlTmpl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTmpl,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> archiver(Archiver<K> archiver) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.CUSTOM, archiver, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> archiver(BinaryLogConfiguration bLogConf) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.MOVE_TO_FILE, null, null, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> archiver(Persistence<K> archivingPersistence) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.MOVE_TO_PERSISTENCE, null, archivingPersistence, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> noArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.NO_ACTION, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> setArchived() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.SET_ARCHIVED, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> deleteArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				ArchiveStrategy.DELETE, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}
	
	public JdbcPersistenceBuilder<K> labelConverter(ObjectConverter<?,String> labelConv) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConv,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public <L extends Enum<L>> JdbcPersistenceBuilder<K> labelConverter(Class<L> enClass) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, new EnumConverter<>(enClass),
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> encryptionSecret(String encryptionSecret) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionSecret(encryptionSecret), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> encryptionSecret(SecretKey secretKey) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.secretKey(secretKey), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> encryptionAlgorithm(String encryptionAlgorithm) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionAlgorithm(encryptionAlgorithm), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> encryptionTransformation(String encryptionTransformation) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionTransformation(encryptionTransformation), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> encryptionKeyLength(int encryptionKeyLength) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionKeyLength(encryptionKeyLength), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> minCompactSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, size, maxBatchSize, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> maxBatchSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, size, maxBatchTime, nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> maxBatchTime(long time, TimeUnit unit) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, TimeUnit.MILLISECONDS.convert(maxBatchTime, unit), nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> maxBatchTime(Duration duration) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, duration.toMillis(), nonPersistentProperties);
	}

	public JdbcPersistenceBuilder<K> doNotSaveCartProperties(String property,String... more) {
		Set<String> set = new HashSet<>(nonPersistentProperties);
		set.add(property);
		if(more != null) {
			set.addAll(Arrays.asList(more));
		}
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, driver, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), connectionUrlTemplate,
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, set);
	}

	
	public String createPartTableSql() {
		
		if(createPartTableSql == null) {
			StringBuilder sb = new StringBuilder("CREATE TABLE ")
					.append(partTable)
					.append(" (");
			
			fields.forEach((col,type)->{
				sb.append(col).append(" ").append(type).append(",");
			});
			sb.deleteCharAt(sb.lastIndexOf(","));
			sb.append(")");
			
			createPartTableSql = sb.toString();
		}
		
		return createPartTableSql;
	}

	private String createCompletedLogTableSql() {
		if(completedLogTableSql==null) {
			completedLogTableSql = "CREATE TABLE "
					+completedLogTable+" ("
					+"CART_KEY "+keySqlType+" PRIMARY KEY"
					+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
					+")";
		}
		return completedLogTableSql;
	}

	private String createPartTableIndexSql() {
		if(createPartTableIndexSql==null) {
			createPartTableIndexSql = "CREATE INDEX "+partTable+"_IDX ON "+partTable+"(CART_KEY)";
		}
		return createPartTableIndexSql;
	}

	private String createDatabaseSql() {
		if(database != null) {
			return "CREATE DATABASE "+database;
		} else {
			return null;
		}
	}

	public String createSchemaSql() {
		if(schema != null) {
			return "CREATE SCHEMA "+schema;
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> JdbcPersistenceBuilder<K> addBinaryConverter(Class<T> clas, ObjectConverter<T, byte[]> conv) {
		converterAdviser.addConverter(clas, conv);
		return this; //TODO: make converterAdviser immutable
	}

	@SuppressWarnings("unchecked")
	public <L,T> JdbcPersistenceBuilder<K> addBinaryConverter(L label, ObjectConverter<T, byte[]> conv) {
		converterAdviser.addConverter(label, conv);
		return this;
	}

	
	public JdbcPersistence<K> build() throws Exception {
		if(autoInit) {
			preInit();
			initDatabase();
			initSchema();
			initTables();
			initIndexes();
		}
		
		Archiver<K> archiver = null;
		switch (archiveStrategy) {
		case CUSTOM:
			archiver = customArchiver;
			break;
		case DELETE:
			archiver = new DeleteArchiver<>(keyClass, partTable, completedLogTable);
			break;
		case SET_ARCHIVED:
			archiver = new SetArchivedArchiver<>(keyClass, partTable, completedLogTable);
			break;
		case MOVE_TO_PERSISTENCE:
			archiver = new PersistenceArchiver<>(keyClass, partTable, completedLogTable, archivingPersistence);
			break;
		case MOVE_TO_FILE:
			archiver = new FileArchiver<>(keyClass, partTable, completedLogTable, bLogConf);
			break;
		case NO_ACTION:
		default:
			archiver = new DoNothingArchiver<>();
			break;
		}
		
		converterAdviser.setEncryptor(encryptionBuilder.get());
		
		infoBuilder.append(engineType.toUpperCase()).append(" Persistence ");
		infoBuilder.append("[schema=").append(schema).append(" ");
		infoBuilder.append("partsTable=").append(partTable).append(" ");
		infoBuilder.append("completedTable=").append(completedLogTable).append(" ");
		infoBuilder.append("archiveStrategy=").append(archiveStrategy).append(" ");
		infoBuilder.append("encryption=").append(encryptionBuilder.get() != null?"ON":"OFF").append("]");

		
		JdbcPersistence<K> persistence = new JdbcPersistence<>(
				  new ConnectionSupplier(convertTemplate(connectionUrlTemplate.connectionUrlTemplate),properties)
				, idSupplier
				, new GenericPersistenceSql(partTable, completedLogTable)
				, archiver
				, labelConverter
				, converterAdviser
				, maxBatchSize
				, maxBatchTime
				, infoBuilder.toString()
				, nonPersistentProperties
				, minCompactSize
				);
		
		String objName = "com.aegisql.conveyor.persistence."+engineType+"."+schema+":type=" + partTable;
		LOG.debug("JMX name {}",objName);
		ObjectName objectName = new ObjectName(objName);
		if( ! JdbcPersistenceMBean.mBeanServer.isRegistered(objectName)) {
			JdbcPersistenceMBean<K> jdbcMbean = new JdbcPersistenceMBean<K>() {
				@Override
				public String getSchema() {
					return schema;
				}
				
				@Override
				public String getPartTable() {
					return partTable;
				}
				
				@Override
				public String getCompleteTable() {
					return completedLogTable;
				}
				
				@Override
				public String getArchiveStrategy() {
					return archiveStrategy.name();
				}

				@Override
				public boolean isEncrypted() {
					return encryptionBuilder.get() != null;
				}

				@Override
				public String getDriver() {
					return driver;
				}

				@Override
				public String getHost() {
					return host;
				}

				@Override
				public int getPort() {
					return port;
				}

				@Override
				public int getMaxBatchSize() {
					return maxBatchSize;
				}

				@Override
				public long getMaxBatchTime() {
					return maxBatchTime;
				}

				@Override
				public Persistence<K> get() {
					return persistence;
				}

				@Override
				public String getEngineType() {
					return engineType;
				}

				@Override
				public String getDatabase() {
					return database;
				}

				@Override
				public String getArchiveStrategyDetails() {
					return ""+customArchiver;
				}

				@Override
				public int minCompactSize() {
					return minCompactSize;
				}

			};
			StandardMBean mbean = new StandardMBean(jdbcMbean, JdbcPersistenceMBean.class);
			JdbcPersistenceMBean.mBeanServer.registerMBean(mbean, objectName);
		}
		return persistence;
	}

	private static <K> String getKeySqlType(Class<K> kClass) {
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
	
	public static <K> JdbcPersistenceBuilder<K> presetInitializer(String type, Class<K> kClass) {
		
		
		JdbcPersistenceBuilder<K> pi = new JdbcPersistenceBuilder<K>(kClass)
				.database(null)
				.schema("conveyor_db")
				.partTable("PART")
				.completedLogTable("COMPLETED_LOG")
				.host("localhost")
				.engineType(type)
				;

		switch (type) {
		case "derby":
			return pi.connectionUrlTemplate(DERBY_EMBEDDED_URL_TEMPLATE)
					.setField(CART_PROPERTIES, "CLOB")
					.setField(CREATION_TIME, "TIMESTAMP")
					.setField(EXPIRATION_TIME, "TIMESTAMP");
		case "derby-client":
			return pi.connectionUrlTemplate(DERBY_CLIENT_URL_TEMPLATE)
					.setField(CART_PROPERTIES, "CLOB")
					.port(1527);
		case "sqlite":
			return pi.connectionUrlTemplate(SQLITE_EMBEDDED_URL_TEMPLATE);
		case "mysql":
			return pi.connectionUrlTemplate(MYSQL_URL_TEMPLATE)
					.port(3306);
		case "postgres":
			return pi.connectionUrlTemplate(POSTGRES_EMBEDDED_URL_TEMPLATE)
					.port(5432);
		default:
			throw new PersistenceException("pre-setted initializer is not available for type "+type+".");
		}
		
		
	}
	
	
}
