package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.Blob;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.Load;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.Archiver;
import com.aegisql.conveyor.persistence.jdbc.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.MapToClobConverter;
import com.aegisql.conveyor.persistence.jdbc.StringConverter;
import com.aegisql.id_builder.IdSource;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;

// TODO: Auto-generated Javadoc
/**
 * The Class DerbyPersistence.
 *
 * @param <K> the key type
 */
public class DerbyPersistence<K> implements Persistence<K>{
	
	/**
	 * The Enum ArchiveStrategy.
	 */
	private static enum ArchiveStrategy {
		
		/** The custom. */
		CUSTOM, 
 /** The delete. */
 //set strategy
		DELETE,
		
		/** The set archived. */
		SET_ARCHIVED,
		
		/** The move to schema table. */
		MOVE_TO_SCHEMA_TABLE, 
 /** The move to file. */
 //schema,table
		MOVE_TO_FILE, 
 /** The no action. */
 //path,file
		NO_ACTION //external archive strategy
	}

	/**
	 * The Class ArchiveBuilder.
	 *
	 * @param <K> the key type
	 */
	public static class ArchiveBuilder<K> {
		
		/** The dpb. */
		private final DerbyPersistenceBuilder<K> dpb;
		
		/**
		 * Instantiates a new archive builder.
		 *
		 * @param dpb the dpb
		 */
		public ArchiveBuilder(DerbyPersistenceBuilder<K> dpb) {
			this.dpb = dpb;
		}
		
		/**
		 * Do nothing.
		 *
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> doNothing() {
			dpb.archiveStrategy = ArchiveStrategy.NO_ACTION;
			return dpb;
		}
		
		/**
		 * Delete.
		 *
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> delete() {
			dpb.archiveStrategy = ArchiveStrategy.DELETE;
			return dpb;
		}
		
		/**
		 * Mark archived.
		 *
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> markArchived() {
			dpb.archiveStrategy = ArchiveStrategy.SET_ARCHIVED;
			return dpb;
		}
		
		/**
		 * Custom strategy.
		 *
		 * @param archiver the archiver
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> customStrategy(Archiver<K> archiver) {
			dpb.archiveStrategy = ArchiveStrategy.CUSTOM;
			dpb.archiver = archiver;
			return dpb;
		}
		
		/**
		 * Move to table.
		 *
		 * @param archiveTable the archive table
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> moveToTable(String archiveTable) {
			dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_SCHEMA_TABLE;
			dpb.archiveTable = archiveTable;
			return dpb;
		}
		
		/**
		 * Move to file.
		 *
		 * @param archiveFileTemplate the archive file template
		 * @param maxFileSize the max file size
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> moveToFile(String archiveFileTemplate,long maxFileSize) {
			dpb.archiveStrategy = ArchiveStrategy.MOVE_TO_FILE;
			dpb.archiveFileTemplate = archiveFileTemplate;
			dpb.maxFileSize = maxFileSize;
			return dpb;
		}
		
	}
	
	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(DerbyPersistence.class);
	
	/**
	 * The Class DerbyPersistenceBuilder.
	 *
	 * @param <K> the key type
	 */
	public static class DerbyPersistenceBuilder<K> {
		
		/** The max file size. */
		public long maxFileSize;
		
		/** The archive file template. */
		public String archiveFileTemplate;
		
		/** The archive table. */
		public String archiveTable;
		
		/** The archiver. */
		public Archiver<K> archiver;

		/**
		 * Instantiates a new derby persistence builder.
		 *
		 * @param clas the clas
		 */
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
			
			if(idSupplier == null) {
				final IdSource idGen = TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000);
				idSupplier = idGen::getId;
			}
		}
		
		/** The key class. */
		private Class<K> keyClass;
		
		/** The do nothing archiver. */
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

		/** The unimplemented archiver. */
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
		
		/** The max batch size. */
		private int maxBatchSize  = 100;
		
		/** The max batch time. */
		private long maxBatchTime = 60_000;
		
		private ConverterAdviser converterAdviser;
		
		/** The archive strategy. */
		private ArchiveStrategy archiveStrategy = ArchiveStrategy.DELETE;
		
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
		 * Encryption cipher.
		 *
		 * @param cipher the cipher
		 * @return the derby persistence builder
		 */
		public DerbyPersistenceBuilder<K> encryptionCipher(Cipher cipher) {
			this.encryptionCipher = cipher;
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
		

		private Archiver<K> makeToFileArchiver(String partTable, String completedTable, Archiver<K> deleteArchiver) {
			return new Archiver<K>() {

				@Override
				public void archiveParts(Connection conn, Collection<Long> ids) {
					deleteArchiver.archiveParts(conn, ids);
				}

				@Override
				public void archiveKeys(Connection conn, Collection<K> keys) {
					deleteArchiver.archiveKeys(conn, keys);
				}

				@Override
				public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
					deleteArchiver.archiveCompleteKeys(conn, keys);
				}

				@Override
				public void archiveExpiredParts(Connection conn) {
					deleteArchiver.archiveExpiredParts(conn);
				}

				@Override
				public void archiveAll(Connection conn) {
					deleteArchiver.archiveAll(conn);
				}
			};
		}

		/**
		 * Make archived archiver.
		 *
		 * @param partTable the part table
		 * @param completedTable the completed table
		 * @return the archiver
		 */
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

		/**
		 * Make delete archiver.
		 *
		 * @param partTable the part table
		 * @param completedTable the completed table
		 * @return the archiver
		 */
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

		/**
		 * Builds the.
		 *
		 * @return the derby persistence
		 * @throws Exception the exception
		 */
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
								+",VALUE_TYPE VARCHAR(255)"
								+",CART_PROPERTIES CLOB"
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
					+",VALUE_TYPE"					
					+") VALUES (?,?,?,?,?,?,?,?,?)"
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
					+",VALUE_TYPE"
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

			if(encryptionSecret != null) {
				LOG.debug("VALUES ENCRIPTION ON");
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
				LOG.debug("VALUES ENCRIPTION OFF");
			}
			
			infoBuilder.append("[schema=").append(schema).append(" ");
			infoBuilder.append("partsTable=").append(partTable).append(" ");
			infoBuilder.append("completedTable=").append(completedLogTable).append(" ");
			infoBuilder.append("archiveStrategy=").append(archiveStrategy).append(" ");
			infoBuilder.append("encryption=").append(encryptionSecret != null?"ON":"OFF").append("]");
			
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
					//,blobConverter
					,getConverterAdviser()
					,maxBatchSize
					,maxBatchTime
					,infoBuilder .toString()
					,nonPersistentProperties
					);
		}
	}

/** The conn. */
///////////////////////////////////////////////////////////////////////////////
	private final Connection   conn;
	
	/** The id supplier. */
	private final LongSupplier idSupplier;
	
	/** The blob converter. */
//	private final BlobConverter blobConverter;
	
	private final ConverterAdviser converterAdviser;
	
	private final MapToClobConverter mapConverter;
	
	/** The load type converter. */
	private final EnumConverter<LoadType> loadTypeConverter = new EnumConverter<>(LoadType.class);
	
	/** The label converter. */
	private final ObjectConverter labelConverter;
	
	/** The save cart query. */
	private final String saveCartQuery;
	
	/** The save completed build key query. */
	private final String saveCompletedBuildKeyQuery;
	
	/** The get part query. */
	private final String getPartQuery;
	
	/** The get all part ids query. */
	private final String getAllPartIdsQuery;
	
	/** The get all unfinished part ids query. */
	private final String getAllUnfinishedPartIdsQuery;
	
	/** The get all completed keys query. */
	private final String getAllCompletedKeysQuery;
	
	/** The archiver. */
	private final Archiver<K> archiver;
	
	/** The get all static parts query. */
	private final String getAllStaticPartsQuery;
	
	/** The builder. */
	private final DerbyPersistenceBuilder<K> builder;
	
	/** The max batch size. */
	private final int maxBatchSize;
	
	/** The max batch time. */
	private final long maxBatchTime;
	
	/** The get number of parts query. */
	private final String getNumberOfPartsQuery;

	private final String info;
	
	private final Set<String> nonPersistentProperties;
	
	/**
	 * Instantiates a new derby persistence.
	 *
	 * @param builder the builder
	 * @param conn the conn
	 * @param idSupplier the id supplier
	 * @param saveCartQuery the save cart query
	 * @param saveCompletedBuildKeyQuery the save completed build key query
	 * @param getPartQuery the get part query
	 * @param getAllPartIdsQuery the get all part ids query
	 * @param getAllUnfinishedPartIdsQuery the get all unfinished part ids query
	 * @param getAllCompletedKeysQuery the get all completed keys query
	 * @param getAllStaticPartsQuery the get all static parts query
	 * @param getNumberOfPartsQuery the get number of parts query
	 * @param archiver the archiver
	 * @param labelConverter the label converter
	 * @param blobConverter the blob converter
	 * @param maxBatchSize the max batch size
	 * @param maxBatchTime the max batch time
	 */
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
			//,BlobConverter blobConverter
			,ConverterAdviser<?> converterAdviser
			,int maxBatchSize
			,long maxBatchTime
			,String info
			,Set<String> nonPersistentProperties
			) {
		this.builder                      = builder;
		this.conn                         = conn;
		this.idSupplier                   = idSupplier;
		//this.blobConverter                = blobConverter;
		this.converterAdviser             = converterAdviser;
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
		this.mapConverter                 = new MapToClobConverter(conn);
		this.info                         = info;
		this.nonPersistentProperties      = nonPersistentProperties;
	}

	/**
	 * For key class.
	 *
	 * @param <K> the key type
	 * @param clas the clas
	 * @return the derby persistence builder
	 */
	public static <K> DerbyPersistenceBuilder<K> forKeyClass(Class<K> clas) {
		return new DerbyPersistenceBuilder<K>(clas);
	}

	/**
	 * Gets the builder.
	 *
	 * @return the builder
	 */
	public DerbyPersistenceBuilder<K> getBuilder() {
		return builder;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#copy()
	 */
	@Override
	public Persistence<K> copy() {
		try {
			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException("Failed copying persistence object",e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#nextUniquePartId()
	 */
	@Override
	public long nextUniquePartId() {
		return idSupplier.getAsLong();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#savePart(long, com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		LOG.debug("SAVING: {}",cart);
		try(PreparedStatement st = conn.prepareStatement(saveCartQuery) ) {
			Object value = cart.getValue();
			L label      = cart.getLabel();
			ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, value == null?null:value.getClass().getCanonicalName());
			String hint = byteConverter.conversionHint();
			st.setLong(1, id);
			st.setString(2, loadTypeConverter.toPersistence(cart.getLoadType()));
			st.setObject(3, cart.getKey());
			st.setObject(4, labelConverter.toPersistence(label));
			st.setTimestamp(5, new Timestamp(cart.getCreationTime()));
			st.setTimestamp(6, new Timestamp(cart.getExpirationTime()));
			st.setBlob(7, toBlob( byteConverter.toPersistence(value)));
			
			Map<String,Object> properties = new HashMap<>();
			cart.getAllProperties().forEach((k,v)->{
				if(isPersistentProperty(k)) {
					properties.put(k, v);
				}
			});
			
			st.setClob(8, mapConverter.toPersistence(properties));
			st.setString(9, hint);
			st.execute();
		} catch (Exception e) {
			e.printStackTrace();
	    	LOG.error("SavePart Exception: {} {}",cart,e.getMessage());
	    	throw new RuntimeException("Save Part failed for "+cart,e);
		}
	}
	
	private Blob toBlob(byte[] bytes) {
    	Blob blob       = null;
    	OutputStream os = null;
		try {
			blob = conn.createBlob();
	    	os = blob.setBinaryStream(1);
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		}
		try {
			os.write( bytes );
			return blob;
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

	private byte[] fromBlob(Blob blob) {
		try(InputStream in = blob.getBinaryStream(1, blob.length())) {
			return IOUtils.toByteArray(in);
		} catch (SQLException e) {
			throw new RuntimeException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new RuntimeException("IO Runntime Exception",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#savePartId(java.lang.Object, long)
	 */
	@Override
	public void savePartId(K key, long partId) {
		// DO NOTHING. SUPPORTED BY SECONDARY INDEX ON THE PART TABLE
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#saveCompletedBuildKey(java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getPart(long)
	 */
	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		Cart<K, ?, L> cart = null;
		LOG.debug("getPart: {} {}",id,getPartQuery);
		try(PreparedStatement st = conn.prepareStatement(getPartQuery) ) {
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				K key = (K)rs.getObject(1);
				String labelString = rs.getString(3);
				L label = null;
				if(labelString != null) {
					label = (L)labelConverter.fromPersistence(labelString.trim());
				}
				String hint = rs.getString(8);
				ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
				Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));

				LOG.debug("getPart LABEL: {}",label);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
//				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				
				if(loadType == LoadType.BUILDER) {
					cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime);
				} else if(loadType == LoadType.RESULT_CONSUMER) {
					cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime);
				} else if(loadType == LoadType.MULTI_KEY_PART) {
					Load load = (Load)val;
					cart = new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(),properties);
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllPartIds(java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		LOG.debug("getAllParts: {}",getAllUnfinishedPartIdsQuery);
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = conn.prepareStatement(getAllUnfinishedPartIdsQuery) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				String labelString = rs.getString(3);
				L label = null;
				if(labelString != null) {
					label = (L)labelConverter.fromPersistence(labelString.trim());
				}
				
				String hint = rs.getString(8);
				ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
				Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));

				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
//				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				if(loadType == LoadType.BUILDER) {
					cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime);
				} else if(loadType == LoadType.RESULT_CONSUMER) {
					cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime);
				} else if(loadType == LoadType.MULTI_KEY_PART) {
					Load load = (Load)val;
					cart = new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(),properties);
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getCompletedKeys()
	 */
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		archiver.archiveParts(conn,ids);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		archiver.archiveKeys(conn, keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		archiver.archiveCompleteKeys(conn, keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveAll()
	 */
	@Override
	public void archiveAll() {
		archiver.archiveAll(conn);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllStaticParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
		LOG.debug("getAllStaticParts: {}",getAllStaticPartsQuery);
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = conn.prepareStatement(getAllStaticPartsQuery) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				L label = (L)labelConverter.fromPersistence(rs.getString(3).trim());
				String hint = rs.getString(8);
				ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
				Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
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

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			conn.close();
		} catch (SQLException e) {
			throw new IOException("SQL Connection close error",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		archiver.archiveExpiredParts(conn);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getMaxArchiveBatchSize()
	 */
	@Override
	public int getMaxArchiveBatchSize() {
		return maxBatchSize;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getMaxArchiveBatchTime()
	 */
	@Override
	public long getMaxArchiveBatchTime() {
		return maxBatchTime;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getNumberOfParts()
	 */
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

	@Override
	public String toString() {
		return info;
	}

	@Override
	public boolean isPersistentProperty(String property) {
		return ! nonPersistentProperties.contains(property);
	}
	
	
}