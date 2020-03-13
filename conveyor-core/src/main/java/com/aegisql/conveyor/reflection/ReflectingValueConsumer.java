package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;
import com.aegisql.conveyor.LabeledValueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
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

		String[] parts = split(label);
		if(parts.length == 1) {
			consumerFactory.offerSetter(label,vClass).accept(builder,value);
		} else if(parts.length == 2) {
			BiFunction<Object, Object, Object> getter = consumerFactory.offerGetter(parts[0],vClass);
			Object subBuilder = getter.apply(builder,value);
			Objects.requireNonNull(subBuilder,"Object with label name '"+parts[0]+"' is not initialized!");
			ReflectingValueConsumer deepConsumer = inDepthConsumers.computeIfAbsent(parts[0], k -> new ReflectingValueConsumer<>());
			deepConsumer.accept(parts[1],value,subBuilder);
		} else {
			throw new AssertionError("Unexpected number of elements in "+Arrays.toString(parts));
		}
	}

	private String[] split(String s){
		boolean inParam = false;
		boolean inB1 = true;
		char[] chars = s.toCharArray();
		StringBuilder b1 = new StringBuilder();
		StringBuilder b2 = new StringBuilder();
		StringBuilder sb = b1;
		for(char ch:chars) {
			if(inB1) {
				if('{'==ch) {
					inParam = true;
				}
				if('}'==ch) {
					inParam = false;
				}
				if('.'==ch && ! inParam) {
					sb = b2;
					inB1 = false;
					continue;
				}
				sb.append(ch);
			} else {
				b2.append(ch);
			}
		}
		if(b2.length() == 0) {
			return new String[]{b1.toString()};
		} else {
			return new String[]{b1.toString(),b2.toString()};
		}
	}

	public static void registerClassShortName(Class<?> aClass, String shortName) {
		Objects.requireNonNull(shortName,"registerClassShortName requires non empty name");
		Objects.requireNonNull(aClass,"registerClassShortName requires non empty class");
		if(LabelProperty.CLASS_MAP.containsKey(shortName) && ! aClass.equals(LabelProperty.CLASS_MAP.get(shortName))) {
			throw new ConveyorRuntimeException("Short name "+shortName+" for class "+aClass.getSimpleName()+" already occupied by "+LabelProperty.CLASS_MAP.get(shortName).getSimpleName());
		}
		LabelProperty.CLASS_MAP.put(shortName,aClass);
	}

	public static <T> void registerStringConverter(Class<T> aClass, Function<String,T> converter) {
		Objects.requireNonNull(aClass,"registerStringConverter requires non empty class");
		Objects.requireNonNull(converter,"registerStringConverter requires converter for class "+aClass.getSimpleName());
		LabelProperty.CONVERSION_MAP.put(aClass,converter);
	}
}
