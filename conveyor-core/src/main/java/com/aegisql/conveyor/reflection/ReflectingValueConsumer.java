package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.java_path.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	private final ClassRegistry classRegistry = new ClassRegistry();
	private final Map<Class<?>, PathUtils> consumerFactoryMap = new HashMap<>();
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
		PathUtils consumerFactory = consumerFactoryMap.computeIfAbsent(builder.getClass(),cls -> new PathUtils(cls,classRegistry));
		List<TypedPathElement> path = JavaPathParser.parse(label);
		consumerFactory.applyValueToPath(path,builder,value);
	}

	public void registerClassShortName(Class<?> aClass, String shortName) {
		Objects.requireNonNull(shortName,"registerClassShortName requires non empty name");
		Objects.requireNonNull(aClass,"registerClassShortName requires non empty class");
		classRegistry.registerClass(aClass);
		classRegistry.registerClassShortName(aClass,shortName);
	}

	public <T> void registerStringConverter(Class<T> aClass, StringConverter<T> converter) {
		Objects.requireNonNull(aClass,"registerStringConverter requires non empty class");
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+aClass.getSimpleName());
		classRegistry.registerStringConverter(aClass,converter);
	}

	public <T> void registerStringConverter(String alias, StringConverter<T> converter) {
		Objects.requireNonNull(alias,"registerStringConverter requires non empty class alias");
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+alias);
		classRegistry.registerStringConverter(alias,converter);
	}

}
