package com.aegisql.conveyor.multichannel;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class MultichannelConceptTest.
 */
public class MultichannelConceptTest {

	/**
	 * Sets the up before class.
	 *
     */
	@BeforeAll
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
     */
	@AfterAll
	public static void tearDownAfterClass() {
	}

	/**
	 * Sets the up.
	 *
     */
	@BeforeEach
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
     */
	@AfterEach
	public void tearDown() {
	}

	/**
	 * Test simple concept.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConcept() throws InterruptedException {
		
		/*
		 * This is a simple fully scalable concept based on independent conveyors for each
		 * channel and one merging conveyor.
		 * 
		 * Additional merging smart labels - one per channel
		 * Partially implemented result. This is a requirement for channels
		 * Concept of label filter. Merging conv should only accept merging labels
		 * channels should accept corresponding labels too
		 * 
		 * */

		AssemblingConveyor<Integer, UserBuilderEvents, User> merge = new AssemblingConveyor<>();
		merge.setBuilderSupplier(UserBuilder::new);
		merge.setName("MERGE");
		merge.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		merge.resultConsumer().first(bin->{
			System.out.println("Merged "+bin.product);

		}).set();
		merge.addCartBeforePlacementValidator(cart->{
			System.out.println("placement: "+cart);
			if(cart.getLabel() != UserBuilderEvents.MERGE_A && cart.getLabel() != UserBuilderEvents.MERGE_B) {
				throw new RuntimeException("Unacceptable label "+cart);
			}
		});
		
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = new AssemblingConveyor<>();
		ch1.setName("CH1");
		ch1.setBuilderSupplier(UserBuilder::new);
		ch1.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.first != null && ub.last != null;
		});
		ch1.resultConsumer().first(bin->{
			ShoppingCart<Integer, User, UserBuilderEvents> cart = new ShoppingCart<>(bin.key, bin.product, UserBuilderEvents.MERGE_A,bin.expirationTime,TimeUnit.MILLISECONDS);
			merge.place(cart);
			System.out.println("A sent "+bin.product);
		}).set();

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = new AssemblingConveyor<>();
		ch2.setName("CH2");
		ch2.setBuilderSupplier(UserBuilder::new);
		ch2.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.yearOfBirth != null;
		});
		ch2.resultConsumer().first(bin->{
			ShoppingCart<Integer, User, UserBuilderEvents> cart = new ShoppingCart<>(bin.key, bin.product, UserBuilderEvents.MERGE_B,bin.expirationTime,TimeUnit.MILLISECONDS);
			merge.place(cart);
			System.out.println("B sent "+bin.product);
		}).set();

		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ch1.place(cartA1);
		ch1.place(cartA2);
		ch2.place(cartB1);
		
		Thread.sleep(20);
		
	}

}
