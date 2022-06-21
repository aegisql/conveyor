package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The type P balanced parallel conveyor.
 *
 * @param <K>   the type parameter
 * @param <L>   the type parameter
 * @param <OUT> the type parameter
 */
public class PBalancedParallelConveyor<K, L, OUT> extends ParallelConveyor<K, L, OUT> {
	
	private final List<ConveyorAcceptor<K, L, OUT>> testers = new ArrayList<>();
	
	private final CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
	{
		failedFuture.complete(false);
	}

	/**
	 * Instantiates a new P balanced parallel conveyor.
	 *
	 * @param testers the testers
	 */
	public PBalancedParallelConveyor(ConveyorAcceptor<K, L, OUT>... testers) {
		this(Arrays.asList(testers));
	}


	/**
	 * Instantiates a new P balanced parallel conveyor.
	 *
	 * @param testers the testers
	 */
	public PBalancedParallelConveyor(List<ConveyorAcceptor<K, L, OUT>> testers) {
		super();
		Objects.requireNonNull(testers,"ConveyorAcceptors must not be null");
		if(testers.size() == 0) {
			throw new ConveyorRuntimeException("ConveyorAcceptors size must be > 0");
		}
		ConveyorAcceptor<K,L,OUT> first = testers.get(0);
		Objects.requireNonNull(first,"ConveyorAcceptor must not be null");
		if(first.getPropertyNames().size() == 0) {
			throw new ConveyorRuntimeException("ConveyorAcceptor must have set of property predicates");
		}
		for(ConveyorAcceptor<K,L,OUT> pt:testers) {
			if( ! first.getPropertyNames().equals(pt.getPropertyNames())) {
				throw new ConveyorRuntimeException("All testers must have the same set of properties. Expected:"
						+first.getPropertyNames()
				+" but was: "+pt.getPropertyNames());
			}
		}
		this.testers.addAll(testers.stream().map(t -> new ConveyorAcceptor<>(t.conveyor, t.testers)).toList());
		this.conveyors.addAll(this.testers.stream().map(ConveyorAcceptor::getConveyor).toList());
	}
	
	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		testers.forEach(tester->tester.getConveyor().setAcknowledgeAction(ackAction));
	}
	
	private Conveyor<K, L, OUT> getMatched(Map<String,Object> properties) {
		for(ConveyorAcceptor<K, L, OUT> tester:testers) {
			if(tester.test(properties)) {
				return tester.getConveyor();
			}
		}
		return null;
	}

	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> cart) {
		Conveyor<K, L, OUT> conv = getMatched(cart.getAllProperties());
		if(conv != null) {
			return conv.command(cart);
		} else {
			return failedFuture;
		}
	}

	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		Conveyor<K, L, OUT> conv = getMatched(cart.getAllProperties());
		if(conv != null) {
			return conv.place(cart);
		} else {
			return failedFuture;
		}
	}

	@Override
	protected <V> CompletableFuture<Boolean> createBuildWithCart(Cart<K, V, L> cart) {
		return place(cart);
	}

	@Override
	protected CompletableFuture<OUT> createBuildFutureWithCart(
			Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier,
			BuilderSupplier<OUT> builderSupplier) {
		CompletableFuture<OUT> productFuture   = new CompletableFuture<>();
		BuilderAndFutureSupplier<OUT> supplier = new BuilderAndFutureSupplier<>(builderSupplier, productFuture);
		CreatingCart<K, OUT, L> cart           = cartSupplier.apply( supplier );
		Conveyor<K,L,OUT> balancedCobveyor     = this.getMatched(cart.getAllProperties());
		CompletableFuture<Boolean> cartFuture  = balancedCobveyor.place(cart);
		if( cartFuture.isCancelled()) {
			productFuture.cancel(true);
		}
		return productFuture;
	}

	@Override
	protected CompletableFuture<OUT> getFutureByCart(FutureCart<K, OUT, L> futureCart) {
		CompletableFuture<OUT> future = futureCart.getValue();
		CompletableFuture<Boolean> cartFuture = this.place( futureCart );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;		
	}

}
