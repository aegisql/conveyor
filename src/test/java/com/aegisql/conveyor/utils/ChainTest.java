package com.aegisql.conveyor.utils;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuulder;
import com.aegisql.conveyor.utils.batch.BatchCart;
import com.aegisql.conveyor.utils.batch.BatchCollectingBuilder;
import com.aegisql.conveyor.utils.batch.BatchConveyor;
//import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuulder;
import com.aegisql.conveyor.utils.scalar.ScalarCart;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

public class ChainTest {

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
	public void testChain() throws InterruptedException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		String csv1 = "John,Dow,1990";
		String csv2 = "John,Smith,1991";
		
		ScalarCart<String,String> scalarCart1 = new ScalarCart<String, String>("test1", csv1);
		ScalarCart<String,String> scalarCart2 = new ScalarCart<String, String>("test2", csv2);
		
		BatchConveyor<User> b = new BatchConveyor<>();
		
		AtomicInteger ai = new AtomicInteger(0);
		AtomicInteger aii = new AtomicInteger(0);
		
		b.setBuilderSupplier( () -> new BatchCollectingBuilder<>(2, 10, TimeUnit.MILLISECONDS) );
		b.setScrapConsumer((obj)->{
			System.out.println(obj);
			ai.decrementAndGet();
		});
		b.setResultConsumer((list)->{
			System.out.println(list);
			ai.incrementAndGet();
			aii.addAndGet(list.product.size());
		});
		b.setExpirationCollectionInterval(100, TimeUnit.MILLISECONDS);
		b.setDefaultCartConsumer((a,l,c)->{
			System.out.println(">>>"+a+" "+l+" "+c);
		});
		
				
		
		
		ChainResult<String, Cart<String,User,?>, User> chain = new ChainResult(b,new BatchCart(new User("A","B",1)));

		sc.setResultConsumer(chain.andThen(u->{
			System.out.println("RESULT: "+u);
			usr.set(u.product);
		}));
		
		sc.add(scalarCart1);
		sc.add(scalarCart2);

		Thread.sleep(20);
		assertNotNull(usr.get());
	}

}
