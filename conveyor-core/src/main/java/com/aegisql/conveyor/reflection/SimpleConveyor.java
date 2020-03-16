package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.cart.Cart;

import java.util.Queue;
import java.util.function.Supplier;

public class SimpleConveyor<K,OUT> extends AssemblingConveyor<K, String, OUT> {

	private final ReflectingValueConsumer reflectingValueConsumer;

	public SimpleConveyor() {
		super();
		reflectingValueConsumer = new ReflectingValueConsumer<Supplier<OUT>>();
		super.setDefaultCartConsumer(reflectingValueConsumer);
	}

	public SimpleConveyor(BuilderSupplier<OUT> builderSupplier) {
		super();
		reflectingValueConsumer = new ReflectingValueConsumer<Supplier<OUT>>();
		super.setDefaultCartConsumer(reflectingValueConsumer);
		super.setBuilderSupplier(builderSupplier);
	}

	public SimpleConveyor(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier, BuilderSupplier<OUT> builderSupplier) {
		super(cartQueueSupplier);
		reflectingValueConsumer = new ReflectingValueConsumer<Supplier<OUT>>();
		super.setDefaultCartConsumer(reflectingValueConsumer);
		super.setBuilderSupplier(builderSupplier);
	}

	@Override
	public final <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<String,?,B> labeledValueConsumer){
		throw new UnsupportedOperationException("Simple Conveyor already initialized with the Cart Consumer");
	}

}
