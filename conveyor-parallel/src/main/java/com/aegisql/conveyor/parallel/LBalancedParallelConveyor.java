/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO: Auto-generated Javadoc
/**
 * The Class ParallelConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the Product type
 */
public class LBalancedParallelConveyor<K, L, OUT> extends ParallelConveyor<K, L, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG   = LoggerFactory.getLogger(LBalancedParallelConveyor.class);

	/** The final consumer. */
	protected Conveyor<K, L, OUT> finalConsumer = null;
	
	/**
	 * Instantiates a new l balanced parallel conveyor.
	 *
	 * @param conveyors the conveyors
	 */
	public LBalancedParallelConveyor(String... conveyors) {
		super();
		Objects.requireNonNull(conveyors,"List of conveyors is null");
		if(conveyors.length == 0) {
			throw new RuntimeException("List of conveyors is empty");
		}
		
		Conveyor<K, L, OUT>[] array = new Conveyor[conveyors.length];
		for(int i = 0; i < conveyors.length; i++) {
			array[i] = Conveyor.byName(conveyors[i]);
		}
		init(array);
	}
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param conveyors the conveyors
	 */
	public LBalancedParallelConveyor(Conveyor<K, L, OUT>... conveyors) {
		super();
		init(conveyors);
	}
	
	private void init(Conveyor<K, L, OUT>... conveyors) {
		this.pf = conveyors.length;
		if( this.pf == 0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		int kBalanced     = 0;
		int lBalanced     = 0;
		int notForvarding = 0;
		
		Conveyor<K, L, OUT> lastNotForwarding = null; 
		
		List<Conveyor<K, L, OUT>> defaultConv = new ArrayList<>();
		Map<L,List<Conveyor<K, L, OUT>>> map = new HashMap<>(); 
		for(Conveyor<K, L, OUT> c:conveyors) {
			this.conveyors.add(c);
			if(c.isLBalanced()) {
				lBalanced++;
				Set<L> labels = c.getAcceptedLabels();
				for(L l:labels) {
					List<Conveyor<K, L, OUT>> convs = map.get(l);
					if(convs == null) {
						convs = new ArrayList<>();
					}
					convs.add(c);
					map.put(l, convs);
				}
			} else {
				kBalanced++;
				defaultConv.add( c );
			}
			if( ! c.isForwardingResults()) {
				lastNotForwarding = c;
				notForvarding++;
			}
		}

		if(notForvarding == 1 && lBalanced > 0) {
			List<Conveyor<K, L, OUT>> notForwardingList = new ArrayList<>();
			notForwardingList.add(lastNotForwarding);
			finalConsumer = lastNotForwarding;
			map.put(null, notForwardingList); //Creating cart delivered to all, had null in place of Label
		}
		
		if(lBalanced == 0) {
			throw new RuntimeException("L-Balanced parallel conveyor must have at least one L-balanced assembling conveyor");
		} else {
			if( kBalanced > 1 ) {
				throw new RuntimeException("L-Balanced parallel conveyor cannot have more than one K-balanced default assembling conveyor");
			} else if(kBalanced == 1) {
				LOG.debug("L-Balanced Parallel conveyor with default labels. {}",map);
				this.balancingCart = cart -> map.getOrDefault(cart.getLabel(), defaultConv);
			} else {
				LOG.debug("L-Balanced Parallel conveyor. {}",map);
				this.balancingCart = cart -> { 
					if(map.containsKey(cart.getLabel())) {
						return map.get(cart.getLabel());
					} else {
						throw new RuntimeException("L-Balanced parallel conveyor "+this.name+"has no default conveyor for label "+cart.getLabel());
					}
				};
			}
			this.balancingCommand = command -> this.conveyors;
			this.lBalanced = true;
		}
		this.setMbean(this.name);		
	}
	
	/**
	 * Command.
	 *
	 * @param <V> the value type
	 * @param cart the cart
	 * @return the completable future
	 */
	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> cart) {
		Objects.requireNonNull(cart, "Command is null");
		CompletableFuture<Boolean> combinedFutures = new CompletableFuture<>();
		combinedFutures.complete(true);
		for(Conveyor<K, L, OUT> conv: this.balancingCommand.apply(cart)) {
			combinedFutures = combinedFutures.thenCombine(conv.command((GeneralCommand<K, V>) cart.copy()), (a,b) -> a && b );
		}
		return combinedFutures;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K,V,L> cart) {
		Objects.requireNonNull(cart, "Cart is null");
		CompletableFuture<Boolean> combinedFuture = null;
		for(Conveyor<K,L,OUT> conv : this.balancingCart.apply(cart)) {
			if(combinedFuture == null) {
				combinedFuture = conv.place(cart.copy());
			} else {
				combinedFuture = combinedFuture.thenCombine(conv.place(cart.copy()), ( a, b ) -> a && b );
			}
		}
		return combinedFuture;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#createBuildWithCart(com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	protected <V> CompletableFuture<Boolean> createBuildWithCart(Cart<K, V, L> cart) {
		Objects.requireNonNull(cart, "Cart is null");
		CompletableFuture<Boolean> combinedFuture = null;
		for(Conveyor<K,L,OUT> conv : this.conveyors) {
			if(combinedFuture == null) {
				combinedFuture = conv.place(cart.copy());
			} else {
				combinedFuture = combinedFuture.thenCombine(conv.place(cart.copy()), ( a, b ) -> a && b );
			}
		}
		return combinedFuture;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#createBuildFutureWithCart(java.util.function.Function, com.aegisql.conveyor.BuilderSupplier)
	 */
	protected CompletableFuture<OUT> createBuildFutureWithCart(Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier, BuilderSupplier<OUT> builderSupplier) {
		Objects.requireNonNull(cartSupplier, "Cart supplier is null");
		CompletableFuture<Boolean> combinedCreateFuture = null;
		CompletableFuture<OUT> productFuture   = new CompletableFuture<>();
		BuilderAndFutureSupplier<OUT> supplier = new BuilderAndFutureSupplier<>(builderSupplier, productFuture);
		CreatingCart<K, OUT, L> cart           = cartSupplier.apply( supplier );
		
		for(Conveyor<K,L,OUT> conv : this.conveyors) {
			if(conv.isForwardingResults()) {
				LOG.debug("Create in conveyor {} {}",conv,cart);
				if(combinedCreateFuture == null) {
					combinedCreateFuture = conv.place(new CreatingCart<>(cart.getKey(), builderSupplier, cart.getCreationTime(), cart.getExpirationTime(), cart.getPriority()));
				} else {
					combinedCreateFuture = combinedCreateFuture.thenCombine(conv.place(new CreatingCart<>(cart.getKey(), builderSupplier, cart.getCreationTime(), cart.getExpirationTime(), cart.getPriority())), (a, b ) -> a && b );
				}
			} else {
				//this conv will finally create the product
				LOG.debug("Final conveyor {} {}",conv,cart);
				if(combinedCreateFuture == null) {
					combinedCreateFuture = conv.place(cart);
				} else {
					combinedCreateFuture = combinedCreateFuture.thenCombine(conv.place(cart), ( a, b ) -> a && b );
				}
			}
		}
		Objects.requireNonNull(combinedCreateFuture, "Create future is empty");
		if( combinedCreateFuture.isCancelled()) {
			productFuture.cancel(true);
		}
		return productFuture;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#getFutureByCart(com.aegisql.conveyor.cart.FutureCart)
	 */
	protected CompletableFuture<OUT> getFutureByCart(FutureCart<K,OUT,L> futureCart) {
		CompletableFuture<OUT> future = futureCart.getValue();
		CompletableFuture<Boolean> cartFuture = this.finalConsumer.place( futureCart );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;		
	}
	
	
	/**
	 * Gets the expiration time.
	 *
	 * @param key the key
	 * @param label the label
	 * @return the expiration time
	 */
	public long getExpirationTime(K key,L label) {
		return ((ParallelConveyor<K, L, OUT>) this.balancingCart.apply( new ShoppingCart(key, null, label)).get(0)).getExpirationTime(key);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#isLBalanced()
	 */
	@Override
	public boolean isLBalanced() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#toString()
	 */
	@Override
	public String toString() {
		return "L-Balanced ParallelConveyor [name=" + name + ", pf=" + pf + ", lBalanced=" + lBalanced + "]";
	}

	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		finalConsumer.setAcknowledgeAction(ackAction);
	}

}
