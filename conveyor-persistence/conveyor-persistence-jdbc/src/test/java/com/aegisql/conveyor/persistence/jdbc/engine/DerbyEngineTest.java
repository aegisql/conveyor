package com.aegisql.conveyor.persistence.jdbc.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;

public class DerbyEngineTest {

	private static String SCHEMA = "test_engine";
	private static String PARTS = "PART";
	private static String LOGS = "COMPLETED_LOG";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Tester.removeDirectory(SCHEMA);
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
	public void testDerby() throws IOException {
		
		LinkedHashMap<String, String> order = new LinkedHashMap<>();
		order.put("PRIORITY", "DESC");
		order.put("ID", "ASC");
		
		GenericEngine<Integer> de = new DerbyEngine<>(Integer.class);
		de.setAdditionalFields(Arrays.asList(new Field(UUID.class,"ADDON")));
		de.setSchema(SCHEMA);
		assertTrue(de.databaseExists(SCHEMA));
		assertFalse(de.schemaExists(SCHEMA));
		de.createSchema(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);
		de.setSortingOrder(order);
		de.createUniqPartTableIndex(PARTS, Arrays.asList(EngineDepo.CART_KEY,EngineDepo.CART_LABEL));
		de.createUniqPartTableIndex(PARTS, Arrays.asList("ADDON"));
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
				, 0
				, Arrays.asList(UUID.randomUUID().toString()));
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
				, Arrays.asList(UUID.randomUUID().toString()));
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
				, Arrays.asList(UUID.randomUUID().toString()));
		assertEquals(3, de.getNumberOfParts());
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
		assertEquals(Long.valueOf(2),unfinished.get(0));
		assertEquals(Long.valueOf(1),unfinished.get(1));

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
				, Arrays.asList(UUID.randomUUID().toString()));
			fail("Must not be saved! Unique constraint");
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		de.close();
	}

	@Test
	public void testDerbyInMemory() throws IOException {
		
		LinkedHashMap<String, String> order = new LinkedHashMap<>();
		order.put("PRIORITY", "DESC");
		order.put("ID", "ASC");
		
		GenericEngine<Integer> de = new DerbyMemoryEngine<>(Integer.class);
		de.setAdditionalFields(Arrays.asList(new Field(UUID.class,"ADDON")));
		de.setSchema(SCHEMA);
		assertTrue(de.databaseExists(SCHEMA));
		assertFalse(de.schemaExists(SCHEMA));
		de.createSchema(SCHEMA);
		de.createPartTable(PARTS);
		de.createPartTableIndex(PARTS);
		de.createCompletedLogTable(LOGS);
		de.setSortingOrder(order);
		de.createUniqPartTableIndex(PARTS, Arrays.asList(EngineDepo.CART_KEY,EngineDepo.CART_LABEL));
		de.createUniqPartTableIndex(PARTS, Arrays.asList("ADDON"));
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
				, 0
				, Arrays.asList(UUID.randomUUID().toString()));
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
				, Arrays.asList(UUID.randomUUID().toString()));
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
				, Arrays.asList(UUID.randomUUID().toString()));
		assertEquals(3, de.getNumberOfParts());
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
		assertEquals(Long.valueOf(2),unfinished.get(0));
		assertEquals(Long.valueOf(1),unfinished.get(1));

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
				, Arrays.asList(UUID.randomUUID().toString()));
			fail("Must not be saved! Unique constraint");
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		de.close();
	}

}
