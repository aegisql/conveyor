package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.ParallelConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import com.aegisql.conveyor.persistence.core.harness.ThreadPool;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyorExpireable;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
import com.aegisql.conveyor.persistence.core.harness.TrioPartExpireable;
import com.aegisql.conveyor.persistence.jdbc.impl.derby.DerbyPersistence;

public class PerfTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();

		String conveyor_db_path = "perfConv";
		File f = new File(conveyor_db_path);
		try {
			FileUtils.deleteDirectory(f);
			System.out.println("Directory perfConv has been deleted!");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	ThreadPool pool;

	int testSize = 10000;

	int batchSize;
	
	double sleepTime = 0.01;
	int sleepNumber;

	@Before
	public void setUp() throws Exception {
		pool = new ThreadPool(3);
		batchSize = testSize / 20;
		sleepNumber = batchSize;
		System.out.println("--- " + new Date());
	}

	@After
	public void tearDown() throws Exception {
		pool.shutdown();
	}

	Persistence<Integer> getPersitence(String table) {
		try {
			Thread.sleep(1000);

			return DerbyPersistence.forKeyClass(Integer.class).schema("perfConv").partTable(table)
					.completedLogTable(table + "Completed").labelConverter(TrioPart.class)
					.whenArchiveRecords().markArchived()
					.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS).maxBatchSize(batchSize).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	Persistence<Integer> getPersitenceExp(String table) {
		try {
			Thread.sleep(1000);

			return DerbyPersistence.forKeyClass(Integer.class).schema("perfConv").partTable(table)
					.completedLogTable(table + "Completed").labelConverter(TrioPartExpireable.class)
					.whenArchiveRecords().markArchived()
					.maxBatchTime(Math.min(60000, batchSize), TimeUnit.MILLISECONDS).maxBatchSize(batchSize).build();
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

	void waitUntilArchived(Persistence<Integer> p) {
		while (p.getNumberOfParts() > 0) {
			sleep(sleepNumber,1000.0);
		}
	}

	@Test
	public void testParallelAsorted() throws InterruptedException {

		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitence("testParallelAsorted");
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);
		pc.setName("testParallelAsorted");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testParallelAsorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelAsorted load complete in " + (end - start) + " msec.");

		waitUntilArchived(p.copy());

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelAsorted data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
	}

	@Test
	public void testParallelSorted() throws InterruptedException {
		TrioConveyor tc = new TrioConveyor();

		Persistence<Integer> p = getPersitence("testParallelSorted");
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);
		pc.setName("testParallelSorted");

		List<Integer> t1 = getIdList();
		List<Integer> t2 = getIdList();
		List<Integer> t3 = getIdList();
		System.out.println("testParallelSorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelSorted load complete in " + (end - start) + " msec.");

		waitUntilArchived(p.copy());

		long toComplete = System.currentTimeMillis();

		System.out.println("testParallelSorted data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		assertEquals(testSize, tc.counter.get());

	}

	@Test
	public void testParallelUnload() throws InterruptedException {

		TrioConveyorExpireable tc = new TrioConveyorExpireable();

		Persistence<Integer> p = getPersitenceExp("testParallelUnload");
		PersistentConveyor<Integer, TrioPartExpireable, Trio> pc = new PersistentConveyor(p, tc);
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

		waitUntilArchived(p.copy());

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
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1);
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2);
		Stack<PersistentConveyor<Integer, TrioPart, Trio>> st = new Stack<>();
		st.push(pc1);
		st.push(pc2);

		ParallelConveyor<Integer, TrioPart, Trio> pc = new KBalancedParallelConveyor<>(() -> st.pop(), 2);
		pc.setName("testParallelParallelAsorted");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testParallelParallelAsorted " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testParallelParallelAsorted load complete in " + (end - start) + " msec.");

		waitUntilArchived(p1.copy());
		waitUntilArchived(p2.copy());

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
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);
		pc.setName("testInMemoryPersistence");

		List<Integer> t1 = getIdListShuffled();
		List<Integer> t2 = getIdListShuffled();
		List<Integer> t3 = getIdListShuffled();
		System.out.println("testInMemoryPersistence " + testSize);

		long start = System.currentTimeMillis();
		load(pc, t1, t2, t3);
		long end = System.currentTimeMillis();
		System.out.println("testInMemoryPersistence load complete in " + (end - start) + " msec.");

		waitUntilArchived(p.copy());

		long toComplete = System.currentTimeMillis();

		System.out.println("testInMemoryPersistence data loaded and archived in  " + (toComplete - start) + " msec");
		assertEquals(testSize, tc.results.size());
		
	}

}