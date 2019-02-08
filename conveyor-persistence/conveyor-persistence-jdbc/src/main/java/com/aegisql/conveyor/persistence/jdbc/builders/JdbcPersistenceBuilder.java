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
import com.aegisql.conveyor.persistence.jdbc.engine.MysqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.PostgresqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.SqliteEngine;
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

	private StringBuilder infoBuilder = new StringBuilder();
	private ConverterAdviser converterAdviser = new ConverterAdviser<>();


	//FINAL FIELDS
	private final boolean autoInit;
	private final Class<K> keyClass;
	private final String keySqlType;
	private final String engineType;
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
	
	private final EngineDepo<K> engineDepo;
	
	public JdbcPersistenceBuilder(Class<K> keyClass) {
		this(TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000)::getId, false, keyClass, null, null, 0, null, null, null, null, null, null, new Properties(), new LinkedHashMap<>(),
				ArchiveStrategy.DELETE,null,null,null,new StringLabelConverter(), new EncryptingConverterBuilder(),
				0,100,60_000,new HashSet<>(), null);
	}	
	
	private JdbcPersistenceBuilder(LongSupplier idSupplier, boolean autoInit, Class<K> keyClass, String type, String host, int port,
			String database, String schema, String partTable, String completedLogTable, String user, String password,
			Properties properties, LinkedHashMap<String, String> fields,
			ArchiveStrategy archiveStrategy, Archiver<K> customArchiver, Persistence<K> archivingPersistence, BinaryLogConfiguration bLogConf,
			ObjectConverter<?,String> labelConverter, EncryptingConverterBuilder encryptionBuilder,
			int minCompactSize, int maxBatchSize, long maxBatchTime, Set<String> nonPersistentProperties, EngineDepo<K> engineDepo
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
	
	
	public JdbcPersistenceBuilder<K> engineDepo(EngineDepo<K> ed) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, ed);
	}

	public JdbcPersistenceBuilder<K> idSupplier(LongSupplier sup) {
		return new JdbcPersistenceBuilder<>(sup, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}
	
	public JdbcPersistenceBuilder<K> autoInit(boolean flag) {
		return new JdbcPersistenceBuilder<>(idSupplier, flag, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> keyClass(Class<K> keyCls) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyCls, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo).
				setField(CART_KEY, getKeySqlType(keyCls));
	}

	public JdbcPersistenceBuilder<K> engineType(String eType) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, eType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> host(String hst) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, hst, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> port(int p) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, p,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> database(String db) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				db, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> schema(String sch) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, sch, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> partTable(String partTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTbl, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> completedLogTable(String completedLogTbl) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTbl, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> user(String usr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, usr, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> password(String pwd) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, pwd,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> properties(Properties pr) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(pr), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> setProperty(String key, String value) {
		Properties p = new Properties(properties);
		p.put(key, value);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(p), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> fields(Map<String, String> f) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> setField(String field, String type) {
		LinkedHashMap<String, String> f = new LinkedHashMap<>(fields);
		f.put(field, type);
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> archiver(Archiver<K> archiver) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.CUSTOM, archiver, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> archiver(BinaryLogConfiguration bLogConf) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.MOVE_TO_FILE, null, null, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> archiver(Persistence<K> archivingPersistence) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.MOVE_TO_PERSISTENCE, null, archivingPersistence, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> noArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.NO_ACTION, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> setArchived() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.SET_ARCHIVED, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> deleteArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				ArchiveStrategy.DELETE, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}
	
	public JdbcPersistenceBuilder<K> labelConverter(ObjectConverter<?,String> labelConv) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConv,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public <L extends Enum<L>> JdbcPersistenceBuilder<K> labelConverter(Class<L> enClass) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, new EnumConverter<>(enClass),
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> encryptionSecret(String encryptionSecret) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionSecret(encryptionSecret), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo);
	}

	public JdbcPersistenceBuilder<K> encryptionSecret(SecretKey secretKey) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.secretKey(secretKey), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo);
	}

	public JdbcPersistenceBuilder<K> encryptionAlgorithm(String encryptionAlgorithm) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionAlgorithm(encryptionAlgorithm), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				, engineDepo);
	}

	public JdbcPersistenceBuilder<K> encryptionTransformation(String encryptionTransformation) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionTransformation(encryptionTransformation), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> encryptionKeyLength(int encryptionKeyLength) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionKeyLength(encryptionKeyLength), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> minCompactSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, size, maxBatchSize, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> maxBatchSize(int size) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, size, maxBatchTime, nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> maxBatchTime(long time, TimeUnit unit) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, TimeUnit.MILLISECONDS.convert(time, unit), 
				nonPersistentProperties, engineDepo);
	}

	public JdbcPersistenceBuilder<K> maxBatchTime(Duration duration) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new LinkedHashMap<>(fields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, duration.toMillis(), nonPersistentProperties, engineDepo);
	}

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
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, set, engineDepo);
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
		engine.buildPartTableQueries(partTable);
		engine.buildCompletedLogTableQueries(completedLogTable);
		return engine;
	}
	
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
			return pi.database("conveyor_db");
		case "postgres":
			return pi.database("conveyor_db").schema("conveyor_db");
		case "sqlite":
			return pi.database("conveyor.db");
		default:
			throw new PersistenceException("pre-setted initializer is not available for type "+type+".");
		}
		
	}
	
	private boolean notEmpty(String s) {
		return s != null && !s.isEmpty();
	}

}
