package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.utils.MultiValue;
import com.aegisql.java_path.ClassRegistry;
import com.aegisql.java_path.JavaPath;
import com.aegisql.java_path.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * The Class ReflectingValueConsumer.
 *
 * @param <B> the generic type
 */
public class ReflectingValueConsumer<B> implements LabeledValueConsumer<String, Object, B> {

	private static final Logger LOG = LoggerFactory.getLogger(ReflectingValueConsumer.class);

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private final ClassRegistry classRegistry = new ClassRegistry();
	private final Map<String,String> pathAliases = new HashMap<>();
	private final Map<Class<?>, JavaPath> consumerFactoryMap = new HashMap<>();
	private final Map<String,ReflectingValueConsumer<?>> inDepthConsumers = new HashMap<>();
	private boolean enablePathCaching = false;

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
		JavaPath consumerFactory = consumerFactoryMap.computeIfAbsent(builder.getClass(), cls -> {
			classRegistry.registerClass(cls,cls.getSimpleName());
			JavaPath javaPath = new JavaPath(cls,classRegistry);
			javaPath.setEnablePathCaching(enablePathCaching);
			pathAliases.forEach(javaPath::setPathAlias);
			return javaPath;
		});
		if(value != null && value instanceof MultiValue multiValue) {
			consumerFactory.evalPath(label,builder,multiValue.asArray());
		} else {
			consumerFactory.evalPath(label, builder, value);
		}
	}

	public void registerClass(Class<?> aClass, String... names) {
		Objects.requireNonNull(aClass,"registerClassShortName requires non empty class");
		classRegistry.registerClass(aClass,names);
	}

	public <T> void registerStringConverter(Class<T> aClass, StringConverter<T> converter) {
		Objects.requireNonNull(aClass,"registerStringConverter requires non empty class");
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+aClass.getSimpleName());
		classRegistry.registerStringConverter(aClass,converter);
	}

	public <T> void registerStringConverter(StringConverter<T> converter, String... names) {
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+String.join(",",names));
		classRegistry.registerStringConverter(converter,names);
	}

	public void setPathAlias(String path, String alias) {
		pathAliases.put(path,alias);
		consumerFactoryMap.forEach((clas,javaPath)-> javaPath.setPathAlias(path,alias));
	}

	public void setEnablePathCaching(boolean enablePathCaching) {
		this.enablePathCaching = enablePathCaching;
		consumerFactoryMap.forEach((clas,javaPath)-> javaPath.setEnablePathCaching(enablePathCaching));
	}

}
