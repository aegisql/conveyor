package com.aegisql.conveyor.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.LabeledValueConsumer;

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

	/** The init. */
	private boolean init = true;

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.LabeledValueConsumer#accept(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void accept(String label, Object value, Object builder) {
		Objects.requireNonNull(builder, "Builder required");
		if (init) {
			seedMethodsWithAnnotations(builder.getClass());
			seedFieldsWithAnnotations(builder.getClass());
			init = false;
		}
		BiConsumer<Object, Object> setter;
		if (savedSetters.containsKey(label)) {
			setter = savedSetters.get(label);
		} else {
			setter = offerSetter(label, value, builder);
			savedSetters.put(label, setter);
		}
		setter.accept(builder, value);
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
						savedSetters.put(match, (b, v) -> {
							try {
								f.setAccessible(true);
								f.set(b, v);
							} catch (Exception e) {
								throw new RuntimeException("Field set error for '" + match + "' value='" + v + "'", e);
							}
						});
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
				if (m.getParameterCount() == 1) {
					for (String match : label.value()) {
						if (savedSetters.containsKey(match)) {
							throw new RuntimeException(
									"Duplicate label match found: " + match + " on method " + m.getName());
						} else {
							savedSetters.put(match, (b, v) -> {
								m.setAccessible(true);
								try {
									m.invoke(b, v);
								} catch (IllegalAccessException | IllegalArgumentException
										| InvocationTargetException e) {
									throw new RuntimeException("Setter execution error for '" + match + "' value=" + v,e);
								}
							});
						}
					}
				} else if (m.getParameterCount() == 2) {
					for (String match : label.value()) {
						if (savedSetters.containsKey(match)) {
							throw new RuntimeException(
									"Duplicate label match found: " + match + " on static method " + m.getName());
						} else {
							savedSetters.put(match, (b, v) -> {
								m.setAccessible(true);
								try {
									m.invoke(null, b, v);
								} catch (IllegalAccessException | IllegalArgumentException
										| InvocationTargetException e) {
									throw new RuntimeException(
											"Static setter execution error for '" + match + "' value=" + v);
								}
							});
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
					"Setter not found for label '" + label + "' and builder " + bClass.getSimpleName());
		};

		List<Member> candidates = new ArrayList<>();

		if (value != null) {
			Class vClass = value.getClass();
			Method m;
			m = tryMethodDeep(label, bClass, vClass); // for setter
			if (m != null) {
				final Method method = m;
				setter = (b, v) -> {
					try {
						method.setAccessible(true);
						method.invoke(builder, value);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException("Setter execution error for " + label + "' value='" + value + "'",
								e);
					}
				};
			} else {
				m = tryMethodDeep(label, bClass, bClass, vClass); // for static setter
				if (m != null) {
					final Method method = m;
					setter = (b, v) -> {
						try {
							method.setAccessible(true);
							method.invoke(null, builder, value);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException(
									"Setter execution error for " + label + "' value='" + value + "'", e);
						}
					};
				} else { // no methods found. Try to find a field
					Field f = tryFieldDeep(label, bClass);
					if (f != null) {
						Field field = f;
						setter = (b, v) -> {
							try {
								field.setAccessible(true);
								field.set(builder, value);
							} catch (Exception e) {
								throw new RuntimeException("Field set error for " + label + "' value='" + value + "'",
										e);
							}
						};
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
						setter = (b, v) -> {
							try {
								method.setAccessible(true);
								method.invoke(builder, value);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new RuntimeException(
										"Setter execution error for " + label + "' value='" + value + "'", e);
							}
						};

					} else {
						setter = (b, v) -> {
							try {
								method.setAccessible(true);
								method.invoke(null, builder, value);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new RuntimeException(
										"Setter execution error for " + label + "' value='" + value + "'", e);
							}
						};

					}
				} else {
					throw new RuntimeException("More than one matching methods found label "+label+" and NULL value. Cannot make a choice.");
				}
			} else {
				// fry field. It can be only one
				Field field = tryFieldDeep(label, bClass);
				setter = (b, v) -> {
					try {
						field.setAccessible(true);
						field.set(builder, value);
					} catch (Exception e) {
						throw new RuntimeException("Field set error for " + label + "' value='" + value + "'", e);
					}
				};

			}

		}

		return setter;
	}

	/**
	 * Try method deep.
	 *
	 * @param name the name
	 * @param bClass the b class
	 * @param params the params
	 * @return the method
	 */
	private Method tryMethodDeep(String name, Class bClass, Class... params) {

		Method m = null;

		try {
			m = bClass.getDeclaredMethod(name, params);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				m = tryMethodDeep(name, sClass, params);
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
	private Field tryFieldDeep(String name, Class bClass) {

		Field f = null;

		try {
			f = bClass.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if (sClass != null) {
				f = tryFieldDeep(name, sClass);
			}
		}
		return f;
	}

}
