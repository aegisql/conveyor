package com.aegisql.conveyor.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;

public class PBalancedParallelConveyor<K, L, OUT> extends ParallelConveyor<K, L, OUT> {
	
	private final List<CartPropertyTester<K, L, OUT>> testers = new ArrayList<>();
	
	private final CompletableFuture<Boolean> failedFuture = new CompletableFuture<Boolean>();
	{
		failedFuture.complete(false);
	}

	public PBalancedParallelConveyor(List<CartPropertyTester<K, L, OUT>> testers) {
		super();
		this.testers.addAll(testers);
	}
	
	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		testers.forEach(tester->tester.getConveyor().setAcknowledgeAction(ackAction));
	}
	
	private Conveyor<K, L, OUT> getMatched(Map<String,Object> properties) {
		for(CartPropertyTester<K, L, OUT> tester:testers) {
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
		CompletableFuture<OUT> productFuture   = new CompletableFuture<OUT>();
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
