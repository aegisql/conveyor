package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.LabeledValueConsumer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

// TODO: Auto-generated Javadoc

/**
 * The Class ReflectingValueConsumer.
 *
 * @param <B> the generic type
 */
public class ReflectingValueConsumer<B> implements LabeledValueConsumer<String, Object, B> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The saved setters. */
	private final Map<String, BiConsumer<Object, Object>> savedSetters = new HashMap<>();

	private final Map<String, Function<Object, Object>> savedGetters = new HashMap<>();

	private final Map<String,ReflectingValueConsumer<?>> deepConsumers = new HashMap<>();

	/** The init. */
	private boolean init = true;

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.LabeledValueConsumer#accept(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void accept(String label, Object value, B builder) {
		Objects.requireNonNull(label, "Label required");
		Objects.requireNonNull(builder, "Builder required");
		if(label.isEmpty()) {
			throw new RuntimeException("Label must not be empty");
		}
		if (init) {
			seedMethodsWithAnnotations(builder.getClass());
			seedFieldsWithAnnotations(builder.getClass());
			init = false;
		}
		String[] parts = label.split("\\.",2);
		if(parts.length == 1) {
			BiConsumer<Object, Object> setter;
			if (savedSetters.containsKey(label)) {
				setter = savedSetters.get(label);
			} else {
				setter = offerSetter(label, value, builder);
				savedSetters.put(label, setter);
			}
			setter.accept(builder, value);
		} else if(parts.length == 2) {
			Function<Object,Object> getter = null;
			if(savedGetters.containsKey(parts[0])) {
				getter = savedGetters.get(parts[0]);
			} else {
				getter = offerGetter(parts[0], value, builder);
			}
			Object obj = getter.apply(builder);
			Objects.requireNonNull(obj,"Object with label name '"+parts[0]+"' is not initialized!");
			ReflectingValueConsumer deepConsumer = deepConsumers.computeIfAbsent(parts[0], k -> new ReflectingValueConsumer<>());
			deepConsumer.accept(parts[1],value,obj);
		} else {
			throw new AssertionError("Unexpected number of elements in "+Arrays.toString(parts));
		}
	}

	private BiConsumer<Object, Object> buildSetter(Field f, String label) {
		return (b, v) -> {
			try {
				f.setAccessible(true);
				f.set(b, v);
			} catch (Exception e) {
				throw new RuntimeException("Field set error for '" + label + "' value='" + v + "'", e);
			}
		};
	}

	private BiConsumer<Object, Object> buildSetter(Method m, String label) {
		return (b, v) -> {
			m.setAccessible(true);
			try {
				m.invoke(b, v);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException("Setter execution error for '" + label + "' value=" + v + " method expects " + Arrays.toString(m.getParameterTypes()) ,e);
			}
		};
	}

	private BiConsumer<Object, Object> buildStaticSetter(Method m, String label) {
		return (b, v) -> {
			m.setAccessible(true);
			try {
				m.invoke(null, b, v);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException(
						"Static setter execution error for '" + label + "' value=" + v);
			}
		};
	}

	private Function<Object, Object> buildGetter(Field f, String label) {
		return (b) -> {
			try {
				f.setAccessible(true);
				Object o = f.get(b);
				if(o==null) {
					Object instance = f.getType().getConstructor().newInstance();
					f.setAccessible(true);
					f.set(b,instance);
				}
				return f.get(b);
			} catch (Exception e) {
				throw new RuntimeException("Field get error for '" + label + "'", e);
			}
		};
	}

	private Function<Object, Object> buildGetter(Method m, String label) {
		return (b) -> {
			try {
				m.setAccessible(true);
				return m.invoke(b);
			} catch (Exception e) {
				throw new RuntimeException("Method get error for '" + label + "'", e);
			}
		};
	}

	private Function<Object, Object> buildStaticGetter(Field f, String label) {
		return (b) -> {
			try {
				f.setAccessible(true);
				return f.get(null);
			} catch (Exception e) {
				throw new RuntimeException("Field static get error for '" + label + "'", e);
			}
		};
	}

	private Function<Object, Object> buildStaticGetter(Method m, String label) {
		return (b) -> {
			try {
				m.setAccessible(true);
				return m.invoke(null);
			} catch (Exception e) {
				throw new RuntimeException("Method static get error for '" + label + "'", e);
			}
		};
	}

	/**
	 * Seed fields with annotations.
	 *
	 * @param bClass the b class
	 */
	private void seedFieldsWithAnnotations(Class<? extends Object> bClass) {
		for (Field f : bClass.getDeclaredFields()) {
			Label label = f.getAnnotation(Label.class);
			if (label != null) {
				for (String match : label.value()) {
					if (savedSetters.containsKey(match)) {
						throw new RuntimeException(
								"Duplicate label match found: '" + match + "' on field " + f.getName());
					} else {
						savedSetters.put(match, buildSetter(f,match));
					}
				}
			}
		}
		Class sClass = bClass.getSuperclass();
		if (sClass != null) {
			seedFieldsWithAnnotations(sClass);
		}
	}

	/**
	 * Seed methods with annotations.
	 *
	 * @param bClass the b class
	 */
	private void seedMethodsWithAnnotations(Class bClass) {
		for (Method m : bClass.getDeclaredMethods()) {
			Label label = m.getAnnotation(Label.class);
			if (label != null) {
				if (m.getParameterCount() == 0) {
					Class<?> returnType = m.getReturnType();
					if( ! Void.TYPE.equals(returnType)) {
						//getter
					}
				} else if (m.getParameterCount() == 1) {
					for (String match : label.value()) {
						if (savedSetters.containsKey(match)) {
							throw new RuntimeException(
									"Duplicate label match found: " + match + "; method " + m.getName());
						} else {
							savedSetters.put(match, buildSetter(m,match));
						}
					}
				} else if (m.getParameterCount() == 2) {
					for (String match : label.value()) {
						if (savedSetters.containsKey(match)) {
							throw new RuntimeException(
									"Duplicate label match found: " + match + "; static method " + m.getName());
						} else {
							savedSetters.put(match, buildStaticSetter(m,match));
						}
					}

				} else {
					throw new RuntimeException("@Label annotation is not applicable to " + m.getName());
				}
			}
		}
		Class sClass = bClass.getSuperclass();
		if (sClass != null) {
			seedMethodsWithAnnotations(sClass);
		}
	}

	private Function<Object, Object> offerGetter(String label, Object v, B builder) {
		Class bClass = builder.getClass();
		Function<Object,Object> getter = (b) -> {
			throw new RuntimeException(
					"Getter not found for label '" + label + "'; builder class: " + bClass.getSimpleName()+"; value class: "+(v == null ? "N/A":v.getClass().getSimpleName()));
		};

		if(v != null) {
			Method m = lookupGetterDeep(label, bClass); // for setter
			if (m != null) {
				final Method method = m;
				getter = buildGetter(m,label);
			} else {
				m = lookupStaticGetterDeep(label, bClass);
				if (m != null) {
					final Method method = m;
					getter = buildStaticGetter(m,label);
				} else { // no methods found. Try to find a field
					Field f = lookupFieldDeep(label, bClass);
					if (f != null) {
						Field field = f;
						getter = buildGetter(f,label);
					}
				}
			}
		} else {
			List<Method> candidateMethods = new ArrayList<>();
			for (Method m : bClass.getDeclaredMethods()) {
				if (!label.equals(m.getName())) {
					continue;
				}
				if (m.getAnnotation(NoLabel.class) != null) {
					continue;
				}
				int params = m.getParameterCount();
				if (params == 0) {
					candidateMethods.add(m);
				}
				if (params == 1 && m.getParameterTypes()[0].equals(bClass)) {
					candidateMethods.add(m);
				}
			}

			if (candidateMethods.size() > 0) {
				if (candidateMethods.size() == 1) {
					Method method = candidateMethods.get(0);
					if (method.getParameterCount() == 1) {
						getter = buildGetter(method,label);
					} else {
						getter = buildStaticGetter(method,label);
					}
				} else {
					throw new RuntimeException("More than one matching methods found label "+label+" and NULL value. Cannot make a choice.");
				}
			} else {
				// try field. It can be only one
				Field field = lookupFieldDeep(label, bClass);
				getter = buildGetter(field, label);
			}
		}
		return getter;
	}


	/**
	 * Offer setter.
	 *
	 * @param label the label
	 * @param value the value
	 * @param builder the builder
	 * @return the bi consumer
	 */
	private BiConsumer<Object, Object> offerSetter(String label, Object value, Object builder) {
		Class bClass = builder.getClass();

		BiConsumer<Object, Object> setter = (b, v) -> {
			throw new RuntimeException(
					"Setter not found for label '" + label + "'; builder class: " + bClass.getSimpleName()+"; value class: "+(v == null ? "N/A":v.getClass().getSimpleName()));
		};

		if (value != null) {
			Class vClass = value.getClass();
			Method m = lookupSetterDeep(label, bClass, vClass); // for setter
			if (m != null) {
				final Method method = m;
				setter = buildSetter(m,label);
			} else {
				m = lookupStaticSetterDeep(label, bClass, vClass);
				if (m != null) {
					final Method method = m;
					setter = buildStaticSetter(m,label);
				} else { // no methods found. Try to find a field
					Field f = lookupFieldDeep(label, bClass);
					if (f != null) {
						Field field = f;
						setter = buildSetter(f,label);
					}
				}
			}

		} else {
			// value is null
			List<Method> candidateMethods = new ArrayList<>();
			for (Method m : bClass.getDeclaredMethods()) {
				if (!label.equals(m.getName())) {
					continue;
				}
				if(m.getAnnotation(NoLabel.class)!=null) {
					continue;
				}
				int params = m.getParameterCount();
				if (params == 1) {
					candidateMethods.add(m);
				}
				if (params == 2 && m.getParameterTypes()[0].equals(bClass)) {
					candidateMethods.add(m);
				}
			}

			if (candidateMethods.size() > 0) {
				if (candidateMethods.size() == 1) {
					Method method = candidateMethods.get(0);
					if (method.getParameterCount() == 1) {
						setter = buildSetter(method,label);

					} else {
						setter = buildStaticSetter(method,label);

					}
				} else {
					throw new RuntimeException("More than one matching methods found label "+label+" and NULL value. Cannot make a choice.");
				}
			} else {
				// try field. It can be only one
				Field field = lookupFieldDeep(label, bClass);
				setter = buildSetter(field,label);
			}

		}

		return setter;
	}

	/**
	 * Try method deep.
	 *
	 * @param name the name
	 * @param bClass the b class
	 * @param vClass the params
	 * @return the method
	 */
	private Method lookupSetterDeep(String name, Class bClass, Class vClass) {

		Method m = null;

		try {
			m = bClass.getDeclaredMethod(name, vClass);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				m = lookupSetterDeep(name, sClass, vClass);
			}
		}

		if(m== null) {
			Class sClass = vClass.getSuperclass();
			if (sClass != null) {
				m = lookupSetterDeep(name, bClass, sClass);
			}
		}

		return m;
	}

	private Method lookupGetterDeep(String name, Class bClass) {

		Method m = null;

		try {
			m = bClass.getDeclaredMethod(name);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				m = lookupGetterDeep(name, sClass);
			}
		}
		return m;
	}

	/**
	 * Try static method deep.
	 *
	 * @param name the name
	 * @param bClass the b class
	 * @param vClass the params
	 * @return the method
	 */
	private Method lookupStaticSetterDeep(String name, Class bClass, Class vClass) {

		Method m = null;

		try {
			m = bClass.getDeclaredMethod(name, bClass, vClass);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				m = lookupStaticSetterDeep(name, sClass, vClass);
			}
		}
		return m;
	}

	private Method lookupStaticGetterDeep(String name, Class bClass) {

		Method m = null;

		try {
			m = bClass.getDeclaredMethod(name, bClass);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				m = lookupStaticGetterDeep(name, sClass);
			}
		}
		return m;
	}


	/**
	 * Try field deep.
	 *
	 * @param name the name
	 * @param bClass the b class
	 * @return the field
	 */
	private Field lookupFieldDeep(String name, Class bClass) {

		Field f = null;

		try {
			f = bClass.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				f = lookupFieldDeep(name, sClass);
			}
		}
		return f;
	}

}
