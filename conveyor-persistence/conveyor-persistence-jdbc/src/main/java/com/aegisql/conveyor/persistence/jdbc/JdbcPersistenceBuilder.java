package com.aegisql.conveyor.persistence.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.DoNothingArchiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.ConnectionSupplier;
import com.aegisql.conveyor.persistence.jdbc.builders.DynamicPersistenceSql;
import com.aegisql.conveyor.persistence.jdbc.builders.GenericInitPersistenceSql;
import com.aegisql.conveyor.persistence.jdbc.builders.GenericPersistenceSql;
import com.aegisql.conveyor.persistence.jdbc.converters.StringConverter;
import com.aegisql.id_builder.IdSource;
import com.aegisql.id_builder.impl.TimeHostIdGenerator;

public class JdbcPersistenceBuilder<K> {

	private final Class<K> keyClass;
	
	private final String driverClass;

	private final boolean autoInit;
	
	private final String connectionUrlTemplate;
	private final String connectionUrl;
	
	private final Supplier<DynamicPersistenceSql> dynamicPersistenceSqlSupplier;
	
	private GenericInitPersistenceSql <K> initSql;
	
	private String host;
	private int port = 0;

	private final String database;
	private final String schema;
	private final String partTable;
	private final String completedLogTable;

	private Connection conn;
	private final LongSupplier idSupplier;

	private Archiver<K> archiver = new DoNothingArchiver<>();
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
	private ConverterAdviser<?> converterAdviser;
	private int maxBatchSize;
	private long maxBatchTime;
	private String info;
	private Set<String> nonPersistentProperties;
	private int minCompactSize;

	private final Properties properties;

	public JdbcPersistenceBuilder(Class<K> keyClass) {
		this.keyClass = keyClass;
		this.driverClass = null;
		this.connectionUrl = null;
		this.connectionUrlTemplate = null;
		this.autoInit = false;
		this.dynamicPersistenceSqlSupplier = null;
		final IdSource idGen = TimeHostIdGenerator.idGenerator_10x8(System.currentTimeMillis()/1000);
		this.idSupplier = idGen::getId;
		this.properties = new Properties();
		this.database = null;
		this.schema = "conveyor_db";
		this.partTable = "PART";
		this.completedLogTable = "COMPLETED_LOG";
	}
	
	private JdbcPersistenceBuilder(
			Class<K> keyClass
			, String driverClass
			, boolean autoInit
			, String connectionUrlTemplate
			, String connectionUrl
			, Supplier<DynamicPersistenceSql> dynamicPersistenceSqlSupplier
			, LongSupplier idSupplier
			, Properties properties
			, String database
			, String schema
			, String partTable
			, String completedLogTable
			) {
		this.keyClass = keyClass;
		this.driverClass = driverClass;
		this.autoInit = autoInit;
		this.connectionUrlTemplate = connectionUrlTemplate;
		this.connectionUrl = connectionUrl;
		this.dynamicPersistenceSqlSupplier = dynamicPersistenceSqlSupplier;
		this.idSupplier = idSupplier;
		this.properties = properties;
		this.database = database;
		this.schema = schema;
		this.partTable = partTable;
		this.completedLogTable = completedLogTable;
	}
	
	public JdbcPersistenceBuilder<K> driverClass(String dc) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				dc,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> autoInit(boolean init) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				init,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> connectionUrlTemplate(String template) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				template,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> connectionUrl(String url) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				url,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> idSupplier(LongSupplier idSup) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSup,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> addProperty(String key, String value) {
		Properties newProperties = new Properties(this.properties);
		newProperties.put(key, value);
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				newProperties,
				database,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> database(String db) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				db,
				schema,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> schema(String sch) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				sch,
				partTable,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> partTable(String tbl) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				tbl,
				completedLogTable
				);
	}

	public JdbcPersistenceBuilder<K> completedLogTable(String tbl) {
		return new JdbcPersistenceBuilder<>(
				keyClass,
				driverClass,
				autoInit,
				connectionUrlTemplate,
				connectionUrl,
				dynamicPersistenceSqlSupplier,
				idSupplier,
				new Properties(this.properties),
				database,
				schema,
				partTable,
				tbl
				);
	}
	
	private ConverterAdviser getConverterAdviser() {
		if(converterAdviser == null) {
			converterAdviser = new ConverterAdviser<>();
		}
		return converterAdviser;
	}

	public JdbcPersistence<K> build() {
		try {
			Class.forName(driverClass);
			Supplier<DynamicPersistenceSql> dps;
			if(dynamicPersistenceSqlSupplier == null) {
				dps = ()->{
					return new GenericPersistenceSql(partTable, completedLogTable);
				};
			} else {
				dps = dynamicPersistenceSqlSupplier;
			}
			
			if(autoInit) {
				initPersistence();
			}
			
			return new JdbcPersistence(
					  new ConnectionSupplier(buildConnectionUrl(), properties)
					, idSupplier
					, dps.get()
					, archiver
					, labelConverter
					, getConverterAdviser()
					, maxBatchSize
					, maxBatchTime
					, info
					, nonPersistentProperties
					, minCompactSize
					);
		} catch (Exception e) {
			throw new PersistenceException("Failed creation of JDBC persistence", e);
		}
	}

	private void initPersistence() throws SQLException {
		if(initSql == null) {
			initSql = new GenericInitPersistenceSql<>(keyClass);
		}
		initSql.setDatabase(database);
		initSql.setSchema(schema);
		initSql.setPartTable(partTable);
		initSql.setCompletedLogTable(completedLogTable);
		Connection con = DriverManager.getConnection(buildConnectionUrl(), properties);
		if(initSql.createDatabaseSql() != null) {
			Statement st = con.createStatement();
			st.executeUpdate(initSql.createDatabaseSql());
			st.close();
			con.setCatalog(database);
		}
		if(initSql.createSchemaSql() != null) {
			Statement st = con.createStatement();
			st.executeUpdate(initSql.createSchemaSql());
			st.close();
			con.setCatalog(schema);
		}
		Statement st = con.createStatement();
		st.executeUpdate(initSql.createPartTableSql());
		st.executeUpdate(initSql.createPartTableIndexSql());
		st.executeUpdate(initSql.createCompletedLogTableSql());
	}

	private String buildConnectionUrl() {
		if(connectionUrl != null) {
			return connectionUrl;
		} else {
			Objects.requireNonNull(connectionUrlTemplate, "Either connectionUrl or connectionUrlTemplate must be specified");
			return connectionUrlTemplate
				.replace("{schema}", schema)
				.replace("{autoInit}", ""+autoInit)
				;
		}
	}

	public static <K> JdbcPersistenceBuilder<K> getBuilder(String persitenceType, Class<K> keyClass) {
		JdbcPersistenceBuilder<K> jpb = new JdbcPersistenceBuilder<>(keyClass);
		switch (persitenceType) {
		case "generic":
			break;
		case "derby-embedded":
			jpb = jpb
			.driverClass("org.apache.derby.jdbc.EmbeddedDriver")
			.autoInit(false)
			.connectionUrlTemplate("jdbc:derby:{schema};create={autoInit}")
			;
			break;
		case "mysql":
			jpb = jpb
			.driverClass("com.mysql.cj.jdbc.Driver")
			.autoInit(false)
			.connectionUrlTemplate("jdbc:mysql://{host}:{port}/")
			;
			break;
		default:
			throw new PersistenceException("Unknown persistence type "+persitenceType+". Use 'generic' to construct your own persistence builder");
		}
		return jpb;
	}
	
}
