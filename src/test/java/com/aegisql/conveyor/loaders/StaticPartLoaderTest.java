package com.aegisql.conveyor.loaders;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class StaticPartLoaderTest {

	@Test
	public void test() {
		StaticPartLoader spl0 = new StaticPartLoader<>(l->{
			assertNotNull(l);
			assertTrue(l.create);
			assertEquals("L", l.label);
			assertEquals("V", l.staticPartValue);			
			return new CompletableFuture();
		});
		
		assertTrue(spl0.create);
		
		StaticPartLoader spl1 = spl0.label("L");
		assertNotNull(spl1);
		assertTrue(spl1.create);
		assertEquals("L", spl1.label);

		StaticPartLoader spl2 = spl1.delete();
		assertNotNull(spl2);
		assertFalse(spl2.create);
		assertEquals("L", spl2.label);

		StaticPartLoader spl3 = spl2.create();
		assertNotNull(spl3);
		assertTrue(spl3.create);
		assertEquals("L", spl3.label);

		StaticPartLoader spl4 = spl3.value("V");
		assertNotNull(spl4);
		assertTrue(spl4.create);
		assertEquals("L", spl4.label);
		assertEquals("V", spl4.staticPartValue);
		
		assertNotNull(spl4.place());
		
		
	}

}
