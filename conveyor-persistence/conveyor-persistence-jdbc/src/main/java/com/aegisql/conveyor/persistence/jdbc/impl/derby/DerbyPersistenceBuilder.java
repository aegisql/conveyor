package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.archive.DoNothingArchiver;
import com.aegisql.conveyor.persistence.archive.UnimplementedArchiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.converters.StringConverter;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.archive.DeleteArchiver;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.archive.FileArchiver;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.archive.PersistenceArchiver;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.archive.SetArchivedArchiver;
import com.aegisql.id_builder.IdSource;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;

/**
 * The Class DerbyPersistenceBuilder.
 *
 * @param <K> the key type
 */
public class DerbyPersistenceBuilder<K> {
	
	public BinaryLogConfiguration bLogConf;

	/** The archive table. */
	public Persistence<K> archivePersistence;
	
	/** The archiver. */
	public Archiver<K> archiver;

	/**
	 * Instantiates a new derby persistence builder.
	 *
	 * @param clas the clas
	 */
	public DerbyPersistenceBuilder(Class<K> clas) {
		this.keyClass = clas;
		if(clas == Integer.class) {
			this.keyType = "CART_KEY INT";
		} else if(clas == Long.class) {
			this.keyType = "CART_KEY BIGINT";
		} else if(clas == UUID.class) {
			this.keyType = "CART_KEY CHAR(36)";
		} else if(clas.isEnum()) {
			int maxLength = 0;
			for(Object o:clas.getEnumConstants()) {
				maxLength = Math.max(maxLength, o.toString().length());
			}
			this.keyType = "CART_KEY CHAR("+maxLength+")";
		} else {
			this.keyType = "CART_KEY VARCHAR(255)";
		}
		
		if(idSupplier == null) {
			final IdSource idGen = TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000);
			idSupplier = idGen::getId;
		}
	}
	
	/** The key class. */
	private Class<K> keyClass;
	
	/** The do nothing archiver. */
	private Archiver<K> DO_NOTHING_ARCHIVER = new DoNothingArchiver<>();

	/** The unimplemented archiver. */
	//TODO: remove when all implemented
	private Archiver<K> UNIMPLEMENTED_ARCHIVER = new UnimplementedArchiver();

	/** The key type. */
	private String keyType = "CART_KEY VARCHAR(255)";
	
	/** The Constant PROTOCOL. */
	private final static String PROTOCOL = "jdbc:derby:";
	
	/** The Constant PORT. */
	private final static int PORT = 1527;
	
	/** The Constant EMBEDDED. */
	private final static String EMBEDDED = "org.apache.derby.jdbc.EmbeddedDriver";
	
	/** The Constant CLIENT. */
	private final static String CLIENT   = "org.apache.derby.jdbc.ClientDriver";
	
	/** The Constant EMBEDDED_URL_PATTERN. */
	private final static String EMBEDDED_URL_PATTERN = PROTOCOL+"{schema}";
	
	/** The Constant CLIENT_URL_PATTERN. */
	private final static String CLIENT_URL_PATTERN   = PROTOCOL+"//{host}:{port}/{schema}";//;user=judy;password=no12see";
	
	/** The embedded. */
	private boolean embedded  = true;
	
	/** The driver. */
	private String driver     = EMBEDDED; //default
	
	/** The url pattern. */
	private String urlPattern = EMBEDDED_URL_PATTERN; //default
	
	/** The host. */
	private String host       = "localhost"; //default
	
	/** The username. */
	private String username   = "";
	
	/** The password. */
	private String password   = "";
	
	/** The port. */
	private int port          = 0; //default
	
	/** The create. */
	private boolean create    = true;
	
	/** The schema. */
	private String schema      = "conveyor_db";
	
	/** The part table. */
	private String partTable   = "PART";
	
	/** The completed log table. */
	private String completedLogTable = "COMPLETED_LOG";
	
	/** The id supplier. */
	private LongSupplier idSupplier;
	
	private int minCompactSize = 0;

	/** The max batch size. */
	private int maxBatchSize  = 100;
	
	/** The max batch time. */
	private long maxBatchTime = 60_000;
	
	private ConverterAdviser converterAdviser;
	
	/** The archive strategy. */
	ArchiveStrategy archiveStrategy = ArchiveStrategy.DELETE;
	
	/** The encryption secret. */
	private String encryptionSecret = null;
	
	/** The secret key. */
	private SecretKey secretKey;
	
	/** The encryption algorithm. */
	private String encryptionAlgorithm = "AES";
	
	/** The encryption transformation. */
	private String encryptionTransformation = "AES/ECB/PKCS5Padding";
	
	/** The encryption cipher. */
	private Cipher encryptionCipher;
	
	/** The encryption key length. */
	private int encryptionKeyLength = 16;

	private StringBuilder infoBuilder = new StringBuilder("DerbyPersistence ");
	
	private Set<String> nonPersistentProperties = new HashSet<>();
	

	/** The label converter. */
	private ObjectConverter<?,String> labelConverter = new StringConverter<String>() {
		@Override
		public String fromPersistence(String p) {
			return p;
		}

		@Override
		public String conversionHint() {
			return "?:String";
		}
	};
	
	/**
	 * Embedded.
	 *
	 * @param embedded the embedded
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> embedded(boolean embedded) {
		if(embedded) {
			this.embedded = true;
			this.driver = EMBEDDED;
			this.urlPattern = EMBEDDED_URL_PATTERN;
			this.port = 0;
		} else {
			this.embedded = false;
			this.driver = CLIENT;
			this.urlPattern = CLIENT_URL_PATTERN;
			if(this.port != 0) {
				this.port = PORT;
			}
		}
		return this;
	}

	/**
	 * Username.
	 *
	 * @param username the username
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> username(String username) {
		this.username = username;
		return this;
	}
	
	/**
	 * Password.
	 *
	 * @param password the password
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> password(String password) {
		this.password = password;
		return this;
	}

	/**
	 * Schema.
	 *
	 * @param schema the schema
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> schema(String schema) {
		this.schema = schema;
		return this;
	}

	/**
	 * Part table.
	 *
	 * @param table the table
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> partTable(String table) {
		this.partTable = table;
		return this;
	}

	/**
	 * Min compact size.
	 *
	 * @param size the size
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> minCompactSize(int size) {
		this.minCompactSize = size;
		return this;
	}

	/**
	 * Completed log table.
	 *
	 * @param table the table
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> completedLogTable(String table) {
		this.completedLogTable = table;
		return this;
	}

	/**
	 * Port.
	 *
	 * @param port the port
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Id supplier.
	 *
	 * @param idSupplier the id supplier
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> idSupplier(LongSupplier idSupplier) {
		this.idSupplier = idSupplier;
		return this;
	}
	
	/**
	 * When archive records.
	 *
	 * @return the archive builder
	 */
	public ArchiveBuilder<K> whenArchiveRecords() {
		return new ArchiveBuilder<>(this);
	}
	
	/**
	 * Encryption secret.
	 *
	 * @param secret the secret
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> encryptionSecret(String secret) {
		this.encryptionSecret = secret;
		return this;
	}

	/**
	 * Encryption secret.
	 *
	 * @param secret the secret
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> encryptionSecret(SecretKey secret) {
		this.secretKey = secret;
		return this;
	}

	/**
	 * Encryption algorithm.
	 *
	 * @param algorithm the algorithm
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> encryptionAlgorithm(String algorithm) {
		this.encryptionAlgorithm = algorithm;
		return this;
	}

	/**
	 * Encryption transformation.
	 *
	 * @param algorithm the algorithm
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> encryptionTransformation(String algorithm) {
		this.encryptionTransformation = algorithm;
		return this;
	}

	/**
	 * Encryption key length.
	 *
	 * @param keyLenght the key lenght
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> encryptionKeyLength(int keyLenght) {
		this.encryptionKeyLength = keyLenght;
		return this;			
	}

	/**
	 * Label converter.
	 *
	 * @param <L> the generic type
	 * @param enClass the en class
	 * @return the derby persistence builder
	 */
	public <L extends Enum<L>> DerbyPersistenceBuilder<K> labelConverter(Class<L> enClass) {
		this.labelConverter = new EnumConverter<>(enClass);
		return this;
	}
	
	/**
	 * Label converter.
	 *
	 * @param <L> the generic type
	 * @param labelConverter the label converter
	 * @return the derby persistence builder
	 */
	public <L> DerbyPersistenceBuilder<K> labelConverter(ObjectConverter<L,String> labelConverter) {
		this.labelConverter = labelConverter;
		return this;
	}

	/**
	 * Max batch size.
	 *
	 * @param maxBatchSize the max batch size
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> maxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
		return this;
	}

	/**
	 * Max batch time.
	 *
	 * @param maxBatchTime the max batch time
	 * @param unit the unit
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> maxBatchTime(long maxBatchTime, TimeUnit unit) {
		this.maxBatchTime = TimeUnit.MILLISECONDS.convert(maxBatchTime, unit);
		return this;
	}

	/**
	 * Max batch time.
	 *
	 * @param duration the duration
	 * @return the derby persistence builder
	 */
	public DerbyPersistenceBuilder<K> maxBatchTime(Duration duration) {
		this.maxBatchTime = duration.toMillis();
		return this;
	}
	
	private ConverterAdviser getConverterAdviser() {
		if(converterAdviser == null) {
			converterAdviser = new ConverterAdviser<>();
		}
		return converterAdviser;
	}
	
	public <T> DerbyPersistenceBuilder<K> addBinaryConverter(Class<T> clas, ObjectConverter<T, byte[]> conv) {
		getConverterAdviser().addConverter(clas, conv);
		return this;
	}

	public <L,T> DerbyPersistenceBuilder<K> addBinaryConverter(L label, ObjectConverter<T, byte[]> conv) {
		getConverterAdviser().addConverter(label, conv);
		return this;
	}

	public <L,T> DerbyPersistenceBuilder<K> doNotSaveProperties(String property,String... properties) {
		nonPersistentProperties.add(property);
		if(properties != null) {
			nonPersistentProperties.addAll(Arrays.asList(properties));
		}
		return this;
	}
	

	/**
	 * Builds the.
	 *
	 * @return the derby persistence
	 * @throws Exception the exception
	 */
	public DerbyPersistence<K> build() throws Exception {
		DerbyPersistence.LOG.debug("DERBY PERSISTENCE");

		Class.forName(driver);
		Properties properties = new Properties();
		String url = urlPattern;
		url = url.replace("{schema}", schema);
		if(embedded && create) {
			properties.setProperty("create", "true");
		} else {
			url = url.replace("{host}", host);
			url = url.replace("{port}", ""+port);
			if( ! username.isEmpty()) {
				properties.setProperty("user", username);
			}
			if( ! password.isEmpty()) {
				properties.setProperty("password", password);
			}
		}
		
		DerbyPersistence.LOG.debug("Driver: {}",driver);
		DerbyPersistence.LOG.debug("Connection Url: {}",url);
		DerbyPersistence.LOG.debug("Schema: {}",schema);
		
		Connection conn = DriverManager.getConnection(url, properties);
		DatabaseMetaData meta = conn.getMetaData();
		DerbyPersistence.LOG.debug("Connected!");
		
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
		if( ! partTableFound ) {
			try(Statement st = conn.createStatement() ) {
				String sql = "CREATE TABLE "
							+partTable+" ("
							+"ID BIGINT PRIMARY KEY"
							+",LOAD_TYPE CHAR(15)"
							+","+keyType
							+",CART_LABEL VARCHAR(100)"
							+",CREATION_TIME TIMESTAMP NOT NULL"
							+",EXPIRATION_TIME TIMESTAMP NOT NULL"
							+",PRIORITY BIGINT NOT NULL DEFAULT 0"
							+",CART_VALUE BLOB"
							+",VALUE_TYPE VARCHAR(255)"
							+",CART_PROPERTIES CLOB"
							+",ARCHIVED SMALLINT NOT NULL DEFAULT 0"
							+")";
				DerbyPersistence.LOG.debug("Table '{}' not found. Trying to create...\n{}",partTable,sql);
				st.execute(sql);
				DerbyPersistence.LOG.debug("Table '{}' created",partTable);
				st.execute("CREATE INDEX "+partTable+"_IDX ON "+partTable+"(CART_KEY)");
				DerbyPersistence.LOG.debug("Index "+partTable+"_IDX ON "+partTable+"(CART_KEY) created");

			} 
		} else {
			DerbyPersistence.LOG.debug("Table '{}' already exists",partTable);
		}

		if( ! keyLogTableFound ) {
			try(Statement st = conn.createStatement() ) {
				String sql = "CREATE TABLE "
							+completedLogTable+" ("
							+keyType+" PRIMARY KEY"
							+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
							+")";
				DerbyPersistence.LOG.debug("Table '{}' not found. Trying to create...\n{}",completedLogTable,sql);
				st.execute(sql);
				DerbyPersistence.LOG.debug("Table '{}' created",completedLogTable);
			} 
		} else {
			DerbyPersistence.LOG.debug("Table '{}' already exists",completedLogTable);
		}

		String saveCartQuery = "INSERT INTO " + partTable + "("
				+"ID"
				+",LOAD_TYPE"
				+",CART_KEY"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",CART_VALUE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"					
				+",PRIORITY"
				+") VALUES (?,?,?,?,?,?,?,?,?,?)"
				;
		
		String saveCompletedBuildKeyQuery = "INSERT INTO " + completedLogTable + "( CART_KEY ) VALUES( ? )"
				;
		String getPartQuery = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+",PRIORITY"
				+" FROM " + partTable + " WHERE ID IN ( ? ) AND ARCHIVED = 0"
				;

		String getExpiredPartQuery = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+",PRIORITY"
				+" FROM " + partTable + " WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP"
				;

		String getAllPartIdsQuery = "SELECT"
				+" ID"
				+" FROM " + partTable + " WHERE CART_KEY = ? AND ARCHIVED = 0 ORDER BY ID ASC"
				;
		String getAllUnfinishedPartIdsQuery = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+",PRIORITY"
				+" FROM " + partTable + " WHERE ARCHIVED = 0  AND LOAD_TYPE <> 'STATIC_PART' ORDER BY ID ASC"
				;
		String getAllCompletedKeysQuery = "SELECT CART_KEY FROM "+completedLogTable;
		
		String getAllStaticPartsQuery = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+",PRIORITY"
				+" FROM " + partTable + " WHERE ARCHIVED = 0 AND LOAD_TYPE = 'STATIC_PART' ORDER BY ID ASC";
		
		switch(archiveStrategy) {
			case CUSTOM:
				//defined by user
				break;
			case DELETE:
				archiver = new DeleteArchiver<>(keyClass, partTable,completedLogTable);
				break;
			case SET_ARCHIVED:
				archiver = new SetArchivedArchiver<>(keyClass, partTable,completedLogTable);
				break;
			case MOVE_TO_PERSISTENCE:
				archiver = new PersistenceArchiver<>(keyClass, partTable, completedLogTable, archivePersistence, converterAdviser, new DeleteArchiver<>(keyClass, partTable, completedLogTable));
				break;
			case MOVE_TO_FILE: 
				archiver = new FileArchiver<>(keyClass, partTable, completedLogTable, bLogConf, converterAdviser,new DeleteArchiver<>(keyClass, partTable, completedLogTable));
				break;
			case NO_ACTION:
				archiver = DO_NOTHING_ARCHIVER;
				break;
			default:
				
		}

		if(encryptionSecret != null) {
			DerbyPersistence.LOG.debug("VALUES ENCRIPTION ON");
			if(secretKey == null) {
				byte[] key = encryptionSecret.getBytes("UTF-8");
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				key = sha.digest(key);
				key = Arrays.copyOf(key, encryptionKeyLength);
				secretKey = new SecretKeySpec(key, encryptionAlgorithm);
				encryptionCipher = Cipher.getInstance(encryptionTransformation);
				getConverterAdviser().setEncryptor(secretKey, encryptionCipher);
			}
		} else {
			DerbyPersistence.LOG.debug("VALUES ENCRIPTION OFF");
		}
		
		infoBuilder.append("[schema=").append(schema).append(" ");
		infoBuilder.append("partsTable=").append(partTable).append(" ");
		infoBuilder.append("completedTable=").append(completedLogTable).append(" ");
		infoBuilder.append("archiveStrategy=").append(archiveStrategy).append(" ");
		infoBuilder.append("encryption=").append(encryptionSecret != null?"ON":"OFF").append("]");
		
		DerbyPersistence<K> result = new DerbyPersistence<K>(
				this
				,conn
				,idSupplier
				,saveCartQuery
				,saveCompletedBuildKeyQuery
				,getPartQuery
				,getExpiredPartQuery
				,getAllPartIdsQuery
				,getAllUnfinishedPartIdsQuery
				,getAllCompletedKeysQuery
				,getAllStaticPartsQuery
				,"SELECT COUNT(*) FROM " + partTable + " WHERE ARCHIVED = 0"
				,archiver
				,labelConverter
				//,blobConverter
				,getConverterAdviser()
				,maxBatchSize
				,maxBatchTime
				,infoBuilder .toString()
				,nonPersistentProperties
				,minCompactSize
				);

		String objName = "com.aegisql.conveyor.persistence.derby."+schema+":type=" + partTable;
		DerbyPersistence.LOG.debug("JMX name {}",objName);
		ObjectName objectName = new ObjectName(objName);
		if( ! DerbyPersistenceMBean.mBeanServer.isRegistered(objectName) ) {
			DerbyPersistenceMBean<K> derbyMBean = new DerbyPersistenceMBean<K>() {
				
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
					return encryptionSecret != null;
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
				public String getArchiveStrategyProperties() {
					return archiver.toString();
				}

				@Override
				public Persistence<K> get() {
					return result;
				}
			};
			StandardMBean mbean = new StandardMBean(derbyMBean, DerbyPersistenceMBean.class);
			DerbyPersistenceMBean.mBeanServer.registerMBean(mbean, objectName);
		}
		
		return result;
	}
}