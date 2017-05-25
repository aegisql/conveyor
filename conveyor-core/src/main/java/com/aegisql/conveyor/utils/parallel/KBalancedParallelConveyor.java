/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.utils.parallel;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.loaders.ResultConsumerLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;

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
public class KBalancedParallelConveyor<K, L, OUT> extends ParallelConveyor<K, L, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(KBalancedParallelConveyor.class);
	
	/**
	 * Instantiates a new k-balanced parallel conveyor with AssemblingConveyors inside.
	 *
	 * @param pf the pf
	 */
	public KBalancedParallelConveyor( int pf ) {
		this(AssemblingConveyor::new, pf);
	}
	
	/**
	 * Instantiates a new k-balanced parallel conveyor with custom Conveyor supplier provided by user.
	 *
	 * @param cs the cs
	 * @param pf the pf
	 */
	public KBalancedParallelConveyor( Supplier<? extends Conveyor<K, L, OUT>> cs, int pf ) {
		super();
		if( pf <=0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		this.pf = pf;
		for(int i = 0; i < pf; i++) {
			this.conveyors.add(cs.get());
		}

		this.balancingCart = cart -> { 
			int index = (cart.getKey().hashCode() & 0x0fffffff) % pf;
			return this.conveyors.subList(index, index+1);
		};

		this.balancingCommand = command -> { 
			int index = (command.getKey().hashCode() & 0x0fffffff) % pf;
			return this.conveyors.subList(index, index+1);
		};

		this.setMbean(this.name);
		
		LOG.debug("K-Balanced Parallel conveyor created with {} threads",pf);
	
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */	
	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		Objects.requireNonNull(command, "Command is null");
		if(command.getKey() != null) {
			return this.balancingCommand.apply(command).get(0).command(command);
		} else {
			CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
			cf.complete(true);
			for(Conveyor<K, L, OUT> conv: this.conveyors) {
				cf = cf.thenCombine(conv.command((GeneralCommand<K, V>) command.copy()), (a,b)-> a && b);
			}
			return cf;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K,V,L> cart) {
		Objects.requireNonNull(cart, "Cart is null");
		Conveyor<K,L,OUT> balancedCobveyor    = this.balancingCart.apply(cart).get(0);
		CompletableFuture<Boolean> cartFuture = balancedCobveyor.place(cart);
		return cartFuture;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#createBuildFutureWithCart(java.util.function.Function, com.aegisql.conveyor.BuilderSupplier)
	 */
	@Override
	protected CompletableFuture<OUT> createBuildFutureWithCart(Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier, BuilderSupplier<OUT> builderSupplier) {
		CompletableFuture<OUT> productFuture   = new CompletableFuture<OUT>();
		BuilderAndFutureSupplier<OUT> supplier = new BuilderAndFutureSupplier<>(builderSupplier, productFuture);
		CreatingCart<K, OUT, L> cart           = cartSupplier.apply( supplier );
		Conveyor<K,L,OUT> balancedCobveyor     = this.balancingCart.apply(cart).get(0);
		CompletableFuture<Boolean> cartFuture  = balancedCobveyor.place(cart);
		if( cartFuture.isCancelled()) {
			productFuture.cancel(true);
		}
		return productFuture;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#getFutureByCart(com.aegisql.conveyor.cart.FutureCart)
	 */
	@Override
	protected CompletableFuture<OUT> getFutureByCart(FutureCart<K,OUT,L> futureCart) {
		CompletableFuture<OUT> future = futureCart.getValue();
		CompletableFuture<Boolean> cartFuture = this.place( futureCart );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#getExpirationTime(java.lang.Object)
	 */
	public long getExpirationTime(K key) {
		return ((ParallelConveyor<K, L, OUT>) this.balancingCart.apply( new ShoppingCart(key, null, null)).get(0)).getExpirationTime(key);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#isLBalanced()
	 */
	@Override
	public boolean isLBalanced() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#toString()
	 */
	@Override
	public String toString() {
		return "K-BalancedParallelConveyor [name=" + name + ", pf=" + pf + "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.parallel.ParallelConveyor#createBuildWithCart(com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	protected <V> CompletableFuture<Boolean> createBuildWithCart(Cart<K, V, L> cart) {
		return place(cart);
	}

}
