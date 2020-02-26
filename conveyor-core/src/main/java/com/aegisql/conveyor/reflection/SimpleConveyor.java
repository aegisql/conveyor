package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.Cart;

import java.util.Queue;
import java.util.function.Supplier;

public class SimpleConveyor<K,OUT> extends AssemblingConveyor<K, String, OUT> {


	public SimpleConveyor() {
		super();
		super.setDefaultCartConsumer(new ReflectingValueConsumer<Supplier<OUT>>());
	}

	public SimpleConveyor(BuilderSupplier<OUT> builderSupplier) {
		super();
		super.setDefaultCartConsumer(new ReflectingValueConsumer<Supplier<OUT>>());
		super.setBuilderSupplier(builderSupplier);
	}

	public SimpleConveyor(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier, BuilderSupplier<OUT> builderSupplier) {
		super(cartQueueSupplier);
		super.setDefaultCartConsumer(new ReflectingValueConsumer<Supplier<OUT>>());
		super.setBuilderSupplier(builderSupplier);
	}

}
