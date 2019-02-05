package com.aegisql.conveyor.persistence.jdbc.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;

public class EnginesTest {

	private static String SCHEMA = "test_engine";
	private static String PARTS = "PART";
	private static String LOGS = "COMPLETED_LOG";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Tester.removeDirectory(SCHEMA);
		Tester.removeFile(SCHEMA+".db");
		Tester.removeLocalMysqlDatabase(SCHEMA);
		Tester.removeLocalPostgresDatabase(SCHEMA);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDerby() {
		GenericEngine<Integer> de = new DerbyEngine<>(Integer.class);
		de.setSchema(SCHEMA);
		assertTrue(de.databaseExists(SCHEMA));
		assertFalse(de.schemaExists(SCHEMA));
		de.createSchema(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);
		de.buildPartTableQueries(PARTS);
		de.buildCompletedLogTableQueries(LOGS);
		
		de.saveCart(1L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0);
		de.saveCart(2L
				, "STATIC_PART"
				, null
				, "STATIC_LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "static value".getBytes()
				, "{}"
				, "hint"
				, 0);
		assertEquals(2, de.getNumberOfParts());
		List<Long> ids = de.getAllPartIds(1);
		assertNotNull(ids);
		assertEquals(Long.valueOf(1),ids.get(0));

		de.saveCompletedBuildKey(100);
		
		List<Long> res = de.getParts(Arrays.asList(1L), rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(res);
		assertEquals(Long.valueOf(1),res.get(0));
		
		List<Long> unfinished = de.getUnfinishedParts(rs->{
			try {
				return rs.getLong(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		});
		assertNotNull(unfinished);
		assertEquals(Long.valueOf(1),unfinished.get(0));

		Tester.sleep(1500);
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
	}

	@Test
	public void testMysql() {
		GenericEngine<Integer> de = new MysqlEngine<>(Integer.class);
		de.setDatabase(SCHEMA);
		de.setUser("root");
		assertFalse(de.databaseExists(SCHEMA));
		assertTrue(de.schemaExists(SCHEMA));
		de.createDatabase(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);
		de.buildPartTableQueries(PARTS);
		de.buildCompletedLogTableQueries(LOGS);

		de.saveCart(1L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+100)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0);
		de.saveCart(2L
				, "STATIC_PART"
				, null
				, "STATIC_LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "static value".getBytes()
				, "{}"
				, "hint"
				, 0);

		assertEquals(2, de.getNumberOfParts());
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
		assertEquals(Long.valueOf(1),unfinished.get(0));

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

	}

	@Test
	public void testPostgres() {
		GenericEngine<Integer> de = new PostgresqlEngine<>(Integer.class);
		de.setDatabase(SCHEMA);
		de.setSchema(SCHEMA);
		de.setUser("postgres");
		de.setPassword("root");
		de.buildPartTableQueries(PARTS);
		de.buildCompletedLogTableQueries(LOGS);
		assertFalse(de.databaseExists(SCHEMA));
		de.createDatabase(SCHEMA);
		assertFalse(de.schemaExists(SCHEMA));
		de.createSchema(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);

		de.saveCart(1L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+100)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0);
		de.saveCart(2L
				, "STATIC_PART"
				, null
				, "STATIC_LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "static value".getBytes()
				, "{}"
				, "hint"
				, 0);

		assertEquals(2, de.getNumberOfParts());
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
		assertEquals(Long.valueOf(1),unfinished.get(0));

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

	}

	@Test
	public void testSqlite() {
		GenericEngine<Integer> de = new SqliteEngine<>(Integer.class);
		de.setDatabase(SCHEMA+".db");
		de.buildPartTableQueries(PARTS);
		de.buildCompletedLogTableQueries(LOGS);
		assertTrue(de.databaseExists(SCHEMA));
		assertTrue(de.schemaExists(SCHEMA));
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);

		de.saveCart(1L
				, PARTS
				, 1, "LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+100)
				, "test value".getBytes()
				, "{}"
				, "hint"
				, 0);
		de.saveCart(2L
				, "STATIC_PART"
				, null
				, "STATIC_LABEL"
				, new Timestamp(0)
				, new Timestamp(System.currentTimeMillis()+1000)
				, "static value".getBytes()
				, "{}"
				, "hint"
				, 0);

		assertEquals(2, de.getNumberOfParts());
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
		assertEquals(Long.valueOf(1),unfinished.get(0));

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

	}

}
