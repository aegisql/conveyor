package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.java_path.StringConverter;

import java.util.Objects;
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

	public void registerClass(Class<?> aClass, String... names) {
		Objects.requireNonNull(aClass,"registerClassShortName requires non empty class");
		reflectingValueConsumer.registerClass(aClass,names);
	}

	public <T> void registerStringConverter(Class<T> aClass, StringConverter<T> converter) {
		Objects.requireNonNull(aClass,"registerStringConverter requires non empty class");
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+aClass.getSimpleName());
		reflectingValueConsumer.registerStringConverter(aClass,converter);
	}

	public <T> void registerStringConverter(StringConverter<T> converter, String... names) {
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+String.join(",",names));
		reflectingValueConsumer.registerStringConverter(converter,names);
	}

}
