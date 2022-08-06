package com.aegisql.conveyor.persistence.jdbc.builders;

import com.aegisql.conveyor.cart.Cart;
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
import com.aegisql.conveyor.persistence.jdbc.engine.*;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.engine.derby.DerbyClientEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.derby.DerbyEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.derby.DerbyMemoryEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.mariadb.MariaDbEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.mysql.MysqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.postgres.PostgresqlEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.sqlite.SqliteEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.sqlite.SqliteMemoryEngine;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class JdbcPersistenceBuilder.
 *
 * @param <K> the key type
 */
public class JdbcPersistenceBuilder<K> implements Cloneable {
	
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

	/** The info builder. */
	private final StringBuilder infoBuilder = new StringBuilder();
	
	/** The converter adviser. */
	@SuppressWarnings("rawtypes")
	private final ConverterAdviser converterAdviser = new ConverterAdviser<>();


	/** The auto init. */
	private final boolean autoInit;
	
	/** The key class. */
	private final Class<K> keyClass;

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
	
	/** The additionalFields. */
	private final List<Field<?>> additionalFields;
	
	/** The additional additionalFields. */
	private final List<List<String>> uniqueFields;
	
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
	
	private final RestoreOrder restoreOrder;

	private final ConnectionFactory connectionFactory;

	private final GenericEngine<K> jdbcEngine;

	private final boolean poolConnection;
	
	/**
	 * Instantiates a new jdbc persistence builder.
	 *
	 * @param keyClass the key class
	 */
	public JdbcPersistenceBuilder(Class<K> keyClass) {
		this(TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000)::getId, false, keyClass, null, null, 0, null, null, null, null, null, null, new Properties(), new ArrayList<>(),
				ArchiveStrategy.DELETE,null,null,null,new StringLabelConverter(), new EncryptingConverterBuilder(),
				0,100,60_000,new HashSet<>(), RestoreOrder.BY_ID, new ArrayList<>(), null,null,false);
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
	 * @param connectionFactory the connection factory
	 */
	private JdbcPersistenceBuilder(LongSupplier idSupplier, boolean autoInit, Class<K> keyClass, String type, String host, int port,
			String database, String schema, String partTable, String completedLogTable, String user, String password,
			Properties properties, List<Field<?>> fields,
			ArchiveStrategy archiveStrategy, Archiver<K> customArchiver, Persistence<K> archivingPersistence, BinaryLogConfiguration bLogConf,
			ObjectConverter<?,String> labelConverter, EncryptingConverterBuilder encryptionBuilder,
			int minCompactSize, int maxBatchSize, long maxBatchTime, Set<String> nonPersistentProperties
			,RestoreOrder restoreOrder, List<List<String>> uniqueFields, ConnectionFactory connectionFactory,GenericEngine<K> jdbcEngine,boolean poolConnection
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
		this.additionalFields = fields;
		/** The key sql type. */
		String keySqlType = getKeySqlType(this.keyClass);
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
		this.restoreOrder = restoreOrder;
		this.uniqueFields = uniqueFields;
		this.connectionFactory = connectionFactory;
		this.jdbcEngine = jdbcEngine;
		this.poolConnection = poolConnection;
	}

	@Override
	public Object clone() {
			return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
					database, schema, partTable, completedLogTable, user, password,
					new Properties(properties), new ArrayList<>(additionalFields),
					archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
					encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
					,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(pr), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(p), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	/**
	 * Fields.
	 *
	 * @param f the f
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> fields(List<Field<?>> f) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	/**
	 * Sets the field.
	 *
	 * @param <T> the generic type
	 * @param fieldClass the field class
	 * @param name the name
	 * @return the jdbc persistence builder
	 */
	public <T> JdbcPersistenceBuilder<K> addField(Class<T> fieldClass, String name) {
		ArrayList<Field<?>> f = new ArrayList<>(additionalFields);
		f.add( new Field<>(fieldClass,name));
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	public <T> JdbcPersistenceBuilder<K> addField(Class<T> fieldClass, String name, Function<Cart<?,?,?>,T> accessor) {
		ArrayList<Field<?>> f = new ArrayList<>(additionalFields);
		f.add( new Field<>(fieldClass,name,accessor));
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(f), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.CUSTOM, archiver, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.MOVE_TO_FILE, null, null, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.MOVE_TO_PERSISTENCE, null, archivingPersistence, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	/**
	 * No archiving.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> noArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.NO_ACTION, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	/**
	 * Sets the archived.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> setArchived() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.SET_ARCHIVED, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	/**
	 * Delete archiving.
	 *
	 * @return the jdbc persistence builder
	 */
	public JdbcPersistenceBuilder<K> deleteArchiving() {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields), 
				ArchiveStrategy.DELETE, null, null, null, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConv,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, new EnumConverter<>(enClass),
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionSecret(encryptionSecret), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.secretKey(secretKey), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionAlgorithm(encryptionAlgorithm), minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionTransformation(encryptionTransformation), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder.encryptionKeyLength(encryptionKeyLength), minCompactSize, maxBatchSize, maxBatchTime, 
				nonPersistentProperties, restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, size, maxBatchSize, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, size, maxBatchTime, nonPersistentProperties
				,restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, TimeUnit.MILLISECONDS.convert(time, unit), 
				nonPersistentProperties, restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, duration.toMillis(), nonPersistentProperties,
				restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
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
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, set,
				restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	public JdbcPersistenceBuilder<K> restoreOrder(RestoreOrder order) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				order, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	public JdbcPersistenceBuilder<K> uniqueFields(List<List<String>> uf) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields), 
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				restoreOrder, new ArrayList<>(uf),connectionFactory,jdbcEngine,poolConnection);
	}

	public JdbcPersistenceBuilder<K> connectionFactory(ConnectionFactory<? extends DataSource> connectionFactory) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields),
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine, connectionFactory == null ? poolConnection : connectionFactory.isCaching());
	}

	public JdbcPersistenceBuilder<K> jdbcEngine(GenericEngine<K> jdbcEngine) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields),
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	public JdbcPersistenceBuilder<K> driverManagerJdbcConnection() {
		return connectionFactory( ConnectionFactory.driverManagerFactoryInstance());
	}

	public JdbcPersistenceBuilder<K> jdbcConnection(DataSource dataSource) {
		return connectionFactory( ConnectionFactory.cachingFactoryInstance(cf->dataSource));
	}

	public JdbcPersistenceBuilder<K> jdbcConnection(Function<ConnectionFactory<? extends DataSource>,? extends DataSource> initializer) {
		return connectionFactory( ConnectionFactory.cachingFactoryInstance((Function)initializer));
	}

	public JdbcPersistenceBuilder<K> dbcpConnection(DataSource dataSource) {
		return connectionFactory( ConnectionFactory.nonCachingFactoryInstance(cf->dataSource));
	}

	public JdbcPersistenceBuilder<K> dbcpConnection(Function<ConnectionFactory<? extends DataSource>,? extends DataSource> initializer) {
		return connectionFactory( ConnectionFactory.nonCachingFactoryInstance((Function)initializer));
	}

	public JdbcPersistenceBuilder<K> dbcp2Connection() {
		return connectionFactory( ConnectionFactory.DBCP2FactoryInstance());
	}

	public JdbcPersistenceBuilder<K> externalJdbcConnection(Supplier<Connection> connectionSupplier) {
		return connectionFactory( ConnectionFactory.cachingExternalConnectionFactoryInstance(connectionSupplier));
	}

	public JdbcPersistenceBuilder<K> externalDbcpConnection(Supplier<Connection> connectionSupplier) {
		return connectionFactory(ConnectionFactory.nonCachingExternalConnectionFactoryInstance(connectionSupplier));
	}

	public JdbcPersistenceBuilder<K> poolConnection(boolean poolConnection) {
		return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
				database, schema, partTable, completedLogTable, user, password,
				new Properties(properties), new ArrayList<>(additionalFields),
				archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
				encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
				restoreOrder, new ArrayList<>(uniqueFields),connectionFactory,jdbcEngine,poolConnection);
	}

	public JdbcPersistenceBuilder<K> addUniqueFields(String f1, String...more) {
		JdbcPersistenceBuilder<K> newBuilder =  uniqueFields(uniqueFields);
		ArrayList<String> fields = new ArrayList<>();
		fields.add(f1);
		if(more != null) {
			fields.addAll(Arrays.asList(more));
		}
		newBuilder.uniqueFields.add(fields);
		return newBuilder;
	}

	public JdbcPersistenceBuilder<K> addUniqueFields(List<String> fields) {
		JdbcPersistenceBuilder<K> newBuilder =  uniqueFields(uniqueFields);
		newBuilder.uniqueFields.add(fields);
		return newBuilder;
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

	public JdbcPersistenceBuilder<K> init() {
		try( EngineDepo<K> sqlEngine = buildPresetSqlEngine(engineType, keyClass) ) {
			sqlEngine.createDatabaseIfNotExists(database);
			sqlEngine.createSchemaIfNotExists(schema);
			sqlEngine.createPartTableIfNotExists(partTable);
			sqlEngine.createPartTableIndexIfNotExists(partTable);
			sqlEngine.createCompletedLogTableIfNotExists(completedLogTable);
			for (List<String> fields : uniqueFields) {
				sqlEngine.createUniqPartTableIndexIfNotExists(partTable, fields);
			}
			if (sqlEngine == this.jdbcEngine) {
				return this;
			} else {
				return new JdbcPersistenceBuilder<>(idSupplier, autoInit, keyClass, engineType, host, port,
						database, schema, partTable, completedLogTable, user, password,
						new Properties(properties), new ArrayList<>(additionalFields),
						archiveStrategy, customArchiver, archivingPersistence, bLogConf, labelConverter,
						encryptionBuilder, minCompactSize, maxBatchSize, maxBatchTime, nonPersistentProperties,
						restoreOrder, new ArrayList<>(uniqueFields), connectionFactory,jdbcEngine,poolConnection);
			}
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	public String getJMXObjName() {
		return "com.aegisql.conveyor.persistence."+engineType+"."+schema+":type=" + partTable;
	}

	/**
	 * Builds the.
	 *
	 * @return the jdbc persistence
	 * @throws Exception the exception
	 */
	public JdbcPersistence<K> build() throws Exception {

		if(autoInit) {
			init(); //creates its own instance of connection factory
		}
		EngineDepo<K> sqlEngine = buildPresetSqlEngine(engineType, keyClass);
		Archiver<K> archiver = switch (archiveStrategy) {
			case CUSTOM -> customArchiver;
			case DELETE -> new DeleteArchiver<>(sqlEngine);
			case SET_ARCHIVED -> new SetArchivedArchiver<>(sqlEngine);
			case MOVE_TO_PERSISTENCE -> new PersistenceArchiver<>(sqlEngine, archivingPersistence);
			case MOVE_TO_FILE -> new FileArchiver<>(sqlEngine, bLogConf);
			case NO_ACTION -> new DoNothingArchiver<>();
			default -> new DoNothingArchiver<>();
		};

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
				, additionalFields
				);
		
		String objName = getJMXObjName();
		LOG.debug("JMX name {}",objName);
		ObjectName objectName = new ObjectName(objName);
		synchronized (JdbcPersistenceMBean.mBeanServer) {
			if( ! JdbcPersistenceMBean.mBeanServer.isRegistered(objectName)) {
				JdbcPersistenceMBean<K> jdbcMbean = new JdbcPersistenceMBean<>() {
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
						return "" + jdbcEngine;
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
						return "" + customArchiver;
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
		String keyType;
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
	private EngineDepo<K> buildPresetSqlEngine(String type, Class<K> kClass) {
		boolean isCached = ! poolConnection;
		EngineDepo<K> engine = switch (type) {
			case "derby" -> new DerbyEngine<>(kClass, connectionFactory,isCached);
			case "derby-client" -> new DerbyClientEngine<>(kClass, connectionFactory,isCached);
			case "derby-memory" -> new DerbyMemoryEngine<>(kClass, connectionFactory,isCached);
			case "mysql" -> new MysqlEngine<>(kClass, connectionFactory,isCached);
			case "mariadb" -> new MariaDbEngine<>(kClass, connectionFactory,isCached);
			case "postgres" -> new PostgresqlEngine<>(kClass, connectionFactory,isCached);
			case "sqlite" -> new SqliteEngine<>(kClass, connectionFactory,isCached);
			case "sqlite-memory" -> new SqliteMemoryEngine<>(kClass, connectionFactory,isCached);
			case "jdbc" -> Objects.requireNonNull(jdbcEngine,"Custom JDBC GenericEngin is not defined");
			default -> throw new PersistenceException("pre-setted sql engine is not available for type " + type + ".");
		};
		engine.setSortingOrder(restoreOrder.getOrder());
		engine.setAdditionalFields(additionalFields);
		engine.buildPartTableQueries(partTable);
		engine.buildCompletedLogTableQueries(completedLogTable);
		ConnectionFactory connectionFactory = engine.getConnectionFactory();
		if(database != null && ! database.isBlank())connectionFactory.setDatabase(database);
		if(schema != null && ! schema.isBlank())connectionFactory.setSchema(schema);
		if(user != null && ! user.isBlank())connectionFactory.setUser(user);
		if(password != null && ! password.isBlank())connectionFactory.setPassword(password);
		if(host != null && ! host.isBlank())connectionFactory.setHost(host);
		if(port > 0) connectionFactory.setPort(port);
		connectionFactory.setProperties(properties);
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

		return switch (type) {
			case "derby", "derby-client", "derby-memory" -> pi.schema("conveyor_db");
			case "mysql", "mariadb" -> pi.database("conveyor_db");
			case "postgres" -> pi.database("conveyor_db").schema("conveyor_db");
			case "sqlite", "sqlite-memory" -> pi.database("conveyor.db");
			case "jdbc" -> pi;
			default -> throw new PersistenceException("pre-setted initializer is not available for type " + type + ".");
		};
		
	}
	

}
