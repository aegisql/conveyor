package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.ParallelConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
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
			System.out.println("Directory has been deleted recursively !");
		} catch (IOException e) {
			System.err.println("Problem occurs when deleting the directory : " + conveyor_db_path);
			e.printStackTrace();
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	ThreadPool pool;

	
	@Before
	public void setUp() throws Exception {
		pool = new ThreadPool(3);
	}

	@After
	public void tearDown() throws Exception {
		pool.shutdown();
	}

	Persistence<Integer> getPersitence(String table) {
		try {
			return DerbyPersistence
					.forKeyClass(Integer.class)
					.schema("perfConv")
					.partTable(table)
					.completedLogTable(table+"Completed")
					.labelConverter(TrioPart.class)
					//.whenArchiveRecords().markArchived()
					.maxBatchTime(60000,TimeUnit.MILLISECONDS)
					.maxBatchSize(10000)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	Persistence<Integer> getPersitenceExp(String table) {
		try {
			return DerbyPersistence
					.forKeyClass(Integer.class)
					.schema("perfConv")
					.partTable(table)
					.completedLogTable(table+"Completed")
					.labelConverter(TrioPartExpireable.class)
					.maxBatchTime(1000,TimeUnit.MILLISECONDS)
					.maxBatchSize(1000)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParallelAsorted() throws InterruptedException {

	int testSize = 10000;
	
	TrioConveyor tc = new TrioConveyor();
	
	Persistence<Integer> p = getPersitence("testParallelAsorted");
	PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);

	List<Integer> t1 = new ArrayList<>();
	for(int i = 1; i<=testSize; i++) {
		t1.add(i);
	}
	Collections.shuffle(t1);
	
	List<Integer> t2 = new ArrayList<>(t1);
	Collections.shuffle(t2);

	List<Integer> t3 = new ArrayList<>(t1);
	Collections.shuffle(t3);

	AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();
	
	CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();
	
	Runnable r1 = ()->{
		t1.forEach(key->{
			fr1.set( pc.part().id(key).label(TrioPart.TEXT1).value("txt1_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f1.complete(1);
	};
	Runnable r2 = ()->{
		t2.forEach(key->{
			fr2.set( pc.part().id(key).label(TrioPart.TEXT2).value("txt2_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f2.complete(1);
	};
	Runnable r3 = ()->{
		t3.forEach(key->{
			fr3.set( pc.part().id(key).label(TrioPart.NUMBER).value(key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	Persistence<Integer> p2 = getPersitence("testParallelAsorted");

	while(p2.getNumberOfParts() > 0) {
		Thread.sleep(1000);		
	}
	
	long toComplete = System.currentTimeMillis();
	
	System.out.println("Batch "+testSize+" loaded in "+(end-start)+" msec. completed in  "+(toComplete - start)+" msec");
	assertEquals(testSize, tc.results.size());
	}

	
	@Test
	public void testParallelSorted() throws InterruptedException {

	int testSize = 10000;
	
	TrioConveyor tc = new TrioConveyor();
	
	Persistence<Integer> p = getPersitence("testParallelSorted");
	PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);

	List<Integer> t1 = new ArrayList<>();
	for(int i = 1; i<=testSize; i++) {
		t1.add(i);
	}
	
	List<Integer> t2 = new ArrayList<>(t1);

	List<Integer> t3 = new ArrayList<>(t1);

	AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();
	
	CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();
	
	Runnable r1 = ()->{
		t1.forEach(key->{
			fr1.set( pc.part().id(key).label(TrioPart.TEXT1).value("txt1_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f1.complete(1);
	};
	Runnable r2 = ()->{
		t2.forEach(key->{
			fr2.set( pc.part().id(key).label(TrioPart.TEXT2).value("txt2_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f2.complete(1);
	};
	Runnable r3 = ()->{
		t3.forEach(key->{
			fr3.set( pc.part().id(key).label(TrioPart.NUMBER).value(key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	Persistence<Integer> p2 = getPersitence("testParallelSorted");

	while(p2.getNumberOfParts() > 0) {
		Thread.sleep(500);		
	}
	
	long toComplete = System.currentTimeMillis();
	
	System.out.println("Batch "+testSize+" loaded in "+(end-start)+" msec. completed in  "+(toComplete - start)+" msec");
	assertEquals(testSize, tc.results.size());
	}


	@Test
	public void testParallelUnload() throws InterruptedException {

	int testSize = 10000;
	
	TrioConveyorExpireable tc = new TrioConveyorExpireable();
	
	Persistence<Integer> p = getPersitenceExp("testParallelUnload");
	PersistentConveyor<Integer, TrioPartExpireable, Trio> pc = new PersistentConveyor(p, tc);
	pc.unloadOnBuilderTimeout(true);
	pc.setName("UL");
	List<Integer> t1 = new ArrayList<>();
	for(int i = 1; i<=testSize; i++) {
		t1.add(i);
	}
	Collections.shuffle(t1);
	
	List<Integer> t2 = new ArrayList<>(t1);
	Collections.shuffle(t2);

	List<Integer> t3 = new ArrayList<>(t1);
	Collections.shuffle(t3);

	AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();
	
	CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();
	
	Runnable r1 = ()->{
		t1.forEach(key->{
			fr1.set( pc.part().id(key).label(TrioPartExpireable.TEXT1).value("txt1_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f1.complete(1);
	};
	Runnable r2 = ()->{
		t2.forEach(key->{
			fr2.set( pc.part().id(key).label(TrioPartExpireable.TEXT2).value("txt2_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f2.complete(1);
	};
	Runnable r3 = ()->{
		t3.forEach(key->{
			fr3.set( pc.part().id(key).label(TrioPartExpireable.NUMBER).value(key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	Persistence<Integer> p2 = getPersitenceExp("testParallelUnload");
//	Thread.sleep(10000);
	while(p2.getNumberOfParts() > 0) {
		Thread.sleep(5000);		
		System.out.println("Batch unload "+testSize+" tc size "+tc.results.size());
	}
	
	long toComplete = System.currentTimeMillis();
	
	System.out.println("Batch unload "+testSize+" loaded in "+(end-start)+" msec. completed in  "+(toComplete - start)+" msec");
	assertEquals(tc.results.size(),testSize);
	}

	@Test
	public void testParallelParallelAsorted() throws InterruptedException {

	int testSize = 10000;
	
	TrioConveyor tc1 = new TrioConveyor();
	TrioConveyor tc2 = new TrioConveyor();
	
	Persistence<Integer> p1 = getPersitence("testParallelAsorted1");
	Persistence<Integer> p2 = getPersitence("testParallelAsorted2");
	PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1);
	PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2);
	Stack<PersistentConveyor<Integer, TrioPart, Trio>> st = new Stack<>();
	st.push(pc1);
	st.push(pc2);

	ParallelConveyor<Integer, TrioPart, Trio> pc = new KBalancedParallelConveyor<>(()->st.pop(), 2);
	
	List<Integer> t1 = new ArrayList<>();
	for(int i = 1; i<=testSize; i++) {
		t1.add(i);
	}
	Collections.shuffle(t1);
	
	List<Integer> t2 = new ArrayList<>(t1);
	Collections.shuffle(t2);

	List<Integer> t3 = new ArrayList<>(t1);
	Collections.shuffle(t3);

	AtomicReference<CompletableFuture<Boolean>> fr1 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr2 = new AtomicReference<>();
	AtomicReference<CompletableFuture<Boolean>> fr3 = new AtomicReference<>();
	
	CompletableFuture<Integer> f1 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f2 = new CompletableFuture<Integer>();
	CompletableFuture<Integer> f3 = new CompletableFuture<Integer>();
	
	Runnable r1 = ()->{
		t1.forEach(key->{
			fr1.set( pc.part().id(key).label(TrioPart.TEXT1).value("txt1_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f1.complete(1);
	};
	Runnable r2 = ()->{
		t2.forEach(key->{
			fr2.set( pc.part().id(key).label(TrioPart.TEXT2).value("txt2_"+key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		f2.complete(1);
	};
	Runnable r3 = ()->{
		t3.forEach(key->{
			fr3.set( pc.part().id(key).label(TrioPart.NUMBER).value(key).place() );
			try {
				Thread.sleep(0,100000);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	Persistence<Integer> p3 = getPersitence("testParallelAsorted1");
	Persistence<Integer> p4 = getPersitence("testParallelAsorted2");

	while(p3.getNumberOfParts() > 0 || p4.getNumberOfParts() > 0) {
		Thread.sleep(1000);		
	}
	
	long toComplete = System.currentTimeMillis();
	
	System.out.println("Batch Parallel "+testSize+" loaded in "+(end-start)+" msec. completed in  "+(toComplete - start)+" msec");
	System.out.println("TC1="+tc1.results.size()+" TC2="+tc2.results.size());
	assertEquals(testSize, tc1.results.size()+tc2.results.size());
	}

	
}
