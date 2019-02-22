package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.ParallelConveyor;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import com.aegisql.conveyor.persistence.core.harness.ThreadPool;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilderExpireable;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyorExpireable;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
import com.aegisql.conveyor.persistence.core.harness.TrioPartExpireable;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;

public class MysqlPerfTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
			.user("root")
			.autoInit(true).setArchived();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Assume.assumeTrue(Tester.testMySqlConnection());
		Tester.removeLocalMysqlDatabase("perfConv");
		Tester.removeLocalMysqlDatabase("perfConvArchive");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			File dir = new File("./");
			
			Arrays.stream(dir.listFiles()).map(f->f.getName()).filter(f->(f.endsWith(".blog")||f.endsWith(".blog.zip"))).forEach(f->new File(f).delete());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	ThreadPool pool;

	int testSize = Tester.getPerfTestSize();

	int batchSize;
	
	double sleepTime = 0.01;
	int sleepNumber;

	@Before
	public void setUp() throws Exception {
		pool = new ThreadPool(3);
		batchSize = testSize / 20;
		sleepNumber = batchSize;
		System.out.println("--- MysqlPerfTest " + new Date());
	}

	@After
	public void tearDown() throws Exception {
		pool.shutdown();
	}

	Persistence<Integer> getPersitence(String table) {
		try {
			Thread.sleep(1000);
			
			return persistenceBuilder
				.database("perfConv")
				.partTable(table)
				.completedLogTable(table + "Completed")
				.labelConverter(TrioPart.class)
				.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS)
				.maxBatchSize(batchSize)
				.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceExp(String table) {
		try {
			Thread.sleep(1000);

			return persistenceBuilder
					.database("perfConv")
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPartExpireable.class)
					.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS)
					.maxBatchSize(batchSize)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceFile(String table) {
		try {
			Thread.sleep(1000);

			return persistenceBuilder
					.database("perfConv")
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPart.class)
					.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS)
					.maxBatchSize(batchSize)
					.archiver(BinaryLogConfiguration.builder()
							.path("./")
							.partTableName(table)
							.bucketSize(500)
							.maxFileSize("1MB")
							.zipFile(true)
							.build())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitencePersistence(String table) {
		try {
			Thread.sleep(1000);

			Persistence<Integer> archive = persistenceBuilder
					.database("perfConvArchive")
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPart.class)
					.maxBatchSize(batchSize)
					.build();

			return archive = persistenceBuilder
					.database("perfConv")
					.partTable(table)
					.completedLogTable(table + "Completed")
					.labelConverter(TrioPart.class)
					.maxBatchSize(batchSize)
					.archiver(archive)
					.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS)
					.maxBatchSize(batchSize)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	
	List<Integer> getIdList() {
		List<Integer> t1 = new ArrayList<>();
		for (int i = 1; i <= testSize; i++) {
			t1.add(i);
		}
		return t1;
	}

	List<Integer> getIdListShuffled() {
		List<Integer> t1 = new ArrayList<>();
		for (int i = 1; i <= testSize; i++) {
			t1.add(i);
		}
		Collections.shuffle(t1);
		return t1;
	}

	void sleep(int i, double frac) {
		if(i % sleepNumber !=  0) {
			return;
		}
		int msec = (int) frac;
		double nsec = frac - msec;
		try {
			Thread.sleep(msec, (int) (999999.0 * nsec));
		} catch (InterruptedException e) {
		}
	}

	void load(Conveyor pc, List<Integer> l1, List<Integer> l2, List<Integer> l3) {
		AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
		AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
		AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();

		CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
		CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
		CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();

		Runnable r1 = () -> {
			l1.forEach(key -> {
				fr1.set(pc.part().id(key).label(TrioPart.TEXT1).value("txt1_" + key).place());
				sleep(key,sleepTime);
			});
			f1.complete(1);
		};
		Runnable r2 = () -> {
			l2.forEach(key -> {
				fr2.set(pc.part().id(key).label(TrioPart.TEXT2).value("txt2_" + key).place());
				sleep(key,sleepTime);
			});
			f2.complete(1);
		};
		Runnable r3 = () -> {
			l3.forEach(key -> {
				fr3.set(pc.part().id(key).label(TrioPart.NUMBER).value(key).place());
				sleep(key,sleepTime);
			});
			f3.complete(1);
		};

		pool.runAsynch(r1);
		pool.runAsynch(r2);
		pool.runAsynch(r3);
		// wait for loop to complete
		f1.join();
		f2.join();
		f3.join();
		// wait last cart absorbed
		fr1.get().join();
		fr2.get().join();
		fr3.get().join();
	}

	@Test
	public void testParallelAsorted() throws InterruptedException {

		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitence("testParallelAsorted");
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.setName("testParallelAsorted");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testParallelAsorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelAsorted load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelAsorted data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
	}

	@Test
	public void testParallelSorted() throws InterruptedException {
		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitence("testParallelSorted");
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.setName("testParallelSorted");

		List<Integer> t1 = getIdList();
		List<Integer> t2 = getIdList();
		List<Integer> t3 = getIdList();
		System.out.println("testParallelSorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelSorted load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelSorted data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		assertEquals(testSize, tc.counter.get());

	}

	@Test
	public void testParallelUnload() throws InterruptedException {

		TrioConveyorExpireable tc = new TrioConveyorExpireable();

		Persistence<Integer> p = getPersitenceExp("testParallelUnload");
		PersistentConveyor<Integer, SmartLabel<TrioBuilderExpireable>, Trio> pc = p.wrapConveyor(tc);
		pc.unloadOnBuilderTimeout(true);
		pc.setName("testParallelUnload");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testParallelUnload " + testSize);

		AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
		AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
		AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();

		CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
		CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
		CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();

		Runnable r1 = () -> {
			t1.forEach(key -> {
				fr1.set(pc.part().id(key).label(TrioPartExpireable.TEXT1).value("txt1_" + key).place());
				sleep(key,sleepTime);
			});
			f1.complete(1);
		};
		Runnable r2 = () -> {
			t2.forEach(key -> {
				fr2.set(pc.part().id(key).label(TrioPartExpireable.TEXT2).value("txt2_" + key).place());
				sleep(key,sleepTime);
			});
			f2.complete(1);
		};
		Runnable r3 = () -> {
			t3.forEach(key -> {
				fr3.set(pc.part().id(key).label(TrioPartExpireable.NUMBER).value(key).place());
				sleep(key,sleepTime);
			});
			f3.complete(1);
		};

		long start = System.currentTimeMillis();
		pool.runAsynch(r1);
		pool.runAsynch(r2);
		pool.runAsynch(r3);

		f1.join();
		f2.join();
		f3.join();

		fr1.get().join();
		fr2.get().join();
		fr3.get().join();
		long end = System.currentTimeMillis();
		System.out.println("testParallelUnload load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelUnload data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		assertEquals(testSize, tc.counter.get());
	}

	@Test
	public void testParallelParallelAsorted() throws InterruptedException {

		TrioConveyor tc1 = new TrioConveyor();
		TrioConveyor tc2 = new TrioConveyor();

		Persistence<Integer> p1 = getPersitence("testParallelAsorted1");
		Persistence<Integer> p2 = getPersitence("testParallelAsorted2");
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc1 = p1.wrapConveyor(tc1);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc2 = p2.wrapConveyor(tc2);
		Stack<PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio>> st = new Stack<>();
		st.push(pc1);
		st.push(pc2);

		ParallelConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = new KBalancedParallelConveyor<>(() -> st.pop(), 2);
		pc.setName("testParallelParallelAsorted");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testParallelParallelAsorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelParallelAsorted load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p1.copy(),testSize);
		Tester.waitUntilArchived(p2.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelParallelAsorted data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc1.results.size() + tc2.results.size());
		assertEquals(testSize, tc1.counter.get()+tc2.counter.get());
	}
	
	@Test
	public void testOnlyConveyor() {
		TrioConveyor tc = new TrioConveyor();


		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testOnlyConveyor " + testSize);

		long start = System.currentTimeMillis();
		load(tc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testOnlyConveyor load complete in " + (end - start) + " msec.");


		long toComplete = System.currentTimeMillis();

		System.out.println("testOnlyConveyor data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		
	}
	
	@Test
	public void testInMemoryPersistence() {
		TrioConveyor tc = new TrioConveyor();

		PersistTestImpl p = new PersistTestImpl();
		p.setMaxBatchSize(batchSize);
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.setName("testInMemoryPersistence");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testInMemoryPersistence " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testInMemoryPersistence load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testInMemoryPersistence data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		
	}
	
	@Test
	public void testParallelSortedToFile() throws InterruptedException {
		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitenceFile("testParallelSortedFile");
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.setName("testParallelSortedFile");

		List<Integer> t1 = getIdList();
		List<Integer> t2 = getIdList();
		List<Integer> t3 = getIdList();
		System.out.println("testParallelSortedFile " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelSortedFile load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelSortedFile data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		assertEquals(testSize, tc.counter.get());

	}

	@Test
	public void testParallelSortedToPersistence() throws InterruptedException {
		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitencePersistence("testParallelSortedPersistence");
		PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> pc = p.wrapConveyor(tc);
		pc.setName("testParallelSortedFile");

		List<Integer> t1 = getIdList();
		List<Integer> t2 = getIdList();
		List<Integer> t3 = getIdList();
		System.out.println("testParallelSortedPersistence " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelSortedPersistence load complete in " + (end - start) + " msec.");

		Tester.waitUntilArchived(p.copy(),testSize);

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelSortedPersistence data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		assertEquals(testSize, tc.counter.get());

	}


}
