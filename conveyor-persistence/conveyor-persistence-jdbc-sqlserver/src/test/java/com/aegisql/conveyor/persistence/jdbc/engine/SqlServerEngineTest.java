package com.aegisql.conveyor.persistence.jdbc.engine;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.DriverManagerDataSource;
import com.aegisql.conveyor.persistence.jdbc.engine.sqlserver.SqlServerEngine;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SqlServerEngineTest {

	private static final String DATABASE = Tester.getTestClass();

	@BeforeAll
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
		assumeTrue(Tester.testSqlServerConnection());
		Tester.removeLocalSqlServerDatabase(DATABASE);
	}

	@AfterAll
	public static void tearDownAfterClass() {
	}

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testSqlServer() throws IOException {
		LinkedHashMap<String, String> order = new LinkedHashMap<>();
		order.put("PRIORITY", "DESC");
		order.put("ID", "ASC");
		ConnectionFactory<DriverManagerDataSource> cf = ConnectionFactory.driverManagerFactoryInstance();
		cf.setDatabase(DATABASE);
		cf.setUser(Tester.getSqlServerUser());
		cf.setPassword(Tester.getSqlServerPassword());
		cf.setHost(Tester.getSqlServerHost());
		cf.setPort(Tester.getSqlServerPort());
		GenericEngine<Integer> de = new SqlServerEngine<>(Integer.class, cf, false);
		de.setAdditionalFields(Arrays.asList(new Field<>(Long.class, "ADDON")));
		de.connectionFactory.setDatabase(DATABASE);
		assertFalse(de.databaseExists(DATABASE));
		de.createDatabaseIfNotExists(DATABASE);
		String parts = "PART";
		de.createPartTable(parts);
		de.createPartTableIndex(parts);
		String logs = "COMPLETED_LOG";
		de.createCompletedLogTable(logs);
		de.setSortingOrder(order);
		de.createUniqPartTableIndex(parts, Arrays.asList(EngineDepo.CART_KEY, EngineDepo.CART_LABEL));
		de.createUniqPartTableIndex(parts, Arrays.asList("ADDON"));
		de.buildPartTableQueries(parts);
		de.buildCompletedLogTableQueries(logs);

		de.saveCart(1L, parts, 1, "LABEL", new Timestamp(0), new Timestamp(System.currentTimeMillis() + 100),
				"test value".getBytes(), "{}", "hint", 0, Arrays.asList(1000L));
		de.saveCart(3L, parts, 2, "LABEL", new Timestamp(0), new Timestamp(System.currentTimeMillis() + 1000),
				"test value".getBytes(), "{}", "hint", 1, Arrays.asList(1001L));
		de.saveCart(2L, "STATIC_PART", null, "STATIC_LABEL", new Timestamp(0), new Timestamp(System.currentTimeMillis() + 1000),
				"static value".getBytes(), "{}", "hint", 0, Arrays.asList(1002L));

		assertEquals(3, de.getNumberOfParts());
		List<Long> ids = de.getAllPartIds(1);
		assertNotNull(ids);
		assertEquals(Long.valueOf(1), ids.get(0));

		de.saveCompletedBuildKey(100);

		List<Long> savedParts = de.getParts(Arrays.asList(1L), rs -> {
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				return null;
			}
		});
		assertNotNull(savedParts);
		assertEquals(Long.valueOf(1), savedParts.get(0));

		List<Long> unfinished = de.getUnfinishedParts(rs -> {
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				return null;
			}
		});
		assertNotNull(unfinished);
		assertEquals(Long.valueOf(2), unfinished.get(0));
		assertEquals(Long.valueOf(1), unfinished.get(1));

		Tester.sleep(2000);
		List<Long> expired = de.getExpiredParts(rs -> {
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				return null;
			}
		});
		assertNotNull(expired);
		assertEquals(Long.valueOf(1), expired.get(0));

		Set<Integer> completed = de.getAllCompletedKeys(rs -> {
			try {
				return rs.getInt(1);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		assertNotNull(completed);
		assertTrue(completed.contains(100));

		List<String> staticPart = de.getStaticParts(rs -> {
			try {
				return new String(rs.getBytes(2));
			} catch (SQLException e) {
				return null;
			}
		});
		assertNotNull(staticPart);
		assertEquals("static value", staticPart.get(0));

		try {
			de.saveCart(10L, parts, 1, "LABEL", new Timestamp(0), new Timestamp(System.currentTimeMillis() + 1000),
					"test value".getBytes(), "{}", "hint", 0, Arrays.asList(1003L));
			fail("Must not be saved! Unique constraint");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		de.close();
	}
}
