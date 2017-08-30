package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.Archiver;
import com.aegisql.conveyor.persistence.jdbc.BlobConverter;
import com.aegisql.conveyor.persistence.jdbc.EncryptingBlobConverter;
import com.aegisql.conveyor.persistence.jdbc.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.StringConverter;

public class DerbyPersistence<K> implements Persistence<K>{
	
	private static enum ArchiveStrategy {
		CUSTOM, //set strategy
		DELETE,
		SET_ARCHIVED,
		MOVE_TO_SCHEMA_TABLE, //schema,table
		MOVE_TO_FILE, //path,file
		NO_ACTION //external archive strategy
	}

	public static class ArchiveBuilder<K> {
		private final DerbyPersistenceBuilder<K> dpb;
		public ArchiveBuilder(DerbyPersistenceBuilder<K> dpb) {
			this.dpb = dpb;
		}
		
		public DerbyPersistenceBuilder<K> doNothing() {
			dpb.archiveStrategy = ArchiveStrategy.NO_ACTION;
			return dpb;
		}
		public DerbyPersistenceBuilder<K> delete() {
			dpb.archiveStrategy = ArchiveStrategy.DELETE;
			return dpb;
		}
		public DerbyPersistenceBuilder<K> markArchived() {
			dpb.archiveStrategy = ArchiveStrategy.SET_ARCHIVED;
			return dpb;
		}
		public DerbyPersistenceBuilder<K> customStrategy(Archiver<K> archiver) {
			dpb.archiveStrategy = ArchiveStrategy.CUSTOM;
			dpb.archiver = archiver;
			return dpb;
		}
		public DerbyPersistenceBuilder<K> moveToTable(String archiveTable) {
			dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_SCHEMA_TABLE;
			dpb.archiveTable = archiveTable;
			return dpb;
		}
		public DerbyPersistenceBuilder<K> moveToFile(String archiveFileTemplate,long maxFileSize) {
			dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_FILE;
			dpb.archiveFileTemplate = archiveFileTemplate;
			dpb.maxFileSize = maxFileSize;
			return dpb;
		}
		
	}
	
	private final static Logger LOG = LoggerFactory.getLogger(DerbyPersistence.class);
	
	public static class DerbyPersistenceBuilder<K> {
		
		public long maxFileSize;
		public String archiveFileTemplate;
		public String archiveTable;
		public Archiver<K> archiver;

		private DerbyPersistenceBuilder(Class<K> clas) {
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
			final AtomicLong idGen = new AtomicLong(System.currentTimeMillis() * 1000000);
			idSupplier = idGen::incrementAndGet;
		}
		
		private Class<K> keyClass;
		
		private Archiver<K> DO_NOTHING_ARCHIVER = new Archiver<K>(){
			@Override
			public void archiveParts(Connection conn, Collection<Long> ids) {}
			@Override
			public void archiveKeys(Connection conn, Collection<K> keys) {}
			@Override
			public void archiveCompleteKeys(Connection conn, Collection<K> keys) {}
			@Override
			public void archiveAll(Connection conn) {}
			@Override
			public void archiveExpiredParts(Connection conn) {}
		};

		//TODO: remove when all implemented
		private Archiver<K> UNIMPLEMENTED_ARCHIVER = new Archiver<K>(){
			@Override
			public void archiveParts(Connection conn, Collection<Long> ids) {throw new RuntimeException("Unimplemented archiver");}
			@Override
			public void archiveKeys(Connection conn, Collection<K> keys) {throw new RuntimeException("Unimplemented archiver");}
			@Override
			public void archiveCompleteKeys(Connection conn, Collection<K> keys) {throw new RuntimeException("Unimplemented archiver");}
			@Override
			public void archiveAll(Connection conn) {throw new RuntimeException("Unimplemented archiver");}
			@Override
			public void archiveExpiredParts(Connection conn) {throw new RuntimeException("Unimplemented archiver");}
		};

		private String keyType = "CART_KEY VARCHAR(255)";
		private final static String PROTOCOL = "jdbc:derby:";
		private final static int PORT = 1527;
		
		private final static String EMBEDDED = "org.apache.derby.jdbc.EmbeddedDriver";
		private final static String CLIENT   = "org.apache.derby.jdbc.ClientDriver";
		
		private final static String EMBEDDED_URL_PATTERN = PROTOCOL+"{schema}";
		private final static String CLIENT_URL_PATTERN   = PROTOCOL+"//{host}:{port}/{schema}";//;user=judy;password=no12see";
		
		private boolean embedded  = true;
		private String driver     = EMBEDDED; //default
		private String urlPattern = EMBEDDED_URL_PATTERN; //default
		private String host       = "localhost"; //default
		private String username   = "";
		private String password   = "";
		private int port          = 0; //default
		private boolean create    = true;
		
		private String schema      = "conveyor_db";
		private String partTable   = "PART";
		private String completedLogTable = "COMPLETED_LOG";
		
		private LongSupplier idSupplier;
		
		private int maxBatchSize  = 100;
		private long maxBatchTime = 60_000;
		
		
		
		private ArchiveStrategy archiveStrategy = ArchiveStrategy.DELETE;
		
		private String encryptionSecret = null;
		private SecretKey secretKey;
		private String encryptionAlgorithm = "AES";
		private String encryptionTransformation = "AES/ECB/PKCS5Padding";
		private Cipher encryptionCipher;
		private int encryptionKeyLength = 16;
		
		private ObjectConverter<?,String> labelConverter = new StringConverter<String>() {
			@Override
			public String fromPersistence(String p) {
				return p;
			}
		};
		
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

		public DerbyPersistenceBuilder<K> username(String username) {
			this.username = username;
			return this;
		}
		
		public DerbyPersistenceBuilder<K> password(String password) {
			this.password = password;
			return this;
		}

		public DerbyPersistenceBuilder<K> schema(String schema) {
			this.schema = schema;
			return this;
		}

		public DerbyPersistenceBuilder<K> partTable(String table) {
			this.partTable = table;
			return this;
		}

		public DerbyPersistenceBuilder<K> completedLogTable(String table) {
			this.completedLogTable = table;
			return this;
		}

		public DerbyPersistenceBuilder<K> port(int port) {
			this.port = port;
			return this;
		}

		public DerbyPersistenceBuilder<K> idSupplier(LongSupplier idSupplier) {
			this.idSupplier = idSupplier;
			return this;
		}
		
		public ArchiveBuilder<K> whenArchiveRecords() {
			return new ArchiveBuilder<>(this);
		}
		
		public DerbyPersistenceBuilder<K> encryptionSecret(String secret) {
			this.encryptionSecret = secret;
			return this;
		}

		public DerbyPersistenceBuilder<K> encryptionSecret(SecretKey secret) {
			this.secretKey = secret;
			return this;
		}

		public DerbyPersistenceBuilder<K> encryptionAlgorithm(String algorithm) {
			this.encryptionAlgorithm = algorithm;
			return this;
		}

		public DerbyPersistenceBuilder<K> encryptionTransformation(String algorithm) {
			this.encryptionTransformation = algorithm;
			return this;
		}

		public DerbyPersistenceBuilder<K> encryptionKeyLength(int keyLenght) {
			this.encryptionKeyLength = keyLenght;
			return this;			
		}

		public DerbyPersistenceBuilder<K> encryptionCipher(Cipher cipher) {
			this.encryptionCipher = cipher;
			return this;
		}

		public <L extends Enum<L>> DerbyPersistenceBuilder<K> labelConverter(Class<L> enClass) {
			this.labelConverter = new EnumConverter<>(enClass);
			return this;
		}
		
		public <L> DerbyPersistenceBuilder<K> labelConverter(ObjectConverter<L,String> labelConverter) {
			this.labelConverter = labelConverter;
			return this;
		}

		public <L> DerbyPersistenceBuilder<K> maxBatchSize(int maxBatchSize) {
			this.maxBatchSize = maxBatchSize;
			return this;
		}

		public <L> DerbyPersistenceBuilder<K> maxBatchTime(long maxBatchTime, TimeUnit unit) {
			this.maxBatchTime = TimeUnit.MILLISECONDS.convert(maxBatchTime, unit);
			return this;
		}

		public <L> DerbyPersistenceBuilder<K> maxBatchTime(Duration duration) {
			this.maxBatchTime = duration.toMillis();
			return this;
		}
		
		private Archiver<K> makeArchivedArchiver(String partTable, String completedTable) {
			
			final String deleteFromPartsById      = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE ID IN(?)";
			final String deleteFromPartsByCartKey = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE CART_KEY IN(?)";
			final String deleteFromCompleted      = "DELETE FROM "+completedTable + " WHERE CART_KEY IN(?)";
			
			final String deleteExpiredParts       = "UPDATE "+partTable+" SET ARCHIVED = 1 WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
			final String deleteAllParts           = "UPDATE "+partTable+" SET ARCHIVED = 1";
			final String deleteAllCompleted       = "DELETE FROM "+completedTable;
			
			final String q;
			if(Number.class.isAssignableFrom(keyClass)) {
				q = "";
			} else {
				q = "'";
			}
			
			return new Archiver<K>(){

				@Override
				public void archiveParts(Connection conn, Collection<Long> ids) {
					if(ids.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						ids.forEach(id->sb.append(id).append(","));
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromPartsById.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveParts failure",e);
						throw new RuntimeException("archiveParts failure",e);
					}
				}

				@Override
				public void archiveKeys(Connection conn, Collection<K> keys) {
					if(keys.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						keys.forEach(id->{
							sb.append(q).append(id).append(q).append(",");
							});
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromPartsByCartKey.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveKeys failure",e);
						throw new RuntimeException("archiveKeys failure",e);
					}
				}

				@Override
				public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
					if(keys.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						keys.forEach(id->{
							sb.append(q).append(id).append(q).append(",");
							});
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromCompleted.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveCompleteKeys failure",e);
						throw new RuntimeException("archiveCompleteKeys failure",e);
					}
				}

				@Override
				public void archiveAll(Connection conn) {
					try(PreparedStatement ps = conn.prepareStatement(deleteAllParts)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveAll parts failure",e);
						throw new RuntimeException("archiveAll parts failure",e);
					}
					try(PreparedStatement ps = conn.prepareStatement(deleteAllCompleted)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveAll completed failure",e);
						throw new RuntimeException("archiveAll completed failure",e);
					}
				}

				@Override
				public void archiveExpiredParts(Connection conn) {
					try(PreparedStatement ps = conn.prepareStatement(deleteExpiredParts)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveExpiredParts failure",e);
						throw new RuntimeException("archiveExpiredParts failure",e);
					}
				}};
		}

		private Archiver<K> makeDeleteArchiver(String partTable, String completedTable) {
			
			final String deleteFromPartsById      = "DELETE FROM "+partTable + " WHERE ID IN(?)";
			final String deleteFromPartsByCartKey = "DELETE FROM "+partTable + " WHERE CART_KEY IN(?)";
			final String deleteFromCompleted      = "DELETE FROM "+completedTable + " WHERE CART_KEY IN(?)";
			final String deleteExpiredParts       = "DELETE FROM "+partTable + " WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";

			final String deleteAllParts           = "DELETE FROM "+partTable;
			final String deleteAllCompleted       = "DELETE FROM "+completedTable;
			
			final String q;
			if(Number.class.isAssignableFrom(keyClass)) {
				q = "";
			} else {
				q = "'";
			}
			
			return new Archiver<K>(){

				@Override
				public void archiveParts(Connection conn, Collection<Long> ids) {
					if(ids.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						ids.forEach(id->sb.append(id).append(","));
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromPartsById.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveParts failure",e);
						throw new RuntimeException("archiveParts failure",e);
					}
				}

				@Override
				public void archiveKeys(Connection conn, Collection<K> keys) {
					if(keys.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						keys.forEach(id->{
							sb.append(q).append(id).append(q).append(",");
							});
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromPartsByCartKey.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveKeys failure",e);
						throw new RuntimeException("archiveKeys failure",e);
					}
				}

				@Override
				public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
					if(keys.isEmpty()) {
						return;
					}
					try(Statement ps = conn.createStatement()) {
						StringBuilder sb = new StringBuilder();
						keys.forEach(id->{
							sb.append(q).append(id).append(q).append(",");
							});
						if(sb.lastIndexOf(",") > 0) {
							sb.deleteCharAt(sb.lastIndexOf(","));
						}
						ps.execute(deleteFromCompleted.replace("?", sb.toString()));
					} catch (SQLException e) {
						LOG.error("archiveCompleteKeys failure",e);
						throw new RuntimeException("archiveCompleteKeys failure",e);
					}
				}

				@Override
				public void archiveAll(Connection conn) {
					try(PreparedStatement ps = conn.prepareStatement(deleteAllParts)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveAll parts failure",e);
						throw new RuntimeException("archiveAll parts failure",e);
					}
					try(PreparedStatement ps = conn.prepareStatement(deleteAllCompleted)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveAll completed failure",e);
						throw new RuntimeException("archiveAll completed failure",e);
					}
				}

				@Override
				public void archiveExpiredParts(Connection conn) {
					try(PreparedStatement ps = conn.prepareStatement(deleteExpiredParts)) {
						ps.execute();
					} catch (SQLException e) {
						LOG.error("archiveExpiredParts failure",e);
						throw new RuntimeException("archiveExpiredParts failure",e);
					}
				}};
		}

		public DerbyPersistence<K> build() throws Exception {
			LOG.debug("DERBY PERSISTENCE");

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
			
			LOG.debug("Driver: {}",driver);
			LOG.debug("Connection Url: {}",url);
			LOG.debug("Schema: {}",schema);
			
			Connection conn = DriverManager.getConnection(url, properties);
			DatabaseMetaData meta = conn.getMetaData();
			LOG.debug("Connected!");
			
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
								+",CART_VALUE BLOB"
								+",CART_PROPERTIES BLOB"
								+",ARCHIVED SMALLINT NOT NULL DEFAULT 0"
								+")";
					LOG.debug("Table '{}' not found. Trying to create...\n{}",partTable,sql);
					st.execute(sql);
					LOG.debug("Table '{}' created",partTable);
					st.execute("CREATE INDEX "+partTable+"_IDX ON "+partTable+"(CART_KEY)");
					LOG.debug("Index "+partTable+"_IDX ON "+partTable+"(CART_KEY) created");

				} 
			} else {
				LOG.debug("Table '{}' already exists",partTable);
			}

			if( ! keyLogTableFound ) {
				try(Statement st = conn.createStatement() ) {
					String sql = "CREATE TABLE "
								+completedLogTable+" ("
								+keyType+" PRIMARY KEY"
								+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
								+")";
					LOG.debug("Table '{}' not found. Trying to create...\n{}",completedLogTable,sql);
					st.execute(sql);
					LOG.debug("Table '{}' created",completedLogTable);
				} 
			} else {
				LOG.debug("Table '{}' already exists",completedLogTable);
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
					+") VALUES (?,?,?,?,?,?,?,?)"
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
					+" FROM " + partTable + " WHERE ID = ? AND ARCHIVED = 0"
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
					+" FROM " + partTable + " WHERE ARCHIVED = 0 AND LOAD_TYPE = 'STATIC_PART' ORDER BY ID ASC";
			
			switch(archiveStrategy) {
				case CUSTOM:
					//defined by user
					break;
				case DELETE:
					archiver = makeDeleteArchiver(partTable,completedLogTable);
					break;
				case SET_ARCHIVED:
					archiver = makeArchivedArchiver(partTable,completedLogTable);
					break;
				case MOVE_TO_SCHEMA_TABLE:
					archiver = UNIMPLEMENTED_ARCHIVER;
					break;
				case MOVE_TO_FILE: 
					archiver = UNIMPLEMENTED_ARCHIVER;
					break;
				case NO_ACTION:
					archiver = DO_NOTHING_ARCHIVER;
					break;
				default:
					
			}
			
			@SuppressWarnings("unused")
			BlobConverter<?> blobConverter;
			if(encryptionSecret != null) {
				LOG.debug("VALUES ENCRIPTION ON");
				if(secretKey == null) {
					byte[] key = encryptionSecret.getBytes("UTF-8");
					MessageDigest sha = MessageDigest.getInstance("SHA-1");
					key = sha.digest(key);
					key = Arrays.copyOf(key, encryptionKeyLength);
					secretKey = new SecretKeySpec(key, encryptionAlgorithm);

					encryptionCipher = Cipher.getInstance(encryptionTransformation);
				}
				blobConverter = new EncryptingBlobConverter<>(conn,secretKey,encryptionCipher);
			} else {
				LOG.debug("VALUES ENCRIPTION OFF");
				blobConverter = new BlobConverter<>(conn);
			}
			
			return new DerbyPersistence<K>(
					this
					,conn
					,idSupplier
					,saveCartQuery
					,saveCompletedBuildKeyQuery
					,getPartQuery
					,getAllPartIdsQuery
					,getAllUnfinishedPartIdsQuery
					,getAllCompletedKeysQuery
					,getAllStaticPartsQuery
					,"SELECT COUNT(*) FROM " + partTable + " WHERE ARCHIVED = 0"
					,archiver
					,labelConverter
					,blobConverter
					,maxBatchSize
					,maxBatchTime
					);
		}
	}
///////////////////////////////////////////////////////////////////////////////
	private final Connection   conn;
	private final LongSupplier idSupplier;
	private final BlobConverter blobConverter;
	private final EnumConverter<LoadType> loadTypeConverter = new EnumConverter<>(LoadType.class);
	private final ObjectConverter labelConverter;
	
	private final String saveCartQuery;
	private final String saveCompletedBuildKeyQuery;
	private final String getPartQuery;
	private final String getAllPartIdsQuery;
	private final String getAllUnfinishedPartIdsQuery;
	private final String getAllCompletedKeysQuery;
	
	private final Archiver<K> archiver;
	private final String getAllStaticPartsQuery;
	
	private final DerbyPersistenceBuilder<K> builder;
	private final int maxBatchSize;
	private final long maxBatchTime;
	private final String getNumberOfPartsQuery;
	
	private DerbyPersistence(
			DerbyPersistenceBuilder<K> builder
			,Connection conn
			,LongSupplier idSupplier
			,String saveCartQuery
			,String saveCompletedBuildKeyQuery
			,String getPartQuery
			,String getAllPartIdsQuery
			,String getAllUnfinishedPartIdsQuery
			,String getAllCompletedKeysQuery
			,String getAllStaticPartsQuery
			,String getNumberOfPartsQuery
			,Archiver<K> archiver
			,ObjectConverter<?,String> labelConverter
			,BlobConverter blobConverter
			,int maxBatchSize
			,long maxBatchTime
			) {
		this.builder                      = builder;
		this.conn                         = conn;
		this.idSupplier                   = idSupplier;
		this.blobConverter                = blobConverter;
		this.saveCartQuery                = saveCartQuery;
		this.saveCompletedBuildKeyQuery   = saveCompletedBuildKeyQuery;
		this.getPartQuery                 = getPartQuery;
		this.getAllPartIdsQuery           = getAllPartIdsQuery;
		this.getAllStaticPartsQuery       = getAllStaticPartsQuery;
		this.getAllUnfinishedPartIdsQuery = getAllUnfinishedPartIdsQuery;
		this.getAllCompletedKeysQuery     = getAllCompletedKeysQuery;
		this.getNumberOfPartsQuery        = getNumberOfPartsQuery;
		this.archiver                     = archiver;
		this.labelConverter               = labelConverter;
		this.maxBatchSize                 = maxBatchSize;
		this.maxBatchTime                 = maxBatchTime;
	}

	public static <K> DerbyPersistenceBuilder<K> forKeyClass(Class<K> clas) {
		return new DerbyPersistenceBuilder<K>(clas);
	}

	public DerbyPersistenceBuilder<K> getBuilder() {
		return builder;
	}
	
	@Override
	public Persistence<K> copy() {
		try {
			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException("Failed copying persistence object",e);
		}
	}
	
	@Override
	public long nextUniquePartId() {
		return idSupplier.getAsLong();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		LOG.debug("SAVING: {}",cart);
		try(PreparedStatement st = conn.prepareStatement(saveCartQuery) ) {
			st.setLong(1, id);
			st.setString(2, loadTypeConverter.toPersistence(cart.getLoadType()));
			st.setObject(3, cart.getKey());
			L label = cart.getLabel();
			st.setObject(4, labelConverter.toPersistence(label));
			st.setTimestamp(5, new Timestamp(cart.getCreationTime()));
			st.setTimestamp(6, new Timestamp(cart.getExpirationTime()));
			st.setBlob(7, blobConverter.toPersistence((Serializable) cart.getValue()));
			st.setBlob(8, blobConverter.toPersistence((Serializable) cart.getAllProperties()));
			st.execute();
		} catch (Exception e) {
			e.printStackTrace();
	    	LOG.error("SavePart Exception: {} {}",cart,e.getMessage());
	    	throw new RuntimeException("Save Part failed for "+cart,e);
		}
	}

	@Override
	public void savePartId(K key, long partId) {
		// DO NOTHING. SUPPORTED BY SECONDARY INDEX ON THE PART TABLE
	}

	@Override
	public void saveCompletedBuildKey(K key) {
		try(PreparedStatement st = conn.prepareStatement(saveCompletedBuildKeyQuery) ) {
			st.setObject(1, key);
			st.execute();
		} catch (Exception e) {
	    	LOG.error("SaveCompletedKey {} Exception: {}",key,e.getMessage());
	    	e.printStackTrace();
	    	throw new RuntimeException("SaveCompletedKey failed",e);
		}
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		Cart<K, ?, L> cart = null;
		LOG.debug("getPart: {} {}",id,getPartQuery);
		try(PreparedStatement st = conn.prepareStatement(getPartQuery) ) {
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				K key = (K)rs.getObject(1);
				Object val = blobConverter.fromPersistence(rs.getBlob(2));
				String labelString = rs.getString(3);
				L label = null;
				if(labelString != null) {
					label = (L)labelConverter.fromPersistence(labelString.trim());
				}
				LOG.debug("getPart LABEL: {}",label);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = (Map<String, Object>) blobConverter.fromPersistence(rs.getBlob(7));
//				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				
				if(loadType == LoadType.BUILDER) {
					cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime);
				} else if(loadType == LoadType.RESULT_CONSUMER) {
					cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime);
				} else {
					cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType);
				}
			}
		} catch (Exception e) {
	    	LOG.error("getPart Exception: {}",id,e.getMessage());
	    	throw new RuntimeException("getPart failed",e);
		}
		return cart;
	}

	@Override
	public Collection<Long> getAllPartIds(K key) {
		Set<Long> res = new LinkedHashSet<>();
		try(PreparedStatement st = conn.prepareStatement(getAllPartIdsQuery) ) {
			st.setObject(1, key);
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				Long id = rs.getLong(1);
				res.add(id);
			}
		} catch (Exception e) {
	    	LOG.error("getAllPartIds Exception: {}",key,e.getMessage());
	    	throw new RuntimeException("getAllPartIds failed",e);
		}
		return res;
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		LOG.debug("getAllParts: {}",getAllUnfinishedPartIdsQuery);
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = conn.prepareStatement(getAllUnfinishedPartIdsQuery) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				Object val = blobConverter.fromPersistence(rs.getBlob(2));
				String labelString = rs.getString(3);
				L label = null;
				if(labelString != null) {
					label = (L)labelConverter.fromPersistence(labelString.trim());
				}
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = (Map<String, Object>) blobConverter.fromPersistence(rs.getBlob(7));
//				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				if(loadType == LoadType.BUILDER) {
					cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime);
				} else if(loadType == LoadType.RESULT_CONSUMER) {
					cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime);
				} else {
					cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType);
				}
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getAllUnfinishedPartIdsQuery exception: ",e.getMessage());
	    	throw new RuntimeException("getAllUnfinishedPartIdsQuery failed",e);
		}
		return carts;
	}

	@Override
	public Set<K> getCompletedKeys() {
		Set<K> res = new LinkedHashSet<>();
		try(PreparedStatement st = conn.prepareStatement(getAllCompletedKeysQuery) ) {
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				@SuppressWarnings("unchecked")
				K key = (K) rs.getObject(1);
				res.add(key);
			}
		} catch (Exception e) {
	    	LOG.error("getCompletedKeys Exception:",e.getMessage());
	    	throw new RuntimeException("getCompletedKeys failed",e);
		}
		return res;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		archiver.archiveParts(conn,ids);
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		archiver.archiveKeys(conn, keys);
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		archiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveAll() {
		archiver.archiveAll(conn);
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
		LOG.debug("getAllStaticParts: {}",getAllStaticPartsQuery);
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = conn.prepareStatement(getAllStaticPartsQuery) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				Object val = blobConverter.fromPersistence(rs.getBlob(2));
				L label = (L)labelConverter.fromPersistence(rs.getString(3).trim());
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = (Map<String, Object>) blobConverter.fromPersistence(rs.getBlob(7));
//				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType);
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getAllStaticParts exception: ",e.getMessage());
	    	throw new RuntimeException("getAllStaticParts failed",e);
		}
		return carts;
	}

	@Override
	public void close() throws IOException {
		try {
			conn.close();
		} catch (SQLException e) {
			throw new IOException("SQL Connection close error",e);
		}
	}

	@Override
	public void archiveExpiredParts() {
		archiver.archiveExpiredParts(conn);
	}
	
	@Override
	public int getMaxArchiveBatchSize() {
		return maxBatchSize;
	}

	@Override
	public long getMaxArchiveBatchTime() {
		return maxBatchTime;
	}

	@Override
	public long getNumberOfParts() {
		long res = -1;
		try(PreparedStatement st = conn.prepareStatement(getNumberOfPartsQuery) ) {
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				res = rs.getLong(1);
			}
		} catch (Exception e) {
	    	LOG.error("getNumberOfParts Exception:",e.getMessage());
	    	throw new RuntimeException("getNumberOfParts failed",e);
		}
		return res;
	}
	
}