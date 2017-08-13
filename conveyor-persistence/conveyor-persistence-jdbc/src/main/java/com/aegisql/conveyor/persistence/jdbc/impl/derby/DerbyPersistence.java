package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.BlobConverter;
import com.aegisql.conveyor.persistence.jdbc.EnumConverter;

public class DerbyPersistence<K> implements Persistence<K>{
	
	private static enum ArchiveStrategy {
		CUSTOM, //set strategy
		DELETE,
		SET_ARCHIVED,
		MOVE_TO_SCHEMA_TABLE, //schema,table
		MOVE_TO_FILE, //path,file
		LOG, //Logger
		NO_ACTION //external archive strategy
	}

	private final static Logger LOG = LoggerFactory.getLogger(DerbyPersistence.class);
	
	public static class DerbyPersistenceBuilder<K> {
		
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
			idSupplier = idGen::get;
		}
		
		private Class<K> keyClass;
		private BiConsumer<PreparedStatement, K> keyPlacer;
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
		
		
		private ArchiveStrategy archiveStrategy = ArchiveStrategy.DELETE;
		private BiConsumer<Connection,Collection<Long>> archiveIdsStrategy;
		
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

		public DerbyPersistenceBuilder<K> port(int port) {
			this.port = port;
			return this;
		}

		public DerbyPersistenceBuilder<K> idSupplier(LongSupplier idSupplier) {
			this.idSupplier = idSupplier;
			return this;
		}
		
		public DerbyPersistenceBuilder<K> archiveIdsStrategy(BiConsumer<Connection,Collection<Long>> archiveIdsStrategy) {
			this.archiveIdsStrategy = archiveIdsStrategy;
			return this;
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
			boolean partIdTableFound = false;
			boolean keyLogTableFound = false;
			while(tables.next()) {
				String tableSchema = tables.getString("TABLE_SCHEM");
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
					st.execute("CREATE INDEX PART_IDX ON PART(CART_KEY)");
					LOG.debug("Index PART_IDX ON PART(CART_KEY) created");

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
					+" FROM " + partTable + " WHERE ID = ?"
					;
			String getAllPartIdsQuery = "SELECT"
					+" ID"
					+" FROM " + partTable + " WHERE CART_KEY = ?"
					;
			String getAllUnfinishedPartIdsQuery = "SELECT"
					+" CART_KEY"
					+",CART_VALUE"
					+",CART_LABEL"
					+",CREATION_TIME"
					+",EXPIRATION_TIME"
					+",LOAD_TYPE"
					+",CART_PROPERTIES"
					+" FROM " + partTable + " WHERE ARCHIVED = 0"
					;
			String getAllCompletedKeysQuery = "SELECT CART_KEY FROM "+completedLogTable;
			
			Consumer<Collection<Long>> ais = ids->{
				archiveIdsStrategy.accept(conn, ids);
			};

			return new DerbyPersistence<K>(
					conn
					,idSupplier
					,saveCartQuery
					,saveCompletedBuildKeyQuery
					,getPartQuery
					,getAllPartIdsQuery
					,getAllUnfinishedPartIdsQuery
					,getAllCompletedKeysQuery
					,ais
					);
		}

	}
///////////////////////////////////////////////////////////////////////////////
	private final Connection   conn;
	private final LongSupplier idSupplier;
	private final BlobConverter blobConverter;
	private final EnumConverter<LoadType> loadTypeConverter = new EnumConverter<>(LoadType.class);
	
	private final String saveCartQuery;
	private final String saveCompletedBuildKeyQuery;
	private final String getPartQuery;
	private final String getAllPartIdsQuery;
	private final String getAllUnfinishedPartIdsQuery;
	private final String getAllCompletedKeysQuery;
	
	private final Consumer<Collection<Long>> archiveIdsStrategy;
	
	public DerbyPersistence(
			Connection conn
			,LongSupplier idSupplier
			,String saveCartQuery
			,String saveCompletedBuildKeyQuery
			,String getPartQuery
			,String getAllPartIdsQuery
			,String getAllUnfinishedPartIdsQuery
			,String getAllCompletedKeysQuery
			,Consumer<Collection<Long>> archiveIdsStrategy
			) {
		this.conn                         = conn;
		this.idSupplier                   = idSupplier;
		this.blobConverter                = new BlobConverter<>(conn);
		this.saveCartQuery                = saveCartQuery;
		this.saveCompletedBuildKeyQuery   = saveCompletedBuildKeyQuery;
		this.getPartQuery                 = getPartQuery;
		this.getAllPartIdsQuery           = getAllPartIdsQuery;
		this.getAllUnfinishedPartIdsQuery = getAllUnfinishedPartIdsQuery;
		this.getAllCompletedKeysQuery     = getAllCompletedKeysQuery;
		this.archiveIdsStrategy           = archiveIdsStrategy;
	}

	public static <K> DerbyPersistenceBuilder<K> forKeyClass(Class<K> clas) {
		return new DerbyPersistenceBuilder<K>(clas);
	}
	
	@Override
	public long nextUniquePartId() {
		return idSupplier.getAsLong();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		try(PreparedStatement st = conn.prepareStatement(saveCartQuery) ) {
			st.setLong(1, id);
			st.setString(2, loadTypeConverter.toPersistence(cart.getLoadType()));
			st.setObject(3, cart.getKey());
			st.setObject(4, cart.getLabel());
			st.setTimestamp(5, new Timestamp(cart.getCreationTime()));
			st.setTimestamp(6, new Timestamp(cart.getExpirationTime()));
			st.setBlob(7, blobConverter.toPersistence((Serializable) cart.getValue()));
			st.setBlob(8, blobConverter.toPersistence((Serializable) cart.getAllProperties()));
			st.execute();
		} catch (Exception e) {
	    	LOG.error("SavePart Exception: {}",cart,e.getMessage());
	    	throw new RuntimeException("Save Part failed",e);
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
	    	LOG.error("SaveCompletedKey Exception: {}",key,e.getMessage());
	    	throw new RuntimeException("SaveCompletedKey failed",e);
		}
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		Cart<K, ?, L> cart = null;
		LOG.debug("getPart: {}",getPartQuery);
		try(PreparedStatement st = conn.prepareStatement(getPartQuery) ) {
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				K key = (K)rs.getObject(1);
				Object val = blobConverter.fromPersistence(rs.getBlob(2));
				L label = (L)rs.getObject(3);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = (Map<String, Object>) blobConverter.fromPersistence(rs.getBlob(7));
				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType);
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
				L label = (L)rs.getObject(3);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				Map<String,Object> properties = (Map<String, Object>) blobConverter.fromPersistence(rs.getBlob(7));
				LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType);
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
		archiveIdsStrategy.accept(ids);
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveAll() {
		// TODO Auto-generated method stub
		
	}
	
}