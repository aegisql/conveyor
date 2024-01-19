package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.*;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.converters.EnumConverter;
import com.aegisql.conveyor.persistence.jdbc.converters.MapToJsonConverter;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO: Auto-generated Javadoc

/**
 * The Class JdbcPersistence.
 *
 * @param <K> the key type
 */
public class JdbcPersistence<K> implements Persistence<K> {

	/**
	 * The Constant LOG.
	 */
	final static Logger LOG = LoggerFactory.getLogger(JdbcPersistence.class);

	/** The id supplier. */
	private final LongSupplier idSupplier;

	/** The blob converter. */
	// private final BlobConverter blobConverter;

	private final ConverterAdviser converterAdviser;

	/** The map converter. */
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

	/** The info. */
	private final String info;

	/** The non persistent properties. */
	private final Set<String> nonPersistentProperties;

	/** The engine. */
	private final EngineDepo<K> engine;

	/** The min compact size. */
	private final int minCompactSize;

	/** The Constant RESTORE_BUILD_COMMAND. */
	private final static CommandLabel RESTORE_BUILD_COMMAND = CommandLabel.RESTORE_BUILD;
	
	/** The additional fields. */
	private final List<Field<?>> additionalFields;
	private final Predicate<Cart<K, ?, ?>> persistentPartFilter;


	/**
	 * Instantiates a new derby persistence.
	 *
	 * @param engine                  the engine
	 * @param idSupplier              the id supplier
	 * @param archiver                the archiver
	 * @param labelConverter          the label converter
	 * @param converterAdviser        the converter adviser
	 * @param maxBatchSize            the max batch size
	 * @param maxBatchTime            the max batch time
	 * @param info                    the info
	 * @param nonPersistentProperties the non persistent properties
	 * @param minCompactSize          the min compact size
	 * @param additionalFields        the additional fields
	 * @param persistentPartFilter    the filter for parts that should or should not be persisted
	 */
	public JdbcPersistence(EngineDepo<K> engine, LongSupplier idSupplier,
                           Archiver<K> archiver,
                           ObjectConverter<?, String> labelConverter, ConverterAdviser<?> converterAdviser, int maxBatchSize,
                           long maxBatchTime, String info, Set<String> nonPersistentProperties, int minCompactSize, List<Field<?>> additionalFields, Predicate<Cart<K, ?, ?>> persistentPartFilter) {
		this.idSupplier = idSupplier;
		this.converterAdviser = converterAdviser;
		this.archiver = archiver;
		this.labelConverter = labelConverter;
		this.maxBatchSize = maxBatchSize;
		this.maxBatchTime = maxBatchTime;
        this.persistentPartFilter = persistentPartFilter;
        this.mapConverter = new MapToJsonConverter();
		this.info = info;
		this.nonPersistentProperties = nonPersistentProperties;
		this.minCompactSize = minCompactSize;
		this.engine = engine;
		this.archiver.setPersistence(this);
		this.additionalFields = additionalFields;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#copy()
	 */
	@Override
	public Persistence<K> copy() {
		return new JdbcPersistence<>(engine, idSupplier, archiver, labelConverter,
				converterAdviser, maxBatchSize, maxBatchTime, info, nonPersistentProperties, minCompactSize, new ArrayList<>(additionalFields), persistentPartFilter);
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
		if ( ! isPartPersistent(cart)) {
			LOG.debug("IGNORING: {}", cart);
			return;
		}
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
		String valueClassName = value == null ? null : value.getClass().getTypeName();
		if (cart instanceof GeneralCommand) {
			byteConverter = converterAdviser.getConverter(RESTORE_BUILD_COMMAND,
					valueClassName);
			hint = byteConverter.conversionHint();
			label = labelConverter.toPersistence(RESTORE_BUILD_COMMAND);
		} else {
			byteConverter = converterAdviser.getConverter(label,
					valueClassName);
			hint = byteConverter.conversionHint();
			label = labelConverter.toPersistence(cart.getLabel());
		}
		
		List<Object> fields = additionalFields
				.stream()
				.map(f->f.getAccessor().apply(cart))
				.collect(Collectors.toList());
		
		engine.saveCart(id, loadTypeConverter.toPersistence(cart.getLoadType()), cart.getKey(), label,
				new Timestamp(cart.getCreationTime()), new Timestamp(cart.getExpirationTime()), byteConverter.toPersistence(value),
				mapConverter.toPersistence(properties), hint, cart.getPriority()
				,fields
				);
	}

	@Override
	public <L> boolean isPartPersistent(Cart<K, ?, L> cart) {
		return persistentPartFilter.test(cart);
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
				LOG.error("getAllUnfinishedPartIdsQuery exception", e);
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
				LOG.error("getExpiredParts exception", e);
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
				LOG.error("getCompletedKeys Exception", e);
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
		archiver.archiveParts(ids);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveKeys(java.util.
	 * Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		archiver.archiveKeys(keys);
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
		archiver.archiveCompleteKeys(keys);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveAll()
	 */
	@Override
	public void archiveAll() {
		archiver.archiveAll();
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
				LOG.error("getAllStaticParts exception", e);
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
		engine.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.persistence.core.Persistence#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		archiver.archiveExpiredParts();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return info;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#isPersistentProperty(java.lang.String)
	 */
	@Override
	public boolean isPersistentProperty(String property) {
		return !nonPersistentProperties.contains(property);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.Persistence#getMinCompactSize()
	 */
	@Override
	public int getMinCompactSize() {
		return minCompactSize;
	}

}