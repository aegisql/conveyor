package com.aegisql.conveyor.config;

import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration.BinaryLogConfigurationBuilder;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.builders.RestoreOrder;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.redis.PriorityRestoreStrategy;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;

// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceProperties.
 */
public class PersistenceProperties {

	private final static Logger LOG = LoggerFactory.getLogger(PersistenceProperties.class);

	/** The is default. */
	private final boolean isDefault;
	
	/** The type. */
	private final String type;
	
	/** The schema. */
	private final String schema;
	
	/** The name. */
	private final String name;

	/** The properties. */
	private final Map<String,LinkedList<PersistenceProperty>> properties = new HashMap<>();
	
	/**
	 * Instantiates a new persistence properties.
	 *
	 * @param type the type
	 * @param schema the schema
	 * @param name the name
	 */
	public PersistenceProperties(String type, String schema, String name) {
		this.type   = type;
		this.schema = schema;
		this.name   = name;
		this.isDefault = type == null || schema == null || name == null;
	}
	
	/**
	 * Adds the property.
	 *
	 * @param pp the pp
	 */
	public void addProperty(PersistenceProperty pp) {
		LinkedList<PersistenceProperty> ppList = properties.computeIfAbsent(pp.getProperty(), k -> new LinkedList<>());
		ppList.add(pp);
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, LinkedList<PersistenceProperty>> getProperties() {
		return properties;
	}

	/**
	 * Gets the level 0 key.
	 *
	 * @return the level 0 key
	 */
	public String getLevel0Key() {
		return "";
	}

	/**
	 * Gets the level 1 key.
	 *
	 * @return the level 1 key
	 */
	public String getLevel1Key() {
		return getType();
	}

	/**
	 * Gets the level 2 key.
	 *
	 * @return the level 2 key
	 */
	public String getLevel2Key() {
		return getType()+"."+getSchema();
	}

	public void merge(PersistenceProperties other) {
		if(other != null) {
			other.getProperties().forEach(properties::putIfAbsent);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PersistenceProperties [isDefault=" + isDefault + ", " + (type != null ? "type=" + type + ", " : "")
				+ (schema != null ? "schema=" + schema + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (properties != null ? "properties=" + properties : "") + "]";
	}
	
	public String buildPersistence() {
		LinkedList<PersistenceProperty> keyProperties = properties.get("keyClass");
		if (keyProperties == null || keyProperties.isEmpty()) {
			throw new ConveyorConfigurationException("Missing mandatory Persistence property 'keyClass' in " + this);
		}
		PersistenceProperty keyProperty = keyProperties.getLast();
		try {
			Class keyClass = keyProperty.getValueAsClass();
			if (isRedisType()) {
				return buildRedisPersistence(keyClass);
			}
			return buildJdbcPersistence(keyClass);
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Failed building persistence for "+this,e);
		}
	}

	private boolean isRedisType() {
		return "redis".equalsIgnoreCase(getType());
	}

	private String buildJdbcPersistence(Class keyClass) throws Exception {
		BinaryLogConfigurationBuilder bLogConf = BinaryLogConfiguration.builder(getName());

		JdbcPersistenceBuilder pb = JdbcPersistenceBuilder.presetInitializer(getType().toLowerCase(),keyClass)
				.autoInit(true)
				.schema(getSchema())
				.partTable(getName());
		pb = applyDatabase(pb);
		pb = applyDbPath(pb);
		pb = applyDriverClass(pb);
		pb = applyAutoInit(pb);
		pb = applyRestoreOrder(pb);
		pb = applyUsername(pb);
		pb = applyPassword(pb);
		pb = applyCompletedLogTable(pb);
		pb = applyHost(pb);
		pb = applyPort(pb);
		pb = applyEncryptionAlgorithm(pb);
		pb = applyEncryptionTransformation(pb);
		pb = applyEncryptionKeyLength(pb);
		pb = applyEncryptionSecret(pb);
		pb = applyMaxBatchSize(pb);
		pb = applyMaxBatchTime(pb);
		pb = applyDoNotSaveProperties(pb);
		pb = applyArchiveStrategyPath(pb,bLogConf);
		pb = applyArchiveStrategyMoveTo(pb,bLogConf);
		pb = applyArchiveStrategyMaxFileSize(pb,bLogConf);
		pb = applyArchiveStrategyBucketSize(pb,bLogConf);
		pb = applyArchiveStrategyZip(pb,bLogConf);
		pb = applyArchiveStrategy(pb,bLogConf);
		pb = applyArchiveStrategyArchiver(pb);
		pb = applyArchiveStrategyPersistence(pb);
		pb = applyLabelConverter(pb);
		pb = applyIdSupplier(pb);
		pb = applyAddBinaryConverter(pb);
		pb = applyAddField(pb);
		pb = applyAddUniqueFields(pb);
		pb = applyMinCompactSize(pb);
		pb = applyDbcp2(pb);
		pb = applyPoolConnection(pb);
		pb = applyProperties(pb);

		pb.build();

		return pb.getJMXObjName();
	}

	private String buildRedisPersistence(Class keyClass) {
		String persistenceName = resolveRedisPersistenceName();
		BinaryLogConfigurationBuilder bLogConf = BinaryLogConfiguration.builder(persistenceName);

		RedisPersistenceBuilder pb = new RedisPersistenceBuilder<>(persistenceName)
				.autoInit(true);
		pb = applyRedisUri(pb);
		pb = applyAutoInit(pb);
		pb = applyRestoreOrder(pb);
		pb = applyPriorityRestoreStrategy(pb);
		pb = applyUsername(pb);
		pb = applyPassword(pb);
		pb = applyEncryptionAlgorithm(pb);
		pb = applyEncryptionTransformation(pb);
		pb = applyEncryptionKeyLength(pb);
		pb = applyEncryptionSecret(pb);
		pb = applyMaxBatchSize(pb);
		pb = applyMaxBatchTime(pb);
		pb = applyDoNotSaveProperties(pb);
		pb = applyArchiveStrategyPath(pb,bLogConf);
		pb = applyArchiveStrategyMoveTo(pb,bLogConf);
		pb = applyArchiveStrategyMaxFileSize(pb,bLogConf);
		pb = applyArchiveStrategyBucketSize(pb,bLogConf);
		pb = applyArchiveStrategyZip(pb,bLogConf);
		pb = applyArchiveStrategy(pb,bLogConf);
		pb = applyArchiveStrategyArchiver(pb);
		pb = applyArchiveStrategyPersistence(pb);
		pb = applyAddBinaryConverter(pb);
		pb = applyAddField(pb);
		pb = applyMinCompactSize(pb);
		pb = applyMaxTotal(pb);
		pb = applyMaxIdle(pb);
		pb = applyMinIdle(pb);
		pb = applyConnectionTimeoutMillis(pb);
		pb = applySocketTimeoutMillis(pb);
		pb = applyBlockingSocketTimeoutMillis(pb);
		pb = applyDatabase(pb);
		pb = applyClientName(pb);
		pb = applySsl(pb);

		pb.build();

		return pb.getJMXObjName();
	}

	private String resolveRedisPersistenceName() {
		if (getSchema() == null || getSchema().isBlank()) {
			return getName();
		}
		if (getName() == null || getName().isBlank()) {
			return getSchema();
		}
		return getSchema() + "." + getName();
	}

	private JdbcPersistenceBuilder applyProperties(final JdbcPersistenceBuilder pb) {
		return apply("properties",pb,p->{
			Properties prop = new Properties();
			Map<String,String> map = (Map<String, String>) p.getValue();
			map.forEach((k,v)->prop.put(k,v));
			return pb.properties(prop);
		});
	}

	private RedisPersistenceBuilder applyRedisUri(final RedisPersistenceBuilder pb) {
		return apply("redisUri", pb, p -> pb.redisUri(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyMaxTotal(final RedisPersistenceBuilder pb) {
		return apply("maxTotal", pb, p -> pb.maxTotal(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyMaxIdle(final RedisPersistenceBuilder pb) {
		return apply("maxIdle", pb, p -> pb.maxIdle(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyMinIdle(final RedisPersistenceBuilder pb) {
		return apply("minIdle", pb, p -> pb.minIdle(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyConnectionTimeoutMillis(final RedisPersistenceBuilder pb) {
		return apply("connectionTimeoutMillis", pb, p -> pb.connectionTimeoutMillis(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applySocketTimeoutMillis(final RedisPersistenceBuilder pb) {
		return apply("socketTimeoutMillis", pb, p -> pb.socketTimeoutMillis(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyBlockingSocketTimeoutMillis(final RedisPersistenceBuilder pb) {
		return apply("blockingSocketTimeoutMillis", pb, p -> pb.blockingSocketTimeoutMillis(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyClientName(final RedisPersistenceBuilder pb) {
		return apply("clientName", pb, p -> pb.clientName(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applySsl(final RedisPersistenceBuilder pb) {
		return apply("ssl", pb, p -> pb.ssl(p.getValueAsBoolean()));
	}


	private JdbcPersistenceBuilder applyPoolConnection(final JdbcPersistenceBuilder pb) {
		return apply("poolConnection",pb,p-> pb.poolConnection(p.getValueAsBoolean()));
	}

	private JdbcPersistenceBuilder applyDbcp2(final JdbcPersistenceBuilder pb) {
		return apply("dbcp2",pb,p->{
			if(p.getValueAsBoolean()) {
				return pb.dbcp2Connection();
			} else {
				return pb;
			}
		});
	}

	private JdbcPersistenceBuilder applyMinCompactSize(final JdbcPersistenceBuilder pb) {
		return apply("minCompactSize",pb,p->pb.minCompactSize(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyMinCompactSize(final RedisPersistenceBuilder pb) {
		return apply("minCompactSize",pb,p->pb.minCompactSize(p.getValueAsInteger()));
	}

	private JdbcPersistenceBuilder applyAddUniqueFields(final JdbcPersistenceBuilder pb) {
		return apply("addUniqueFields",pb,p->{
			String[] fields = p.getValueAsString().split(",");
			List<String> fl = new ArrayList<>();
			for(String f:fields) {
				fl.add(f.trim());
			}
			return pb.addUniqueFields(fl);
		});
	}

	private JdbcPersistenceBuilder applyAddField(final JdbcPersistenceBuilder pb) {
		return apply("addField",pb,p->{
			String[] classFieldName = p.getValueAsString().split(",",3);
			Class fieldClass = null;
			try {
				fieldClass = Class.forName(classFieldName[0].trim());
			} catch (ClassNotFoundException e) {
				throw new ConveyorConfigurationException("Failed extracting class name from "+p.getValueAsString(),e);
			}
			String name = classFieldName[1].trim();
			if(classFieldName.length == 2) {
				return pb.addField(fieldClass, name);
			} else {
				Function accessor = (Function)ConfigUtils.stringToFunctionSupplier.apply(classFieldName[2].trim());
				return pb.addField(fieldClass, name, accessor);
			}
		});
	}

	private RedisPersistenceBuilder applyAddField(final RedisPersistenceBuilder pb) {
		return apply("addField",pb,p->{
			String[] classFieldName = p.getValueAsString().split(",",3);
			Class fieldClass;
			try {
				fieldClass = Class.forName(classFieldName[0].trim());
			} catch (ClassNotFoundException e) {
				throw new ConveyorConfigurationException("Failed extracting class name from "+p.getValueAsString(),e);
			}
			String name = classFieldName[1].trim();
			if(classFieldName.length == 2) {
				return pb.addField(fieldClass, name);
			} else {
				Function accessor = (Function)ConfigUtils.stringToFunctionSupplier.apply(classFieldName[2].trim());
				return pb.addField(fieldClass, name, accessor);
			}
		});
	}

	private JdbcPersistenceBuilder applyAddBinaryConverter(final JdbcPersistenceBuilder pb) {
		return apply("addBinaryConverter",pb,p->{
			String[] s = p.getValueAsString().split(",");
			ObjectConverter oc = ConfigUtils.stringToObjectConverter.apply(s[1].trim());
			try {
				Class clas = Class.forName(s[0].trim());
				return pb.addBinaryConverter(clas, oc);
			} catch (Exception e) {
				Object label = ConfigUtils.stringToRefConverter.apply(s[0]);
				return pb.addBinaryConverter(label, oc);
			}
		});
	}

	private RedisPersistenceBuilder applyAddBinaryConverter(final RedisPersistenceBuilder pb) {
		return apply("addBinaryConverter",pb,p->{
			String[] s = p.getValueAsString().split(",");
			ObjectConverter oc = ConfigUtils.stringToObjectConverter.apply(s[1].trim());
			try {
				Class clas = Class.forName(s[0].trim());
				return pb.addBinaryConverter(clas, oc);
			} catch (Exception e) {
				Object label = ConfigUtils.stringToRefConverter.apply(s[0]);
				return pb.addBinaryConverter(label, oc);
			}
		});
	}

	private JdbcPersistenceBuilder applyIdSupplier(final JdbcPersistenceBuilder pb) {
		return apply("idSupplier",pb,p-> pb.idSupplier(ConfigUtils.stringToIdSupplier.apply(p.getValueAsString())));
	}

	private JdbcPersistenceBuilder applyLabelConverter(final JdbcPersistenceBuilder pb) {
		return apply("labelConverter",pb,p->{
			try {
				return pb.labelConverter(p.getValueAsClass());
			} catch (Exception e) {
				ObjectConverter oc = ConfigUtils.stringToObjectConverter.apply(p.getValueAsString());
				return pb.labelConverter(oc);
			}
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyPersistence(final JdbcPersistenceBuilder pb) {
		return apply("archiveStrategy.persistence",pb,p->{
			Persistence per = Persistence.byName(p.getValueAsString());
			return pb.archiver(per);
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyPersistence(final RedisPersistenceBuilder pb) {
		return apply("archiveStrategy.persistence",pb,p->{
			Persistence per = Persistence.byName(p.getValueAsString());
			return pb.archiver(per);
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyArchiver(final JdbcPersistenceBuilder pb) {
		return apply("archiveStrategy.archiver",pb,p->{
			Archiver ar = ConfigUtils.stringToArchiverConverter.apply(p.getValueAsString());
			return pb.archiver(ar);
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyArchiver(final RedisPersistenceBuilder pb) {
		return apply("archiveStrategy.archiver",pb,p->{
			Archiver ar = ConfigUtils.stringToArchiverConverter.apply(p.getValueAsString());
			return pb.archiver(ar);
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategy(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy",pb,p->{
			ArchiveStrategy as = ArchiveStrategy.valueOf(p.getValueAsString());
			return switch (as) {
				case NO_ACTION -> pb.noArchiving();
				case DELETE -> pb.deleteArchiving();
				case SET_ARCHIVED -> pb.setArchived();
				case MOVE_TO_FILE -> pb.archiver(bLogConf.build());
				default -> pb;
			};
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategy(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy",pb,p->{
			ArchiveStrategy as = ArchiveStrategy.valueOf(p.getValueAsString());
			return switch (as) {
				case NO_ACTION -> pb.noArchiving();
				case DELETE -> pb.deleteArchiving();
				case MOVE_TO_FILE -> pb.archiver(bLogConf.build());
				case SET_ARCHIVED -> throw new ConveyorConfigurationException("Redis persistence does not support archiveStrategy=SET_ARCHIVED");
				default -> pb;
			};
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyZip(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.zip",pb,p->{
			bLogConf.zipFile(p.getValueAsBoolean());
			return pb.archiver(bLogConf.build());
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyZip(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.zip",pb,p->{
			bLogConf.zipFile(p.getValueAsBoolean());
			return pb.archiver(bLogConf.build());
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyBucketSize(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.bucketSize",pb,p->{
			bLogConf.bucketSize(p.getValueAsInteger());
			return pb.archiver(bLogConf.build());
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyBucketSize(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.bucketSize",pb,p->{
			bLogConf.bucketSize(p.getValueAsInteger());
			return pb.archiver(bLogConf.build());
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyMaxFileSize(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.maxFileSize",pb,p->{
			bLogConf.maxFileSize(p.getValueAsString());
			return pb.archiver(bLogConf.build());
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyMaxFileSize(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.maxFileSize",pb,p->{
			bLogConf.maxFileSize(p.getValueAsString());
			return pb.archiver(bLogConf.build());
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyMoveTo(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.moveTo",pb,p->{
			bLogConf.moveToPath(resolveDbRelativePath(p.getValueAsString()));
			return pb.archiver(bLogConf.build());
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyMoveTo(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.moveTo",pb,p->{
			bLogConf.moveToPath(resolveDbRelativePath(p.getValueAsString()));
			return pb.archiver(bLogConf.build());
		});
	}

	private JdbcPersistenceBuilder applyArchiveStrategyPath(final JdbcPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.path",pb,p->{
			bLogConf.path(resolveDbRelativePath(p.getValueAsString()));
			return pb.archiver(bLogConf.build());
		});
	}

	private RedisPersistenceBuilder applyArchiveStrategyPath(final RedisPersistenceBuilder pb, final BinaryLogConfigurationBuilder bLogConf) {
		return apply("archiveStrategy.path",pb,p->{
			bLogConf.path(resolveDbRelativePath(p.getValueAsString()));
			return pb.archiver(bLogConf.build());
		});
	}

	private JdbcPersistenceBuilder applyMaxBatchTime(final JdbcPersistenceBuilder pb) {
		return apply("maxBatchTime",pb,p->pb.maxBatchTime(p.getValueAsDuration()));
	}

	private RedisPersistenceBuilder applyMaxBatchTime(final RedisPersistenceBuilder pb) {
		return apply("maxBatchTime",pb,p->pb.maxArchiveBatchTime(p.getValueAsDuration().toMillis()));
	}

	private JdbcPersistenceBuilder applyDoNotSaveProperties(final JdbcPersistenceBuilder pb) {
		return apply("doNotSaveProperties",pb,p->{
			String[] parts = p.getValueAsString().split(",");
			JdbcPersistenceBuilder pbf = (JdbcPersistenceBuilder) pb.clone();
			for (String part : parts) {
				pbf = pbf.doNotSaveCartProperties(part.trim());
			}
			return pbf;
		});
	}

	private RedisPersistenceBuilder applyDoNotSaveProperties(final RedisPersistenceBuilder pb) {
		return apply("doNotSaveProperties",pb,p->{
			String[] parts = p.getValueAsString().split(",");
			RedisPersistenceBuilder pbf = pb;
			for (String part : parts) {
				pbf = pbf.doNotSaveCartProperties(part.trim());
			}
			return pbf;
		});
	}

	private JdbcPersistenceBuilder applyMaxBatchSize(final JdbcPersistenceBuilder pb) {
		return apply("maxBatchSize",pb,p->pb.maxBatchSize(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyMaxBatchSize(final RedisPersistenceBuilder pb) {
		return apply("maxBatchSize",pb,p->pb.maxArchiveBatchSize(p.getValueAsInteger()));
	}

	private JdbcPersistenceBuilder applyEncryptionSecret(final JdbcPersistenceBuilder pb) {
		return apply("encryptionSecret",pb,p->pb.encryptionSecret(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyEncryptionSecret(final RedisPersistenceBuilder pb) {
		return apply("encryptionSecret",pb,p->pb.encryptionSecret(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyEncryptionKeyLength(final JdbcPersistenceBuilder pb) {
		return apply("encryptionKeyLength",pb,p->pb.encryptionKeyLength(p.getValueAsInteger()));
	}

	private RedisPersistenceBuilder applyEncryptionKeyLength(final RedisPersistenceBuilder pb) {
		return apply("encryptionKeyLength",pb,p->pb.encryptionKeyLength(p.getValueAsInteger()));
	}

	private JdbcPersistenceBuilder applyEncryptionTransformation(final JdbcPersistenceBuilder pb) {
		return apply("encryptionTransformation",pb,p->pb.encryptionTransformation(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyEncryptionTransformation(final RedisPersistenceBuilder pb) {
		return apply("encryptionTransformation",pb,p->pb.encryptionTransformation(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyEncryptionAlgorithm(final JdbcPersistenceBuilder pb) {
		return apply("encryptionAlgorithm",pb,p->pb.encryptionAlgorithm(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyEncryptionAlgorithm(final RedisPersistenceBuilder pb) {
		return apply("encryptionAlgorithm",pb,p->pb.encryptionAlgorithm(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyPort(final JdbcPersistenceBuilder pb) {
		return apply("port",pb,p->pb.port(p.getValueAsInteger()));
	}

	private JdbcPersistenceBuilder applyHost(final JdbcPersistenceBuilder pb) {
		return apply("host",pb,p->pb.host(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyCompletedLogTable(final JdbcPersistenceBuilder pb) {
		return apply("completedLogTable",pb,p->pb.completedLogTable(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyPassword(final JdbcPersistenceBuilder pb) {
		return apply("password",pb,p->pb.password(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyPassword(final RedisPersistenceBuilder pb) {
		return apply("password",pb,p->pb.password(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyUsername(final JdbcPersistenceBuilder pb) {
		return apply("username",pb,p->pb.user(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyUsername(final RedisPersistenceBuilder pb) {
		return apply("username",pb,p->pb.user(p.getValueAsString()));
	}

	private JdbcPersistenceBuilder applyRestoreOrder(final JdbcPersistenceBuilder pb) {
		return apply("restoreOrder",pb,p->pb.restoreOrder(RestoreOrder.valueOf(p.getValueAsString())));
	}

	private RedisPersistenceBuilder applyRestoreOrder(final RedisPersistenceBuilder pb) {
		return apply("restoreOrder",pb,p->pb.restoreOrder(com.aegisql.conveyor.persistence.redis.RestoreOrder.valueOf(p.getValueAsString())));
	}

	private RedisPersistenceBuilder applyPriorityRestoreStrategy(final RedisPersistenceBuilder pb) {
		return apply("priorityRestoreStrategy",pb,p->pb.priorityRestoreStrategy(PriorityRestoreStrategy.valueOf(p.getValueAsString())));
	}

	private JdbcPersistenceBuilder applyAutoInit(final JdbcPersistenceBuilder pb) {
		return apply("autoInit",pb,p->pb.autoInit(p.getValueAsBoolean()));
	}

	private RedisPersistenceBuilder applyAutoInit(final RedisPersistenceBuilder pb) {
		return apply("autoInit",pb,p->pb.autoInit(p.getValueAsBoolean()));
	}

	private JdbcPersistenceBuilder applyDriverClass(final JdbcPersistenceBuilder pb) {
		return apply("driverClass",pb,p->{
			ConnectionFactory cf = ConnectionFactory.driverManagerFactoryInstance();
			cf.setDriverClassName(p.getValueAsString());
			return pb.connectionFactory(cf);
		});
	}

	private JdbcPersistenceBuilder applyDatabase(final JdbcPersistenceBuilder pb) {
		return apply("database",pb,p->pb.database(p.getValueAsString()));
	}

	private RedisPersistenceBuilder applyDatabase(final RedisPersistenceBuilder pb) {
		return apply("database",pb,p->pb.database(p.getValueAsInteger()));
	}

	private JdbcPersistenceBuilder applyDbPath(final JdbcPersistenceBuilder pb) {
		return apply("db.path", pb, p -> {
			File dbRoot = new File(p.getValueAsString());
			if (!dbRoot.exists() && !dbRoot.mkdirs()) {
				throw new ConveyorConfigurationException("Failed creating db.path directory " + dbRoot.getPath());
			}
			String databaseName = resolveConfiguredDatabaseName();
			if (databaseName == null || databaseName.isBlank()) {
				return pb;
			}
			File databaseFile = new File(databaseName);
			if (databaseFile.isAbsolute()) {
				return pb.database(databaseFile.getPath());
			}
			File resolvedDatabaseFile = new File(dbRoot, databaseName);
			File parent = resolvedDatabaseFile.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				throw new ConveyorConfigurationException("Failed creating database parent directory " + parent.getPath());
			}
			return pb.database(resolvedDatabaseFile.getPath());
		});
	}

	private String resolveConfiguredDatabaseName() {
		LinkedList<PersistenceProperty> databaseProperties = properties.get("database");
		if (databaseProperties != null && !databaseProperties.isEmpty()) {
			return databaseProperties.getLast().getValueAsString();
		}
		return getSchema();
	}

	private String resolveDbRelativePath(String path) {
		if (path == null || path.isBlank()) {
			return path;
		}
		File configuredPath = new File(path);
		if (configuredPath.isAbsolute()) {
			return configuredPath.getPath();
		}
		LinkedList<PersistenceProperty> dbPathProperties = properties.get("db.path");
		if (dbPathProperties == null || dbPathProperties.isEmpty()) {
			return path;
		}
		File resolvedPath = new File(dbPathProperties.getLast().getValueAsString(), path);
		if (!resolvedPath.exists() && !resolvedPath.mkdirs()) {
			throw new ConveyorConfigurationException("Failed creating archive path " + resolvedPath.getPath());
		}
		return resolvedPath.getPath();
	}

	private JdbcPersistenceBuilder apply(String key, JdbcPersistenceBuilder pb, Function<PersistenceProperty,JdbcPersistenceBuilder> pbf) {
		LinkedList<PersistenceProperty> persistenceProperties = properties.get(key);
		if(persistenceProperties != null && persistenceProperties.size() > 0) {
			LOG.debug("Applying persistence property {}", key);
			for(PersistenceProperty pp:persistenceProperties) {
				pb = pbf.apply(pp);
			}
		}
		return pb;
	}

	private RedisPersistenceBuilder apply(String key, RedisPersistenceBuilder pb, Function<PersistenceProperty,RedisPersistenceBuilder> pbf) {
		LinkedList<PersistenceProperty> persistenceProperties = properties.get(key);
		if(persistenceProperties != null && persistenceProperties.size() > 0) {
			LOG.debug("Applying persistence property {}", key);
			for(PersistenceProperty pp:persistenceProperties) {
				pb = pbf.apply(pp);
			}
		}
		return pb;
	}


}
