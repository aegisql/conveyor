package com.aegisql.conveyor.loaders;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class StaticPartLoaderTest {

	@Test
	public void test() {
		StaticPartLoader spl0 = new StaticPartLoader<>(l->{
			assertNotNull(l);
			assertTrue(l.create);
			assertEquals("L", l.label);
			assertEquals("V", l.staticPartValue);
			assertEquals(1, l.priority);
			return new CompletableFuture();
		});
		
		assertTrue(spl0.create);
		
		StaticPartLoader spl1 = spl0.label("L").priority(1);
		assertNotNull(spl1);
		assertTrue(spl1.create);
		assertEquals("L", spl1.label);

		StaticPartLoader spl2 = spl1.delete();
		assertNotNull(spl2);
		assertFalse(spl2.create);
		assertEquals("L", spl2.label);

		StaticPartLoader spl3 = spl2.value(1);
		assertNotNull(spl3);
		assertTrue(spl3.create);
		assertEquals("L", spl3.label);

		StaticPartLoader spl4 = spl3.value("V");
		assertNotNull(spl4);
		assertTrue(spl4.create);
		assertEquals("L", spl4.label);
		assertEquals("V", spl4.staticPartValue);
		
		assertNotNull(spl4.place());

		StaticPartLoader spl5 = spl1.addProperty("p1", "test1")
				.addProperty("p2", "test2")
				.addProperties(new HashMap<String, Object>() {{
					put("p3", "test3");
				}});
		Map allProperties = spl5.getAllProperties();
		assertNotNull(allProperties);
		assertEquals("test2",spl5.getProperty("p2",String.class));
		assertEquals(3,allProperties.size());
		System.out.println(spl1);
		
	}

}
