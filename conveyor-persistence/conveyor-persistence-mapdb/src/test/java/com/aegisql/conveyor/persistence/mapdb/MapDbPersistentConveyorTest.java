package com.aegisql.conveyor.persistence.mapdb;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.mapdb.MapDbPersistentConveyor;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

public class MapDbPersistentConveyorTest {
	
	static String DB_PATH= "./src/test/resources/MapDbPersistentConveyorTest";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		FileUtils.deleteDirectory(new File(DB_PATH));
		new File(DB_PATH).mkdirs();
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

	static class StringScalar extends ScalarConvertingBuilder<String, String>{
		@Override
		public String get() {
			return super.scalar;
		}
	}
	
	@Test
	public void test() throws InterruptedException {
		Conveyor<String, String, String> ac = new ScalarConvertingConveyor<>();
		ac.setBuilderSupplier(StringScalar::new);
		ac.resultConsumer().first(bin->System.out.println(bin)).set();
		MapDbPersistentConveyor<String, String, String> pc = new MapDbPersistentConveyor<>("test", DB_PATH, ac);
		
		Cart<String,String,String> c1 = new ShoppingCart<>("k1", "v1", "l1");
		pc.place(c1);

		Cart<String,String,String> c2 = new ShoppingCart<>("k2", "v2", "l2");
		pc.place(c2);

		Thread.sleep(1000);
		
	}

}
