package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.persistence.converters.BigDecimalToBytesConverter;
import com.aegisql.conveyor.persistence.converters.BigIntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.BooleanToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ByteToBytesConverter;
import com.aegisql.conveyor.persistence.converters.CharToBytesConverter;
import com.aegisql.conveyor.persistence.converters.DateToBytesConverter;
import com.aegisql.conveyor.persistence.converters.DoubleToBytesConverter;
import com.aegisql.conveyor.persistence.converters.FloatToBytesConverter;
import com.aegisql.conveyor.persistence.converters.IntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.LongToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ObjectToJsonBytesConverter;
import com.aegisql.conveyor.persistence.converters.ObjectToJsonStringConverter;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ShortToBytesConverter;
import com.aegisql.conveyor.persistence.converters.StringToBytesConverter;
import com.aegisql.conveyor.persistence.converters.UuidToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntPrimToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.IntegersToBytesConverter;
import com.aegisql.conveyor.persistence.converters.arrays.StringsToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;
import com.aegisql.conveyor.persistence.converters.collections.MapToByteArrayConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlDateToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimeToBytesConverter;
import com.aegisql.conveyor.persistence.converters.sql.SqlTimestampToBytesConverter;
import com.aegisql.conveyor.persistence.core.harness.Trio;

public class ConvertersTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	public void testByte() {
		ByteToBytesConverter ic = new ByteToBytesConverter();
		byte[] b = ic.toPersistence((byte) 100);
		assertNotNull(b);
		assertEquals(1, b.length);
		byte x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testChar() {
		CharToBytesConverter ic = new CharToBytesConverter();
		byte[] b = ic.toPersistence('A');
		assertNotNull(b);
		assertEquals(2, b.length);
		char x = ic.fromPersistence(b);
		assertEquals('A', x);
	}

	@Test
	public void testShort() {
		ShortToBytesConverter ic = new ShortToBytesConverter();
		byte[] b = ic.toPersistence((short) 5);
		assertNotNull(b);
		assertEquals(2, b.length);
		short x = ic.fromPersistence(b);
		assertEquals(5, x);
	}

	@Test
	public void testInteger() {
		IntegerToBytesConverter ic = new IntegerToBytesConverter();
		byte[] b = ic.toPersistence(100);
		assertNotNull(b);
		assertEquals(4, b.length);
		int x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testFloat() {
		FloatToBytesConverter ic = new FloatToBytesConverter();
		byte[] b = ic.toPersistence(3.14f);
		assertNotNull(b);
		assertEquals(4, b.length);
		float x = ic.fromPersistence(b);
		assertEquals(3.14, x, 0.00001);
	}

	@Test
	public void testLong() {
		LongToBytesConverter ic = new LongToBytesConverter();
		byte[] b = ic.toPersistence(100L);
		assertNotNull(b);
		assertEquals(8, b.length);
		long x = ic.fromPersistence(b);
		assertEquals(100, x);
	}

	@Test
	public void testDate() {
		DateToBytesConverter ic = new DateToBytesConverter();
		Date now = new Date();
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		Date x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlDate() {
		SqlDateToBytesConverter ic = new SqlDateToBytesConverter();
		java.sql.Date now = java.sql.Date.valueOf("2017-09-01");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Date x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlTime() {
		SqlTimeToBytesConverter ic = new SqlTimeToBytesConverter();
		java.sql.Time now = java.sql.Time.valueOf("12:00:00");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Time x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testSqlTimestamp() {
		SqlTimestampToBytesConverter ic = new SqlTimestampToBytesConverter();
		java.sql.Timestamp now = java.sql.Timestamp.valueOf("2017-01-01 12:00:00");
		byte[] b = ic.toPersistence(now);
		assertNotNull(b);
		assertEquals(8, b.length);
		java.sql.Timestamp x = ic.fromPersistence(b);
		assertEquals(now, x);
	}

	@Test
	public void testDouble() {
		DoubleToBytesConverter ic = new DoubleToBytesConverter();
		byte[] b = ic.toPersistence(3.14);
		assertNotNull(b);
		assertEquals(8, b.length);
		double x = ic.fromPersistence(b);
		assertEquals(3.14, x, 0.00001);
	}

	@Test
	public void testString() {
		StringToBytesConverter ic = new StringToBytesConverter();
		byte[] b = ic.toPersistence("test");
		assertNotNull(b);
		assertEquals("test".getBytes().length, b.length);
		String x = ic.fromPersistence(b);
		assertEquals("test", x);
	}

	@Test
	public void testSerializedString() {
		SerializableToBytesConverter<String> ic = new SerializableToBytesConverter<>();
		byte[] b = ic.toPersistence("test");
		System.out.println(new String(b));
		assertNotNull(b);
		String x = ic.fromPersistence(b);
		assertEquals("test", x);
	}

	@Test
	public void testSerializedLong() {
		SerializableToBytesConverter<Long> ic = new SerializableToBytesConverter<>();
		byte[] b = ic.toPersistence(100L);
		System.out.println(new String(b));
		assertNotNull(b);
		long x = ic.fromPersistence(b);
		assertEquals(100L, x);
	}

	@Test
	public void testBigInt() {
		BigIntegerToBytesConverter ic = new BigIntegerToBytesConverter();
		byte[] b = ic.toPersistence(new BigInteger("12345678900987654321"));
		assertNotNull(b);
		assertEquals(9, b.length);
		BigInteger x = ic.fromPersistence(b);
		assertEquals(new BigInteger("12345678900987654321"), x);
	}

	@Test
	public void testBigDec() {
		BigDecimalToBytesConverter ic = new BigDecimalToBytesConverter();
		byte[] b = ic.toPersistence(new BigDecimal("12345678900987654321"));
		assertNotNull(b);
		assertEquals(9, b.length);
		BigDecimal x = ic.fromPersistence(b);
		assertEquals(new BigDecimal("12345678900987654321"), x);
	}

	@Test
	public void testUuid() {
		UUID u1 = new UUID(Long.MAX_VALUE,Long.MAX_VALUE);
		UuidToBytesConverter uc = new UuidToBytesConverter();
		byte[] b = uc.toPersistence(u1);
		assertNotNull(b);
		assertEquals(16, b.length);
		UUID x = uc.fromPersistence(b);
		assertEquals(u1, x);
	}

	
	@Test
	public void testBoolean1() {
		BooleanToBytesConverter ic = new BooleanToBytesConverter();
		byte[] b = ic.toPersistence(true);
		assertNotNull(b);
		assertEquals(1, b.length);
		boolean x = ic.fromPersistence(b);
		assertTrue(x);
	}

	@Test
	public void testBoolean2() {
		BooleanToBytesConverter ic = new BooleanToBytesConverter();
		byte[] b = ic.toPersistence(false);
		assertNotNull(b);
		assertEquals(1, b.length);
		boolean x = ic.fromPersistence(b);
		assertFalse(x);
	}

	@Test
	public void testIntegerArray() {
		Integer[] ia = new Integer[]{1,2,3};
		System.out.println(ia.getClass().getCanonicalName());
		
		IntegersToBytesConverter bc = new IntegersToBytesConverter();
		byte[] b = bc.toPersistence(ia);
		assertNotNull(b);
		assertEquals(12, b.length);
		Integer[] ia2 = bc.fromPersistence(b);
		assertNotNull(ia2);
		assertEquals(3, ia2.length);
		assertEquals(ia[0], ia2[0]);
		assertEquals(ia[1], ia2[1]);
		assertEquals(ia[2], ia2[2]);

	}

	
	@Test
	public void testIntArray() {
		int[] ia = new int[]{1,2,3};
		System.out.println(ia.getClass().getCanonicalName());
		
		IntPrimToBytesConverter bc = new IntPrimToBytesConverter();
		byte[] b = bc.toPersistence(ia);
		assertNotNull(b);
		assertEquals(12, b.length);
		int[] ia2 = bc.fromPersistence(b);
		assertNotNull(ia2);
		assertEquals(3, ia2.length);
		assertEquals(ia[0], ia2[0]);
		assertEquals(ia[1], ia2[1]);
		assertEquals(ia[2], ia2[2]);

	}
	
	@Test
	public void typeTest() {
		Class<IntPrimToBytesConverter> cc = IntPrimToBytesConverter.class;
		
		Type[] tt = cc.getGenericInterfaces();
		System.out.println(tt[0]);
		ParameterizedType t = (ParameterizedType) tt[0];
		Type[] at = t.getActualTypeArguments();
		System.out.println(((Class)at[0]).getCanonicalName());
		
		
	}
	
	@Test
	public void collectionConverterIntTest() {
		CollectionToByteArrayConverter<Integer> cc = new CollectionToByteArrayConverter<Integer>(ArrayList::new, new IntegerToBytesConverter()){
			@Override
			public String conversionHint() {
				return "ArrayList<Integer>[]:byte[]";
			}
		};
		
		ArrayList<Integer> l = new ArrayList<>();
		l.add(100);
		l.add(200);
		l.add(null);
		l.add(400);
		
		byte[] b = cc.toPersistence(l);
		assertNotNull(b);
		Collection<Integer> l2 = cc.fromPersistence(b);
		assertNotNull(l2);
		System.out.println(l2);
		System.out.println("size="+b.length);
	}

	private String bigString(int size) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < size; i++) {
			sb.append('a');
		}
		
		return sb.toString();
	}
	
	@Test
	public void collectionConverterStringTest() {
		CollectionToByteArrayConverter<String> cc = new CollectionToByteArrayConverter<String>(ArrayList::new, new StringToBytesConverter()){
			@Override
			public String conversionHint() {
				return "";
			}			
		};
		
		ArrayList<String> l = new ArrayList<>();
		l.add("one");
		l.add("two");
		l.add("three");
		l.add("four");
		
		byte[] b = cc.toPersistence(l);
		assertNotNull(b);
		Collection<String> l2 = cc.fromPersistence(b);
		assertNotNull(l2);
		System.out.println(l2);
		System.out.println("size="+b.length);
	}

	@Test
	public void arrayConverterStringTest() {
		StringsToBytesConverter cc = new StringsToBytesConverter();
		
		String[] l = new String[]{
		"one",
		"two",
		"three",
		"four"
		};
		
		byte[] b = cc.toPersistence(l);
		assertNotNull(b);
		String[] l2 = cc.fromPersistence(b);
		assertNotNull(l2);
		assertEquals(4,l2.length);
		assertEquals("two",l2[1]);
		System.out.println(l2);
		System.out.println("size="+b.length);
	}

	
	@Test
	public void collectionConverterEmptyStringTest() {
		CollectionToByteArrayConverter<String> cc = new CollectionToByteArrayConverter<String>(ArrayList::new, new StringToBytesConverter()){
			@Override
			public String conversionHint() {
				return "";
			}			
		};
		
		ArrayList<String> l = new ArrayList<>();
		byte[] b = cc.toPersistence(l);
		assertNotNull(b);
		assertEquals(0, b.length);
		Collection<String> l2 = cc.fromPersistence(b);
		assertNotNull(l2);
		assertEquals(0, l2.size());
		System.out.println(l2);
		System.out.println("size="+b.length);
	}

	
	@Test
	public void collectionConverterBigStringTest() {
		CollectionToByteArrayConverter<String> cc = new CollectionToByteArrayConverter<String>(ArrayList::new, new StringToBytesConverter()){
			@Override
			public String conversionHint() {
				return "";
			}			
		};
		
		ArrayList<String> l = new ArrayList<>();
		l.add(bigString(100));
		l.add(bigString(1000));
		l.add(bigString(100_000));
		l.add(bigString(17_000_000));
		
		byte[] b = cc.toPersistence(l);
		assertNotNull(b);
		Collection<String> l2 = cc.fromPersistence(b);
		assertNotNull(l2);
		String[] s = l2.toArray(new String[4]);
		assertEquals(4, s.length);
		assertEquals(100, s[0].length());
		assertEquals(1000, s[1].length());
		assertEquals(100_000, s[2].length());
		assertEquals(17_000_000, s[3].length());
		System.out.println("size="+b.length);
	}

	@Test
	public void basicMapTest() {
		MapToByteArrayConverter<Integer, String> mc = new MapToByteArrayConverter<Integer,String>(HashMap::new, new IntegerToBytesConverter(), new StringToBytesConverter()){
			@Override
			public String conversionHint() {
				return "";
			}
		};
		
		Map<Integer,String> m = new HashMap<>();
		
		m.put(1, "one");
		m.put(2, "two");
		m.put(3, "three");
		
		byte[] b = mc.toPersistence(m);
		assertNotNull(b);
		
		Map<Integer,String> m2 = mc.fromPersistence(b);
		System.out.println(m2);
		assertEquals(3, m2.size());
		assertTrue(m2.containsKey(1));
		assertTrue(m2.containsKey(2));
		assertTrue(m2.containsKey(3));
		assertEquals("one", m2.get(1));
		assertEquals("two", m2.get(2));
		assertEquals("three", m2.get(3));
		
	}

	@Test
	public void basicMapTest2() {
		MapToByteArrayConverter<String,Integer> mc = new MapToByteArrayConverter<String,Integer>(HashMap::new, new StringToBytesConverter(), new IntegerToBytesConverter()){
			@Override
			public String conversionHint() {
				return "";
			}
		};
		
		Map<String,Integer> m = new HashMap<>();
		
		m.put("one",1);
		m.put("two",2);
		m.put("three",3);
		
		byte[] b = mc.toPersistence(m);
		assertNotNull(b);
		
		Map<String,Integer> m2 = mc.fromPersistence(b);
		System.out.println("size="+b.length);
		System.out.println(m2);
		assertEquals(3, m2.size());
		assertTrue(m2.containsKey("one"));
		assertTrue(m2.containsKey("two"));
		assertTrue(m2.containsKey("three"));
		assertEquals(new Integer(1), m2.get("one"));
		assertEquals(new Integer(2), m2.get("two"));
		assertEquals(new Integer(3), m2.get("three"));
		
	}

	@Test
	public void jsonTest() {
		Trio t1 = new Trio("one", "two", 100);
		ObjectToJsonBytesConverter<Trio> oc = new ObjectToJsonBytesConverter<Trio>(Trio.class);
		byte[] b = oc.toPersistence(t1);
		System.out.println(oc.conversionHint());
		System.out.println(new String(b));
		Trio t2 = oc.fromPersistence(b);
		assertEquals(t1, t2);
	}
	
	@Test
	public void testJsonMap() {
		Map<String,Object> m = new HashMap<>();
		m.put("one", "one");
		m.put("two", 2);
		ObjectToJsonStringConverter<Map> oc = new ObjectToJsonStringConverter<Map>(Map.class);
		String s = oc.toPersistence(m);
		System.out.println(oc.conversionHint());
		System.out.println(s);
		Map m2 = oc.fromPersistence(s);
		assertEquals(m, m2);

	}
}
