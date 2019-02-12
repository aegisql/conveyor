package com.aegisql.conveyor.persistence.jdbc.builders;

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
import com.aegisql.conveyor.persistence.jdbc.engine.DerbyClientEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.DerbyEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.MariaDbEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.MysqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.PostgresqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.SqliteEngine;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;

// TODO: Auto-generated Javadoc
/**
 * The Class JdbcPersistenceBuilder.
 *
 * @param <K> the key type
 */
public class JdbcPersistenceBuilder<K> {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(JdbcPersistenceBuilder.class);
	
	/** The Constant LOAD_TYPE_MAX_LENGTH. */
	private static final int LOAD_TYPE_MAX_LENGTH = 15;
	
	/** The Constant LABEL_MAX_LENGTH. */
	private static final int LABEL_MAX_LENGTH = 100;
	
	/** The Constant VALUE_CLASS_MAX_LENGTH. */
	private static final int VALUE_CLASS_MAX_LENGTH = 255;

	/** The Constant ID. */
	private static final String ID="ID";
	
	/** The Constant LOAD_TYPE. */
	private static final String LOAD_TYPE="LOAD_TYPE";
	
	/** The Constant CART_KEY. */
	private static final String CART_KEY="CART_KEY";
	
	/** The Constant CART_LABEL. */
	private static final String CART_LABEL="CART_LABEL";
	
	/** The Constant CREATION_TIME. */
	private static final String CREATION_TIME="CREATION_TIME";
	
	/** The Constant EXPIRATION_TIME. */
	private static final String EXPIRATION_TIME="EXPIRATION_TIME";
	
	/** The Constant PRIORITY. */
	private static final String PRIORITY="PRIORITY";
	
	/** The Constant CART_VALUE. */
	private static final String CART_VALUE="CART_VALUE";
	
	/** The Constant VALUE_TYPE. */
	private static final String VALUE_TYPE="VALUE_TYPE";
	
	/** The Constant CART_PROPERTIES. */
	private static final String CART_PROPERTIES="CART_PROPERTIES";
	
	/** The Constant ARCHIVED. */
	private static final String ARCHIVED="ARCHIVED";

	/** The Constant MAIN_FIELDS. */
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

	/** The info builder. */
	private StringBuilder infoBuilder = new StringBuilder();
	
	/** The converter adviser. */
	private ConverterAdviser converterAdviser = new ConverterAdviser<>();


	/** The auto init. */
	//FINAL FIELDS
	private final boolean autoInit;
	
	/** The key class. */
	private final Class<K> keyClass;
	
	/** The key sql type. */
	private final String keySqlType;
	
	/** The engine type. */
	private final String engineType;
	
	/** The host. */
	private final String host;
	
	/** The port. */
	private final int port;
	
	/** The database. */
	private final String database;
	
	/** The schema. */
	private final String schema;
	
	/** The part table. */
	private final String partTable;
	
	/** The completed log table. */
	private final String completedLogTable;
	
	/** The user. */
	private final String user;
	
	/** The password. */
	private final String password;
	
	/** The properties. */
	private final Properties properties;
	
	/** The fields. */
	private final LinkedHashMap<String,String> fields;
	
	/** The additional fields. */
	private final LinkedHashSet<String> additionalFields = new LinkedHashSet<>(); //TODO: add support
	
	/** The id supplier. */
	private final LongSupplier idSupplier;
	
	/** The encryption builder. */
	private final EncryptingConverterBuilder encryptionBuilder;
	
	/** The custom archiver. */
	//archiving settings
	private final Archiver<K> customArchiver;
	
	/** The archive strategy. */
	private final ArchiveStrategy archiveStrategy;
	
	/** The archiving persistence. */
	private final Persistence<K> archivingPersistence;
	
	/** The b log conf. */
	private final BinaryLogConfiguration bLogConf;
	
	/** The label converter. */
	private final ObjectConverter<?,String> labelConverter;
	
	/** The min compact size. */
	private final int minCompactSize;
	
	/** The max batch size. */
	private final int maxBatchSize;
	
	/** The max batch time. */
	private final long maxBatchTime;
	
	/** The non persistent properties. */
	private final Set<String> nonPersistentProperties;
	
	/** The engine depo. */
	private final EngineDepo<K> engineDepo;
	
	private final RestoreOrder restoreOrder;
	
	/**
	 * Instantiates a new jdbc persistence builder.
	 *
	 * @param keyClass the key class
	 */
	public JdbcPersistenceBuilder(Class<K> keyClass) {
		this(TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000)::getId, false, keyClass, null, null, 0, null, null, null, null, null, null, new Properties(), new LinkedHashMap<>(),
				ArchiveStrategy.DELETE,null,null,null,new StringLabelConverter(), new EncryptingConverterBuilder(),
				0,100,60_000,new HashSet<>(), null,RestoreOrder.BY_ID);
	}	
	
	/**
	 * Instantiates a new jdbc persistence builder.
	 *
	 * @param idSupplier the id supplier
	 * @param autoInit the auto init
	 * @param keyClass the key class
	 * @param type the type
	 * @param host the host
	 * @param port the port
	 * @param database the database
	 * @param schema the schema
	 * @param partTable the part table
	 * @param completedLogTable the completed log table
	 * @param user the user
	 * @param password the password
	 * @param properties the properties
	 * @param fields the fields
	 * @param archiveStrategy the archive strategy
	 * @param customArchiver the custom archiver
	 * @param archivingPersistence the archiving persistence
	 * @param bLogConf the b log conf
	 * @param labelConverter the label converter
	 * @param encryptionBuilder the encryption builder
	 * @param minCompactSize the min compact size
	 * @param maxBatchSize the max batch size
	 * @param maxBatchTime the max batch time
	 * @param nonPersistentProperties the non persistent properties
	 * @param engineDepo the engine depo
	 */
	private JdbcPersistenceBuilder(LongSupplier idSupplier, boolean autoInit, Class<K> keyClass, String type, String host, int port,
			String database, String schema, String partTable, String completedLogTable, String user, String password,
			Properties properties, LinkedHashMap<String, String> fields,
			ArchiveStrategy archiveStrategy, Archiver<K> customArchiver, Persistence<K> archivingPersistence, BinaryLogConfiguration bLogConf,
			ObjectConverter<?,String> labelConverter, EncryptingConverterBuilder encryptionBuilder,
			int minCompactSize, int maxBatchSize, long maxBatchTime, Set<String> nonPersistentProperties, EngineDepo<K> engineDepo
			,RestoreOrder restoreOrder
			) {
		Objects.requireNonNull(keyClass,"key class must not be null");
		this.idSupplier = idSupplier;
		this.autoInit = autoInit;
		this.keyClass = keyClass;
		this.engineType = type;
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
		this.engineDepo = engineDepo;
		this.restoreOrder = restoreOrder;
		
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
	
	
	/**
	 * Engine depo.
	 *
	 * @param ed the ed
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> engineDepo(EngineDepo<K> ed) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, ed
				,restoreOrder);
	}

	/**
	 * Id supplier.
	 *
	 * @param sup the sup
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> idSupplier(LongSupplier sup) {
		return new JdbcPersistenceBuilder<>(sup, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}
	
	/**
	 * Auto init.
	 *
	 * @param flag the flag
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> autoInit(boolean flag) {
		return new JdbcPersistenceBuilder<>(idSupplier, flag, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Key class.
	 *
	 * @param keyCls the key cls
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> keyClass(Class<K> keyCls) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyCls, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder).
				setField(CART_KEY, getKeySqlType(keyCls));
	}

	/**
	 * Engine type.
	 *
	 * @param eType the e type
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> engineType(String eType) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, eType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Host.
	 *
	 * @param hst the hst
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> host(String hst) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, hst, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Port.
	 *
	 * @param p the p
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> port(int p) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, p,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Database.
	 *
	 * @param db the db
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> database(String db) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				db, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Schema.
	 *
	 * @param sch the sch
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> schema(String sch) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, sch, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Part table.
	 *
	 * @param partTbl the part tbl
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> partTable(String partTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTbl, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Completed log table.
	 *
	 * @param completedLogTbl the completed log tbl
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> completedLogTable(String completedLogTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTbl, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * User.
	 *
	 * @param usr the usr
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> user(String usr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, usr, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Password.
	 *
	 * @param pwd the pwd
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> password(String pwd) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, pwd,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Properties.
	 *
	 * @param pr the pr
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> properties(Properties pr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(pr), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Sets the property.
	 *
	 * @param key the key
	 * @param value the value
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> setProperty(String key, String value) {
		Properties p = new Properties(properties);
		p.put(key, value);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(p), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Fields.
	 *
	 * @param f the f
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> fields(Map<String, String> f) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Sets the field.
	 *
	 * @param field the field
	 * @param type the type
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> setField(String field, String type) {
		LinkedHashMap<String, String> f = new LinkedHashMap<>(fields);
		f.put(field, type);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Archiver.
	 *
	 * @param archiver the archiver
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> archiver(Archiver<K> archiver) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.CUSTOM, archiver, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Archiver.
	 *
	 * @param bLogConf the b log conf
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> archiver(BinaryLogConfiguration bLogConf) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.MOVE_TO_FILE, null, null, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Archiver.
	 *
	 * @param archivingPersistence the archiving persistence
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> archiver(Persistence<K> archivingPersistence) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.MOVE_TO_PERSISTENCE, null, archivingPersistence, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * No archiving.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> noArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.NO_ACTION, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Sets the archived.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> setArchived() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.SET_ARCHIVED, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Delete archiving.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> deleteArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.DELETE, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}
	
	/**
	 * Label converter.
	 *
	 * @param labelConv the label conv
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> labelConverter(ObjectConverter<?,String> labelConv) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConv,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Label converter.
	 *
	 * @param <L> the generic type
	 * @param enClass the en class
	 * @return the jdbc persistence builder
	 */
	public <L extends Enum<L>> JdbcPersistenceBuilder<K> labelConverter(Class<L> enClass) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, new EnumConverter<>(enClass),
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Encryption secret.
	 *
	 * @param encryptionSecret the encryption secret
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> encryptionSecret(String encryptionSecret) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionSecret(encryptionSecret), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo
				,restoreOrder);
	}

	/**
	 * Encryption secret.
	 *
	 * @param secretKey the secret key
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> encryptionSecret(SecretKey secretKey) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.secretKey(secretKey), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo,restoreOrder);
	}

	/**
	 * Encryption algorithm.
	 *
	 * @param encryptionAlgorithm the encryption algorithm
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> encryptionAlgorithm(String encryptionAlgorithm) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionAlgorithm(encryptionAlgorithm), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo,restoreOrder);
	}

	/**
	 * Encryption transformation.
	 *
	 * @param encryptionTransformation the encryption transformation
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> encryptionTransformation(String encryptionTransformation) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionTransformation(encryptionTransformation), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, engineDepo,restoreOrder);
	}

	/**
	 * Encryption key length.
	 *
	 * @param encryptionKeyLength the encryption key length
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> encryptionKeyLength(int encryptionKeyLength) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionKeyLength(encryptionKeyLength), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, engineDepo,restoreOrder);
	}

	/**
	 * Min compact size.
	 *
	 * @param size the size
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> minCompactSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, size, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Max batch size.
	 *
	 * @param size the size
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> maxBatchSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, size, maxBatchTime, nonPersistentProperties, engineDepo
				,restoreOrder);
	}

	/**
	 * Max batch time.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> maxBatchTime(long time, TimeUnit unit) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, TimeUnit.MILLISECONDS.convert(time, unit), 
				nonPersistentProperties, engineDepo,restoreOrder);
	}

	/**
	 * Max batch time.
	 *
	 * @param duration the duration
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> maxBatchTime(Duration duration) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, duration.toMillis(), nonPersistentProperties,
				engineDepo,restoreOrder);
	}

	/**
	 * Do not save cart properties.
	 *
	 * @param property the property
	 * @param more the more
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> doNotSaveCartProperties(String property,String... more) {
		Set<String> set = new HashSet<>(nonPersistentProperties);
		set.add(property);
		if(more != null) {
			set.addAll(Arrays.asList(more));
		}
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, set, engineDepo,restoreOrder);
	}

	public JdbcPersistenceBuilder<K> restoreOrder(RestoreOrder order) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				engineDepo,order);
	}

	
	/**
	 * Adds the binary converter.
	 *
	 * @param <T> the generic type
	 * @param clas the clas
	 * @param conv the conv
	 * @return the jdbc persistence builder
	 */
	@SuppressWarnings("unchecked")
	public <T> JdbcPersistenceBuilder<K> addBinaryConverter(Class<T> clas, ObjectConverter<T, byte[]> conv) {
		converterAdviser.addConverter(clas, conv);
		return this; //TODO: make converterAdviser immutable
	}

	/**
	 * Adds the binary converter.
	 *
	 * @param <L> the generic type
	 * @param <T> the generic type
	 * @param label the label
	 * @param conv the conv
	 * @return the jdbc persistence builder
	 */
	@SuppressWarnings("unchecked")
	public <L,T> JdbcPersistenceBuilder<K> addBinaryConverter(L label, ObjectConverter<T, byte[]> conv) {
		converterAdviser.addConverter(label, conv);
		return this;
	}

	
	/**
	 * Builds the.
	 *
	 * @return the jdbc persistence
	 * @throws Exception the exception
	 */
	public JdbcPersistence<K> build() throws Exception {
		
		EngineDepo<K> sqlEngine = null;
		if(this.engineDepo != null) {
			sqlEngine = this.engineDepo;
		} else {
			sqlEngine = buildSqlEngine(engineType, keyClass);
		}
		//TODO: replace with depo impl
		if(autoInit) {
			if( ! sqlEngine.databaseExists(database) ) {
				sqlEngine.createDatabase(database);
			}
			if( ! sqlEngine.schemaExists(schema) ) {
				sqlEngine.createSchema(schema);
			}
			if( ! sqlEngine.partTableExists(partTable)) {
				sqlEngine.createPartTable(partTable);
			}
			if( ! sqlEngine.partTableIndexExists(partTable)) {
				sqlEngine.createPartTableIndex(partTable);
			}
			if( ! sqlEngine.completedLogTableExists(completedLogTable)) {
				sqlEngine.createCompletedLogTable(completedLogTable);
			}			
		}
		
		//TODO: replace with depo impl
		Archiver<K> archiver = null;
		switch (archiveStrategy) {
		case CUSTOM:
			archiver = customArchiver;
			break;
		case DELETE:
			archiver = new DeleteArchiver<>(sqlEngine);
			break;
		case SET_ARCHIVED:
			archiver = new SetArchivedArchiver<>(sqlEngine);
			break;
		case MOVE_TO_PERSISTENCE:
			archiver = new PersistenceArchiver<>(sqlEngine, archivingPersistence);
			break;
		case MOVE_TO_FILE:
			archiver = new FileArchiver<>(sqlEngine, bLogConf);
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
				  sqlEngine  
				, idSupplier
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
		synchronized (JdbcPersistenceMBean.mBeanServer) {
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
					public String getEngine() {
						return ""+engineDepo;
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
		}
		return persistence;
	}

	/**
	 * Gets the key sql type.
	 *
	 * @param <K> the key type
	 * @param kClass the k class
	 * @return the key sql type
	 */
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
	
	/**
	 * Builds the sql engine.
	 *
	 * @param type the type
	 * @param kClass the k class
	 * @return the engine depo
	 */
	public EngineDepo<K> buildSqlEngine(String type, Class<K> kClass) {
		GenericEngine<K> engine = null;
		switch (type) {
		case "derby":
			engine = new DerbyEngine<>(kClass);
			break;
		case "derby-client":
			engine = new DerbyClientEngine<>(kClass);
			break;
		case "mysql":
			engine = new MysqlEngine<>(kClass);
			break;
		case "mariadb":
			engine = new MariaDbEngine<>(kClass);
			break;
		case "postgres":
			engine = new PostgresqlEngine<>(kClass);
			break;
		case "sqlite":
			engine = new SqliteEngine<>(kClass);
			break;
		default:
			throw new PersistenceException("pre-setted sql engine is not available for type "+type+".");
		}
		engine.setDatabase(database);
		engine.setSchema(schema);
		if(notEmpty(host)) {
			engine.setHost(host);
		}
		if(port > 0) {
			engine.setPort(port);
		}
		engine.setProperties(properties);
		engine.setUser(user);
		engine.setPassword(password);
		engine.setSortingOrder(restoreOrder.getOrder());
		engine.buildPartTableQueries(partTable);
		engine.buildCompletedLogTableQueries(completedLogTable);
		return engine;
	}
	
	/**
	 * Preset initializer.
	 *
	 * @param <K> the key type
	 * @param type the type
	 * @param kClass the k class
	 * @return the jdbc persistence builder
	 */
	public static <K> JdbcPersistenceBuilder<K> presetInitializer(String type, Class<K> kClass) {
		
		
		JdbcPersistenceBuilder<K> pi = new JdbcPersistenceBuilder<K>(kClass)
				.database(null)
				.partTable("PART")
				.completedLogTable("COMPLETED_LOG")
				.host("localhost")
				.engineType(type)
				;

		switch (type) {
		case "derby":
			return pi.schema("conveyor_db");
		case "derby-client":
			return pi.schema("conveyor_db");
		case "mysql":
		case "mariadb":
			return pi.database("conveyor_db");
		case "postgres":
			return pi.database("conveyor_db").schema("conveyor_db");
		case "sqlite":
			return pi.database("conveyor.db");
		default:
			throw new PersistenceException("pre-setted initializer is not available for type "+type+".");
		}
		
	}
	
	/**
	 * Not empty.
	 *
	 * @param s the s
	 * @return true, if successful
	 */
	private boolean notEmpty(String s) {
		return s != null && !s.isEmpty();
	}

}
