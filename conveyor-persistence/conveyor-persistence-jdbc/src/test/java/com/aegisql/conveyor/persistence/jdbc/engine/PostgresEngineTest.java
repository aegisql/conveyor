package com.aegisql.conveyor.persistence.jdbc.engine;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class PostgresEngineTest {

	private static final String SCHEMA = "test_engine";

	@BeforeClass
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
		Assume.assumeTrue(Tester.testPostgresConnection());
		Tester.removeLocalPostgresDatabase(SCHEMA);
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testPostgres() throws IOException {
		
		LinkedHashMap<String, String> order = new LinkedHashMap<>();
		order.put("PRIORITY", "DESC");
		order.put("ID", "ASC");

		GenericEngine<Integer> de = new PostgresqlEngine<>(Integer.class, ConnectionFactory.driverManagerFactoryInstance());
		de.setAdditionalFields(Arrays.asList(new Field(Long.class,"ADDON")));
		de.setDatabase(SCHEMA);
		de.setSchema(SCHEMA);
		de.setUser("postgres");
		de.setPassword("root");
		de.setSortingOrder(order);
		String PARTS = "PART";
		de.buildPartTableQueries(PARTS);
		String LOGS = "COMPLETED_LOG";
		de.buildCompletedLogTableQueries(LOGS);
		assertFalse(de.databaseExists(SCHEMA));
		de.createDatabase(SCHEMA);
		assertFalse(de.schemaExists(SCHEMA));
		de.createSchema(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);
		de.createUniqPartTableIndex(PARTS, Arrays.asList(EngineDepo.CART_KEY,EngineDepo.CART_LABEL));
		de.createUniqPartTableIndex(PARTS, Arrays.asList("ADDON"));
		de.deleteAllParts();
		de.saveCart(1L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+100)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0
				, Arrays.asList(1000L));
		de.saveCart(3L
				, PARTS
				, 2
				, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 1
				, Arrays.asList(1001L));
		de.saveCart(2L
				, "STATIC_PART"
				, null
				, "STATIC_LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "static value".getBytes()
				, "{}"
				, "hint"
				, 0
				, Arrays.asList(1002L));

		assertEquals(3, de.getNumberOfParts());
		List<Long> ids = de.getAllPartIds(1);
		assertNotNull(ids);
		assertEquals(Long.valueOf(1),ids.get(0));

		de.saveCompletedBuildKey(100);
		
		List<Long> parts = de.getParts(Arrays.asList(1L), rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(parts);
		assertEquals(Long.valueOf(1),parts.get(0));

		List<Long> unfinished = de.getUnfinishedParts(rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(unfinished);
		assertEquals(Long.valueOf(2),unfinished.get(0));
		assertEquals(Long.valueOf(1),unfinished.get(1));

		Tester.sleep(2000);
		List<Long> exp = de.getExpiredParts(rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(exp);
		assertEquals(Long.valueOf(1),exp.get(0));
		
		Set<Integer> completed = de.getAllCompletedKeys(rs->{
			try {
				return rs.getInt(1);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		assertNotNull(completed);
		assertTrue(completed.contains(100));
		
		List<String> staticPart = de.getStaticParts(rs->{
			try {
				return new String(rs.getBytes(2));
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(staticPart);
		assertEquals("static value",staticPart.get(0));
		
		try {
			de.saveCart(10L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0
				, Arrays.asList(1003L));
			fail("Must not be saved! Unique constraint");
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}

		de.close();

	}

}
