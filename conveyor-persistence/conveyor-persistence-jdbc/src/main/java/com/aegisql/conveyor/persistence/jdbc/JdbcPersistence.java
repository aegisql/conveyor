package com.aegisql.conveyor.persistence.jdbc;

import java.io.IOException;
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
import com.aegisql.conveyor.persistence.jdbc.converters.MapToJsonConverter;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistenceBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class JdbcPersistence.
 *
 * @param <K>
 *            the key type
 */
public class JdbcPersistence<K> implements Persistence<K> {

	/** The Constant LOG. */
	final static Logger LOG = LoggerFactory.getLogger(JdbcPersistence.class);

	/** The connectionSupplier. */
	///////////////////////////////////////////////////////////////////////////////
	private final ConnectionSupplier connectionSupplier;

	/** The id supplier. */
	private final LongSupplier idSupplier;

	/** The blob converter. */
	// private final BlobConverter blobConverter;

	private final ConverterAdviser converterAdviser;

	private final MapToJsonConverter mapConverter;

	/** The load type converter. */
	private final EnumConverter<LoadType> loadTypeConverter = new EnumConverter<>(LoadType.class);

	/** The label converter. */
	private final ObjectConverter labelConverter;

	// private final DynamicPersistenceSql dynamicPersistenceSql;

	/** The archiver. */
	private final Archiver<K> archiver;

	/** The max batch size. */
	private final int maxBatchSize;

	/** The max batch time. */
	private final long maxBatchTime;

	private final String info;

	private final Set<String> nonPersistentProperties;

	private final EngineDepo<K> engine;

	private int minCompactSize = 0;

	private final static CommandLabel RESTORE_BUILD_COMMAND = CommandLabel.RESTORE_BUILD;

	/**
	 * Instantiates a new derby persistence.
	 *
	 * @param builder
	 *            the builder
	 * @param connectionSupplier
	 *            the connectionSupplier
	 * @param idSupplier
	 *            the id supplier
	 * @param saveCartQuery
	 *            the save cart query
	 * @param saveCompletedBuildKeyQuery
	 *            the save completed build key query
	 * @param getPartQuery
	 *            the get part query
	 * @param getAllPartIdsQuery
	 *            the get all part ids query
	 * @param getAllUnfinishedPartIdsQuery
	 *            the get all unfinished part ids query
	 * @param getAllCompletedKeysQuery
	 *            the get all completed keys query
	 * @param getAllStaticPartsQuery
	 *            the get all static parts query
	 * @param getNumberOfPartsQuery
	 *            the get number of parts query
	 * @param archiver
	 *            the archiver
	 * @param labelConverter
	 *            the label converter
	 * @param blobConverter
	 *            the blob converter
	 * @param maxBatchSize
	 *            the max batch size
	 * @param maxBatchTime
	 *            the max batch time
	 */
	public JdbcPersistence(ConnectionSupplier connectionSupplier, EngineDepo<K> engine, LongSupplier idSupplier,
			DynamicPersistenceSql dynamicPersistenceSql, Archiver<K> archiver,
			ObjectConverter<?, String> labelConverter, ConverterAdviser<?> converterAdviser, int maxBatchSize,
			long maxBatchTime, String info, Set<String> nonPersistentProperties, int minCompactSize) {
		this.connectionSupplier = connectionSupplier;
		this.idSupplier = idSupplier;
		this.converterAdviser = converterAdviser;
		// this.dynamicPersistenceSql = dynamicPersistenceSql;
		this.archiver = archiver;
		this.labelConverter = labelConverter;
		this.maxBatchSize = maxBatchSize;
		this.maxBatchTime = maxBatchTime;
		this.mapConverter = new MapToJsonConverter();
		this.info = info;
		this.nonPersistentProperties = nonPersistentProperties;
		this.minCompactSize = minCompactSize;
		this.engine = engine;
		this.archiver.setPersistence(this);
	}

	/**
	 * For key class.
	 *
	 * @param <K>
	 *            the key type
	 * @param clas
	 *            the clas
	 * @return the derby persistence builder
	 */
	public static <K> DerbyPersistenceBuilder<K> forKeyClass(Class<K> clas) {
		return new DerbyPersistenceBuilder<K>(clas);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#copy()
	 */
	@Override
	public Persistence<K> copy() {
		return new JdbcPersistence<>(connectionSupplier.clone(), engine, idSupplier, null, archiver, labelConverter,
				converterAdviser, maxBatchSize, maxBatchTime, info, nonPersistentProperties, minCompactSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#nextUniquePartId()
	 */
	@Override
	public long nextUniquePartId() {
		return idSupplier.getAsLong();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#savePart(long,
	 * com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		LOG.debug("SAVING: {}", cart);
		String hint;
		ObjectConverter<Object, byte[]> byteConverter;
		Object value = cart.getValue();
		Object label = null;
		Map<String, Object> properties = new HashMap<>();
		cart.getAllProperties().forEach((k, v) -> {
			if (isPersistentProperty(k)) {
				properties.put(k, v);
			}
		});

		if (cart instanceof GeneralCommand) {
			byteConverter = converterAdviser.getConverter(RESTORE_BUILD_COMMAND,
					value == null ? null : value.getClass().getCanonicalName());
			hint = byteConverter.conversionHint();
			label = labelConverter.toPersistence(RESTORE_BUILD_COMMAND);
		} else {
			byteConverter = converterAdviser.getConverter(label,
					value == null ? null : value.getClass().getCanonicalName());
			hint = byteConverter.conversionHint();
			label = labelConverter.toPersistence(cart.getLabel());
		}
		engine.saveCart(id, loadTypeConverter.toPersistence(cart.getLoadType()), cart.getKey(), label,

				new Timestamp(cart.getCreationTime()), new Timestamp(cart.getExpirationTime()), byteConverter.toPersistence(value),
				mapConverter.toPersistence(properties), hint, cart.getPriority());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#savePartId(java.lang.
	 * Object, long)
	 */
	@Override
	public void savePartId(K key, long partId) {
		// DO NOTHING. SUPPORTED BY SECONDARY INDEX ON THE PART TABLE
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#saveCompletedBuildKey(java.
	 * lang.Object)
	 */
	@Override
	public void saveCompletedBuildKey(K key) {
		engine.saveCompletedBuildKey(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#getAllPartIds(java.lang.
	 * Object)
	 */
	@Override
	public Collection<Long> getAllPartIds(K key) {
		return engine.getAllPartIds(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getParts(java.util.
	 * Collection)
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getParts(Collection<Long> ids) {
		// TODO finish

		LOG.debug("getAllParts for: {}", ids);
		return engine.getParts(ids, rs -> {
			try {
				K key = (K) rs.getObject(1);
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				String labelString = rs.getString(3);
				String hint = rs.getString(8);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				long priority = rs.getLong(9);
				if (loadType == LoadType.COMMAND) {
					CommandLabel command = CommandLabel.valueOf(labelString.trim());
					if (command == CommandLabel.RESTORE_BUILD) {
						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command, hint);
						Memento memento = (Memento) byteConverter.fromPersistence(rs.getBytes(2));
						return new GeneralCommand(key, memento, command, creationTime, expirationTime);
					}
				} else {
					L label = null;
					if (labelString != null) {
						label = (L) labelConverter.fromPersistence(labelString.trim());
					}
					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(rs.getBytes(2));

					Map<String, Object> properties = mapConverter.fromPersistence(rs.getString(7));
					// LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);

					if (loadType == LoadType.BUILDER) {
						return new CreatingCart<>(key, (BuilderSupplier) val, creationTime, expirationTime, priority);
					} else if (loadType == LoadType.RESULT_CONSUMER) {
						return new ResultConsumerCart<>(key, (ResultConsumer) val, creationTime, expirationTime,
								priority);
					} else if (loadType == LoadType.MULTI_KEY_PART) {
						Load load = (Load) val;
						return new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,
								load.getLoadType(), properties, priority);
					} else {
						return new ShoppingCart<>(key, val, label, creationTime, expirationTime, properties, loadType,
								priority);
					}
				}
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
			throw new PersistenceException("Unexpected result");
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		return engine.getUnfinishedParts(rs -> {
			try {
				Cart<K, ?, L> cart = null;
				K key = (K) rs.getObject(1);
				String labelString = rs.getString(3);
				LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
				String hint = rs.getString(8);
				long creationTime = rs.getTimestamp(4).getTime();
				long expirationTime = rs.getTimestamp(5).getTime();
				long priority = rs.getLong(9);
				if (loadType == LoadType.COMMAND) {
					CommandLabel command = CommandLabel.valueOf(labelString.trim());
					if (command == CommandLabel.RESTORE_BUILD) {
						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command, hint);
						Memento memento = (Memento) byteConverter.fromPersistence(rs.getBytes(2));
						return new GeneralCommand(key, memento, command, creationTime, expirationTime);
					}
				} else {
					L label = null;
					if (labelString != null) {
						label = (L) labelConverter.fromPersistence(labelString.trim());
					}

					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(rs.getBytes(2));

					Map<String, Object> properties = mapConverter.fromPersistence(rs.getString(7));
					// LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
					if (loadType == LoadType.BUILDER) {
						return new CreatingCart<>(key, (BuilderSupplier) val, creationTime, expirationTime, priority);
					} else if (loadType == LoadType.RESULT_CONSUMER) {
						return new ResultConsumerCart<>(key, (ResultConsumer) val, creationTime, expirationTime,
								priority);
					} else if (loadType == LoadType.MULTI_KEY_PART) {
						Load load = (Load) val;
						return new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime, expirationTime,
								load.getLoadType(), properties, priority);
					} else {
						return new ShoppingCart<>(key, val, label, creationTime, expirationTime, properties, loadType,
								priority);
					}
				}

			} catch (Exception e) {
				LOG.error("getAllUnfinishedPartIdsQuery exception: ", e.getMessage());
				throw new PersistenceException("getAllUnfinishedPartIdsQuery failed", e);
			}
			throw new PersistenceException("Unexpected result in getAllParts");

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getExpiredParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getExpiredParts() {

		return engine.getExpiredParts(rs -> {
			try {
					K key = (K) rs.getObject(1);
					LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
					String labelString = rs.getString(3);
					String hint = rs.getString(8);
					long creationTime = rs.getTimestamp(4).getTime();
					long expirationTime = rs.getTimestamp(5).getTime();
					long priority = rs.getLong(9);
					if (loadType == LoadType.COMMAND) {
						CommandLabel command = CommandLabel.valueOf(labelString.trim());
						if (command == CommandLabel.RESTORE_BUILD) {
							ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(command,
									hint);
							Memento memento = (Memento) byteConverter.fromPersistence(rs.getBytes(2));
							return new GeneralCommand(key, memento, command, creationTime, expirationTime);
						}
					} else {
						L label = null;
						if (labelString != null) {
							label = (L) labelConverter.fromPersistence(labelString.trim());
						}

						ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
						Object val = byteConverter.fromPersistence(rs.getBytes(2));

						Map<String, Object> properties = mapConverter.fromPersistence(rs.getString(7));
						// LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
						if (loadType == LoadType.BUILDER) {
							return new CreatingCart<>(key, (BuilderSupplier) val, creationTime, expirationTime,
									priority);
						} else if (loadType == LoadType.RESULT_CONSUMER) {
							return new ResultConsumerCart<>(key, (ResultConsumer) val, creationTime, expirationTime,
									priority);
						} else if (loadType == LoadType.MULTI_KEY_PART) {
							Load load = (Load) val;
							return new MultiKeyCart(load.getFilter(), load.getValue(), label, creationTime,
									expirationTime, load.getLoadType(), properties, priority);
						} else {
							return new ShoppingCart<>(key, val, label, creationTime, expirationTime, properties,
									loadType, priority);
						}
				}
			} catch (Exception e) {
				LOG.error("getExpiredParts exception: ", e.getMessage());
				throw new PersistenceException("getExpiredParts failed", e);
			}
			throw new PersistenceException("Unexpected result in getExpiredParts");

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getCompletedKeys()
	 */
	@Override
	public Set<K> getCompletedKeys() {
		return engine.getAllCompletedKeys(rs->{
			try {
				return (K) rs.getObject(1);
			} catch (Exception e) {
				LOG.error("getCompletedKeys Exception:", e.getMessage());
				throw new PersistenceException("getCompletedKeys failed", e);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#archiveParts(java.util.
	 * Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		archiver.archiveParts(null, ids);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveKeys(java.util.
	 * Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		archiver.archiveKeys(null, keys);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#archiveCompleteKeys(java.
	 * util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		archiver.archiveCompleteKeys(null, keys);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveAll()
	 */
	@Override
	public void archiveAll() {
		archiver.archiveAll(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getAllStaticParts()
	 */
	@Override
	public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
		return engine.getStaticParts(rs->{
			try {
					K key = (K) rs.getObject(1);
					L label = (L) labelConverter.fromPersistence(rs.getString(3).trim());
					String hint = rs.getString(8);
					ObjectConverter<Object, byte[]> byteConverter = converterAdviser.getConverter(label, hint);
					Object val = byteConverter.fromPersistence(rs.getBytes(2));
					long creationTime = rs.getTimestamp(4).getTime();
					long expirationTime = rs.getTimestamp(5).getTime();
					LoadType loadType = loadTypeConverter.fromPersistence(rs.getString(6).trim());
					Map<String, Object> properties = mapConverter.fromPersistence(rs.getString(7));
					// LOG.debug("{},{},{},{},{},{}",key,val,label,creationTime,expirationTime,loadType);
					return new ShoppingCart<>(key, val, label, creationTime, expirationTime, properties, loadType, 0);
			} catch (Exception e) {
				LOG.error("getAllStaticParts exception: ", e.getMessage());
				throw new PersistenceException("getAllStaticParts failed", e);
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		try {
			connectionSupplier.get().close();
		} catch (SQLException e) {
			throw new IOException("SQL Connection close error", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		archiver.archiveExpiredParts(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#getMaxArchiveBatchSize()
	 */
	@Override
	public int getMaxArchiveBatchSize() {
		return maxBatchSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.persistence.core.Persistence#getMaxArchiveBatchTime()
	 */
	@Override
	public long getMaxArchiveBatchTime() {
		return maxBatchTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getNumberOfParts()
	 */
	@Override
	public long getNumberOfParts() {
		return engine.getNumberOfParts();
	}

	@Override
	public String toString() {
		return info;
	}

	@Override
	public boolean isPersistentProperty(String property) {
		return !nonPersistentProperties.contains(property);
	}

	@Override
	public int getMinCompactSize() {
		return minCompactSize;
	}

	public Connection getConnection() {
		return connectionSupplier.get();
	}

}