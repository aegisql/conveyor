package com.aegisql.conveyor.reflection;

import java.util.function.Supplier;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;

public class SimpleConveyor<K,OUT> extends AssemblingConveyor<K, String, OUT> {

	public SimpleConveyor(BuilderSupplier<OUT> builderSupplier) {
		super();
		super.setDefaultCartConsumer(new ReflectingValueConsumer<Supplier<OUT>>());
		super.setBuilderSupplier(builderSupplier);
	}

}
