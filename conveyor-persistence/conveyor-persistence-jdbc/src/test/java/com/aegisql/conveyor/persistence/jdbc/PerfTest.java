package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.core.harness.ThreadPool;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;
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
					.whenArchiveRecords().markArchived()
					.maxBatchTime(1000,TimeUnit.MILLISECONDS)
					.maxBatchSize(100)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	
	@Test
	public void testParallel() throws InterruptedException {

	int testSize = 10000;
	
	TrioConveyor tc = new TrioConveyor();
	
	Persistence<Integer> p = getPersitence("trio");
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

	Persistence<Integer> p2 = getPersitence("trio");

	while(p2.getAllParts().size() > 0) {
		Thread.sleep(500);		
	}
	
	long toComplete = System.currentTimeMillis();
	
	System.out.println("Batch "+testSize+" loaded in "+(end-start)+" msec. completed in  "+(toComplete - start)+" msec");
	
	

	
	}

}
