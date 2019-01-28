package com.aegisql.conveyor.persistence.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.Load;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.ConnectionSupplier;
import com.aegisql.conveyor.persistence.jdbc.builders.DynamicPersistenceSql;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.converters.MapToClobConverter;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistenceBuilder;
// TODO: Auto-generated Javadoc
/**
 * The Class JdbcPersistence.
 *
 * @param <K> the key type
 */
public class JdbcPersistence<K> implements Persistence<K>{
	
	/** The Constant LOG. */
	final static Logger LOG = LoggerFactory.getLogger(JdbcPersistence.class);
	
	/** The connectionSupplier. */
///////////////////////////////////////////////////////////////////////////////
	private final ConnectionSupplier connectionSupplier;
	
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
	
	private final DynamicPersistenceSql dynamicPersistenceSql;
	
		
	/** The archiver. */
	private final Archiver<K> archiver;
	
	/** The max batch size. */
	private final int maxBatchSize;
	
	/** The max batch time. */
	private final long maxBatchTime;
	
	private final String info;
	
	private final Set<String> nonPersistentProperties;

	private int minCompactSize = 0;
		
	/**
	 * Instantiates a new derby persistence.
	 *
	 * @param builder the builder
	 * @param connectionSupplier the connectionSupplier
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
	public JdbcPersistence(
			 ConnectionSupplier connectionSupplier
			,LongSupplier idSupplier
			,DynamicPersistenceSql dynamicPersistenceSql
			,Archiver<K> archiver
			,ObjectConverter<?,String> labelConverter
			,ConverterAdviser<?> converterAdviser
			,int maxBatchSize
			,long maxBatchTime
			,String info
			,Set<String> nonPersistentProperties
			,int minCompactSize
			) {
		this.connectionSupplier           = connectionSupplier;
		this.idSupplier                   = idSupplier;
		this.converterAdviser             = converterAdviser;
		this.dynamicPersistenceSql        = dynamicPersistenceSql;
		this.archiver                     = archiver;
		this.labelConverter               = labelConverter;
		this.maxBatchSize                 = maxBatchSize;
		this.maxBatchTime                 = maxBatchTime;
		this.mapConverter                 = new MapToClobConverter(connectionSupplier.get());
		this.info                         = info;
		this.nonPersistentProperties      = nonPersistentProperties;
		this.minCompactSize               = minCompactSize;
		this.archiver.setPersistence(this);		
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#copy()
	 */
	@Override
	public Persistence<K> copy() {
		return new JdbcPersistence<>(
				  connectionSupplier.clone()
				, idSupplier
				, dynamicPersistenceSql
				, archiver
				, labelConverter
				, converterAdviser
				, maxBatchSize
				, maxBatchTime
				, info
				, nonPersistentProperties
				, minCompactSize
				);
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
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.saveCartQuery()) ) {
			Object value = cart.getValue();
			st.setLong(1, id);
			st.setString(2, loadTypeConverter.toPersistence(cart.getLoadType()));
			st.setObject(3, cart.getKey());
			st.setTimestamp(5, new Timestamp(cart.getCreationTime()));
			st.setTimestamp(6, new Timestamp(cart.getExpirationTime()));
			String hint;
			ObjectConverter<Object, byte[]> byteConverter;
			if(cart instanceof GeneralCommand) {
				CommandLabel command = CommandLabel.RESTORE_BUILD;
				byteConverter = converterAdviser.getConverter(command, value == null?null:value.getClass().getCanonicalName());
				hint = byteConverter.conversionHint();
				st.setObject(4, labelConverter.toPersistence(command));
			} else {
				L label      = cart.getLabel();
				byteConverter = converterAdviser.getConverter(label, value == null?null:value.getClass().getCanonicalName());
				hint = byteConverter.conversionHint();
				st.setObject(4, labelConverter.toPersistence(label));
			}
			st.setBlob(7, value == null? null:toBlob( byteConverter.toPersistence(value)));
			Map<String,Object> properties = new HashMap<>();
			cart.getAllProperties().forEach((k,v)->{
				if(isPersistentProperty(k)) {
					properties.put(k, v);
				}
			});
			
			st.setClob(8, mapConverter.toPersistence(properties));
			st.setString(9, hint);
			st.setLong(10, cart.getPriority());
			st.execute();
		} catch (Exception e) {
			e.printStackTrace();
	    	LOG.error("SavePart Exception: {} {}",cart,e.getMessage());
	    	throw new PersistenceException("Save Part failed for "+cart,e);
		}
	}
	
	private Blob toBlob(byte[] bytes) {
    	Blob blob       = null;
    	OutputStream os = null;
		try {
			blob = connectionSupplier.get().createBlob();
	    	os = blob.setBinaryStream(1);
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception",e);
		}
		try {
			if(bytes != null) {
				os.write( bytes );
			} else {
				os.write( new byte[] {} );
			}
			os.flush();
			os.close();
			return blob;
		} catch (IOException e) {
			throw new PersistenceException("IO Runntime Exception",e);
		}
	}

	private byte[] fromBlob(Blob blob) {
		if(blob == null) {
			return null;
		}
		try(InputStream in = blob.getBinaryStream(1, blob.length())) {
			return IOUtils.toByteArray(in);
		} catch (SQLException e) {
			throw new PersistenceException("SQL Runntime Exception",e);
		} catch (IOException e) {
			throw new PersistenceException("IO Runntime Exception",e);
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
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.saveCompletedBuildKeyQuery()) ) {
			st.setObject(1, key);
			st.execute();
		} catch (Exception e) {
	    	LOG.error("SaveCompletedKey {} Exception: {}",key,e.getMessage());
	    	e.printStackTrace();
	    	throw new PersistenceException("SaveCompletedKey failed",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllPartIds(java.lang.Object)
	 */
	@Override
	public Collection<Long> getAllPartIds(K key) {
		Set<Long> res = new LinkedHashSet<>();
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getAllPartIdsQuery()) ) {
			st.setObject(1, key);
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				Long id = rs.getLong(1);
				res.add(id);
			}
		} catch (Exception e) {
	    	LOG.error("getAllPartIds Exception: {}",key,e.getMessage());
	    	throw new PersistenceException("getAllPartIds failed",e);
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getParts(java.util.Collection)
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getParts(Collection<Long> ids) {
		//TODO finish

		LOG.debug("getAllParts for: {}",ids);
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		
		Cart<K, ?, L> cart = null;
		String idList = ids.stream().map(n->n.toString()).collect(Collectors.joining( "," ));
		String query = dynamicPersistenceSql.getPartQuery().replace("?", idList);
		LOG.debug("getPart: {} {}",ids,dynamicPersistenceSql.getPartQuery());
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(query) ) {
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				String labelString = rs.getString(3);
				String hint = rs.getString(8);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				long priority = rs.getLong(9);
				if(loadType == LoadType.COMMAND) {
					CommandLabel command = CommandLabel.valueOf(labelString.trim());
					if(command == CommandLabel.RESTORE_BUILD) {
						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command, hint);
						Memento memento = (Memento) byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));
						cart = new GeneralCommand(key, memento, command, creationTime, expirationTime);
					}
				} else {
					L label = null;
					if(labelString != null) {
						label = (L)labelConverter.fromPersistence(labelString.trim());
					}
					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));

					Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
//					LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
				
					if(loadType == LoadType.BUILDER) {
						cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.RESULT_CONSUMER) {
						cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.MULTI_KEY_PART) {
						Load load = (Load)val;
						cart = new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(),properties,priority);
					} else {
						cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType,priority);
					}
				}
				LOG.debug("Read cart: {}",cart);
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getPart Exception: {}",ids,e.getMessage());
	    	throw new PersistenceException("getPart failed",e);
		}
		
		return carts;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		LOG.debug("getAllParts: {}",dynamicPersistenceSql.getAllUnfinishedPartIdsQuery());
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getAllUnfinishedPartIdsQuery()) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				String labelString = rs.getString(3);
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				String hint = rs.getString(8);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				long priority = rs.getLong(9);
				if(loadType == LoadType.COMMAND) {
					CommandLabel command = CommandLabel.valueOf(labelString.trim());
					if(command == CommandLabel.RESTORE_BUILD) {
						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command, hint);
						Memento memento = (Memento) byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));
						cart = new GeneralCommand(key, memento, command, creationTime, expirationTime);
					}
				} else {
					L label = null;
					if(labelString != null) {
						label = (L)labelConverter.fromPersistence(labelString.trim());
					}
				
					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));

					Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
//					LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
					if(loadType == LoadType.BUILDER) {
						cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.RESULT_CONSUMER) {
						cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.MULTI_KEY_PART) {
						Load load = (Load)val;
						cart = new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(),properties,priority);
					} else {
						cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType,priority);
					}
				}
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getAllUnfinishedPartIdsQuery exception: ",e.getMessage());
	    	throw new PersistenceException("getAllUnfinishedPartIdsQuery failed",e);
		}
		return carts;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getExpiredParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getExpiredParts() {
		LOG.debug("getExpiredParts: {}",dynamicPersistenceSql.getExpiredPartQuery());
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getExpiredPartQuery()) ) {
			Cart<K, ?, L> cart = null;
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				K key = (K)rs.getObject(1);
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				String labelString = rs.getString(3);
				String hint = rs.getString(8);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				long priority = rs.getLong(9);
				if(loadType == LoadType.COMMAND) {
					CommandLabel command = CommandLabel.valueOf(labelString.trim());
					if(command == CommandLabel.RESTORE_BUILD) {
						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command, hint);
						Memento memento = (Memento) byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));
						cart = new GeneralCommand(key, memento, command, creationTime, expirationTime);
					}
				} else {
					L label = null;
					if(labelString != null) {
						label = (L)labelConverter.fromPersistence(labelString.trim());
					}
				
					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(fromBlob(rs.getBlob(2)));

					Map<String,Object> properties = mapConverter.fromPersistence(rs.getClob(7));
//					LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
					if(loadType == LoadType.BUILDER) {
						cart = new CreatingCart<>(key, (BuilderSupplier)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.RESULT_CONSUMER) {
						cart = new ResultConsumerCart<>(key, (ResultConsumer)val, creationTime, expirationTime,priority);
					} else if(loadType == LoadType.MULTI_KEY_PART) {
						Load load = (Load)val;
						cart = new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,load.getLoadType(),properties,priority);
					} else {
						cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType,priority);
					}
				}
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getExpiredParts exception: ",e.getMessage());
	    	throw new PersistenceException("getExpiredParts failed",e);
		}
		return carts;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getCompletedKeys()
	 */
	@Override
	public Set<K> getCompletedKeys() {
		Set<K> res = new LinkedHashSet<>();
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getAllCompletedKeysQuery()) ) {
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				@SuppressWarnings("unchecked")
				K key = (K) rs.getObject(1);
				res.add(key);
			}
		} catch (Exception e) {
	    	LOG.error("getCompletedKeys Exception:",e.getMessage());
	    	throw new PersistenceException("getCompletedKeys failed",e);
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		archiver.archiveParts(connectionSupplier.get(),ids);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		archiver.archiveKeys(connectionSupplier.get(), keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		archiver.archiveCompleteKeys(connectionSupplier.get(), keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveAll()
	 */
	@Override
	public void archiveAll() {
		archiver.archiveAll(connectionSupplier.get());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllStaticParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
		LOG.debug("getAllStaticParts: {}",dynamicPersistenceSql.getAllStaticPartsQuery());
		Collection<Cart<K, ?, L>> carts = new ArrayList<>();
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getAllStaticPartsQuery()) ) {
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
				cart = new ShoppingCart<>(key,val,label,creationTime,expirationTime,properties,loadType,0);
				carts.add(cart);
			}
		} catch (Exception e) {
	    	LOG.error("getAllStaticParts exception: ",e.getMessage());
	    	throw new PersistenceException("getAllStaticParts failed",e);
		}
		return carts;
	}

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			connectionSupplier.get().close();
		} catch (SQLException e) {
			throw new IOException("SQL Connection close error",e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		archiver.archiveExpiredParts(connectionSupplier.get());
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
		try(PreparedStatement st = connectionSupplier.get().prepareStatement(dynamicPersistenceSql.getNumberOfPartsQuery()) ) {
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				res = rs.getLong(1);
			}
		} catch (Exception e) {
	    	LOG.error("getNumberOfParts Exception:",e.getMessage());
	    	throw new PersistenceException("getNumberOfParts failed",e);
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

	@Override
	public int getMinCompactSize() {
		return minCompactSize ;
	}
	
	public Connection getConnection() {
		return connectionSupplier.get();
	}

}