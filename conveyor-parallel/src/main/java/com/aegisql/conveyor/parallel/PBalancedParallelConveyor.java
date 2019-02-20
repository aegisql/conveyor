package com.aegisql.conveyor.parallel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;

public class PBalancedParallelConveyor<K, L, OUT> extends ParallelConveyor<K, L, OUT> {

	public PBalancedParallelConveyor() {
		super();
	}
	
	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> cart) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected <V> CompletableFuture<Boolean> createBuildWithCart(Cart<K, V, L> cart) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected CompletableFuture<OUT> createBuildFutureWithCart(
			Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier,
			BuilderSupplier<OUT> builderSupplier) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected CompletableFuture<OUT> getFutureByCart(FutureCart<K, OUT, L> futureCart) {
		// TODO Auto-generated method stub
		return null;
	}

}
