package com.aegisql.conveyor;

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

public class ReflectingValueConsumer <B> implements LabeledValueConsumer<String, Object, B> {

	private static final long serialVersionUID = 1L;
	
	private final Map<String,BiConsumer<Object,Object>> savedSetters = new HashMap<>();

	@Override
	public void accept(String label, Object value, Object builder) {
		Objects.requireNonNull(builder, "Builder required");
		BiConsumer<Object,Object> setter;
		if(savedSetters.containsKey(label)) {
			setter = savedSetters.get(label);
		} else {
			setter = offerSetter(label,value,builder);
			savedSetters.put(label, setter);
		}
		setter.accept(builder, value);
	}

	private BiConsumer<Object,Object> offerSetter(String label, Object value, Object builder) {
		Class bClass =  builder.getClass();
		
		BiConsumer<Object, Object> setter = (b,v)->{ throw new RuntimeException("Setter not found for label '"+label+"' and builder "+bClass.getSimpleName()); };

		List<Member> candidates = new ArrayList<>();
		
		if(value != null) {
			Class vClass = value.getClass();
			Method m;
			m = tryMethodDeep(label, bClass, vClass); //for setter
			if(m != null) {
				final Method method = m;
				setter = (b,v)->{
					try {
						method.setAccessible(true);
						method.invoke(builder, value);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException("Setter execution error for "+label+"' value='"+value+"'",e);
					}
				};
			} else {
				m = tryMethodDeep(label, bClass, bClass,vClass);	//for static setter			
				if(m!=null) {
					final Method method = m;
					setter = (b,v)->{
						try {
							method.setAccessible(true);
							method.invoke(null,builder, value);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException("Setter execution error for "+label+"' value='"+value+"'",e);
						}
					};
				} else { //no methods found. Try to find a field
					Field f = tryFieldDeep(label, bClass);
					if( f != null) {
						Field field = f;
						setter = (b,v)->{
							try {
								field.setAccessible(true);
								field.set(builder, value);
							} catch (Exception e) {
								throw new RuntimeException("Field set error for "+label+"' value='"+value+"'",e);
							}
						};
					}
				}
			}
			
		} else {
			//value is null
			List<Method> candidateMethods = new ArrayList<>();
			for(Method m: bClass.getDeclaredMethods()) {
				if( ! label.equals(m.getName())) {
					continue;
				}
				int params = m.getParameterCount(); 
				if(params == 1) {
					candidateMethods.add(m);
				}
				if(params == 2 && m.getParameterTypes()[0].equals(bClass)) {
					candidateMethods.add(m);
				}
			}
			
			if(candidateMethods.size() > 0) {
				if(candidateMethods.size() == 1) {
					Method method = candidateMethods.get(0);
					if(method.getParameterCount() == 1) {
						setter = (b,v)->{
							try {
								method.setAccessible(true);
								method.invoke(builder, value);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new RuntimeException("Setter execution error for "+label+"' value='"+value+"'",e);
							}
						};

					} else {
						setter = (b,v)->{
							try {
								method.setAccessible(true);
								method.invoke(null,builder, value);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new RuntimeException("Setter execution error for "+label+"' value='"+value+"'",e);
							}
						};

					}
				}
			} else {
				// fry field. It can be only one
				Field field = tryFieldDeep(label, bClass);
				setter = (b,v)->{
					try {
						field.setAccessible(true);
						field.set(builder, value);
					} catch (Exception e) {
						throw new RuntimeException("Field set error for "+label+"' value='"+value+"'",e);
					}
				};

			}
			
		}
		
		return setter;
	}
	
	private Method tryMethodDeep(String name, Class bClass, Class... params ) {
		
		Method m = null;
		
		try {
			m = bClass.getDeclaredMethod(name, params);
		} catch (NoSuchMethodException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if(sClass != null) {
				m = tryMethodDeep(name, sClass, params);
			}
		}
		return m;
	}

	private Field tryFieldDeep(String name, Class bClass ) {
		
		Field f = null;
		
		try {
			f = bClass.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			Class sClass = bClass.getSuperclass();
			if(sClass != null) {
				f = tryFieldDeep(name, sClass);
			}
		}
		return f;
	}

}
