package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;
import com.aegisql.conveyor.LabeledValueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// TODO: Auto-generated Javadoc

/**
 * The Class ReflectingValueConsumer.
 *
 * @param <B> the generic type
 */
public class ReflectingValueConsumer<B> implements LabeledValueConsumer<String, Object, B> {

	private static Logger LOG = LoggerFactory.getLogger(ReflectingValueConsumer.class);

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private final Map<Class<?>,ConsumerFactory> consumerFactoryMap = new HashMap<>();
	private final Map<String,ReflectingValueConsumer<?>> inDepthConsumers = new HashMap<>();

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.LabeledValueConsumer#accept(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void accept(String label, Object value, B builder) {
		Objects.requireNonNull(label, "Label required");
		Objects.requireNonNull(builder, "Builder required");
		if(label.isEmpty()) {
			throw new ConveyorRuntimeException("Label must not be empty");
		}
		if(label.startsWith("@")) {
			LOG.debug("Accepted {}. No action",label);
			return;
		}

		ConsumerFactory consumerFactory = consumerFactoryMap.computeIfAbsent(builder.getClass(),ConsumerFactory::new);

		Class vClass = value == null ? null : value.getClass();

		String[] parts = label.split("\\.",2);
		if(parts.length == 1) {
			consumerFactory.offerSetter(label,vClass).accept(builder,value);
		} else if(parts.length == 2) {
			Function<Object, Object> getter = consumerFactory.offerGetter(parts[0]);
			Object subBuilder = getter.apply(builder);
			Objects.requireNonNull(subBuilder,"Object with label name '"+parts[0]+"' is not initialized!");
			ReflectingValueConsumer deepConsumer = inDepthConsumers.computeIfAbsent(parts[0], k -> new ReflectingValueConsumer<>());
			deepConsumer.accept(parts[1],value,subBuilder);
		} else {
			throw new AssertionError("Unexpected number of elements in "+Arrays.toString(parts));
		}
	}

}
