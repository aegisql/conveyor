package com.aegisql.conveyor.utils.map;

import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.loaders.PartLoader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MapBuillderTest {

	@Test
	public void testMapBuilder() throws InterruptedException, ExecutionException {
		MapConveyor<Integer, String, String> mc = new MapConveyor<>();

		mc.setBuilderSupplier(MapBuilder::new);

		mc.resultConsumer(LogResult.stdOut(mc)).set();
		mc.scrapConsumer(bin->fail("Failed "+bin)).set();

		PartLoader<Integer, String> pl = mc.part().id(1);
		pl.label("FIRST").value("ONE").place();
		pl.label("SECOND").value("TWO").place();
		pl.label("THIRD").value("THREE").place();
		CompletableFuture<Boolean> last = (CompletableFuture<Boolean>) pl.label(null).value(null).place();
		assertTrue(last.get());
		Thread.sleep(10);
	}

	@Test
	public void constructorTests() {
		MapBuilder mb1 = new MapBuilder(1000);
		MapBuilder mb2 = new MapBuilder(1000, TimeUnit.SECONDS);
		MapBuilder mb3 = new MapBuilder(new HashMap(),1000);
		MapBuilder mb4 = new MapBuilder(new HashMap(),1000, TimeUnit.SECONDS);
		MapBuilder mb5 = new MapBuilder(new HashMap());
	}
}
