/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.serial.SerializableBiConsumer;
import com.aegisql.conveyor.serial.SerializableConsumer;
import com.aegisql.conveyor.serial.SerializableRunnable;

// TODO: Auto-generated Javadoc
/**
 * The Interface SmartLabel.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <B> the generic type
 */
@FunctionalInterface
public interface SmartLabel<B> extends Serializable, Supplier<BiConsumer<B, Object>> {
	
	/**
	 * Gets the setter.
	 *
	 * @return the setter
	 */
	@Override
	BiConsumer<B, Object> get();
	
	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param <T> the generic type
	 * @param method the method
	 * @return the smart label
	 */
	static <B,T> SmartLabel<B> of(SerializableBiConsumer<B, T> method) {
		return ()->(b,t)->method.accept(b,(T)t);
	}

	static <B,T> SmartLabel<B> of(SerializableBiConsumer<B, T> method, Class<T> acceptedType) {
		return of(method).acceptType(acceptedType);
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param <T> the generic type
	 * @param labelName the method
	 * @param method the method
	 * @return the smart label
	 */
	static <B,T> SmartLabel<B> of(final String labelName, SerializableBiConsumer<B, T> method) {
		return of(method).labelName(labelName);
	}

	static <B,T> SmartLabel<B> of(final String labelName, SerializableBiConsumer<B, T> method, Class<T> acceptedType) {
		return of(method).labelName(labelName).acceptType(acceptedType);
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(SerializableConsumer<B> method) {
		return of( (b,oPhony) -> method.accept(b) );
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param labelName the method
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(final String labelName, SerializableConsumer<B> method) {
		return of( labelName, (SerializableBiConsumer<B, Object>)(b,oPhony) -> method.accept(b) );
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(SerializableRunnable method) {
		return of( (bPhoby,oPhony) -> method.run() );
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param labelName the method
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(final String labelName, SerializableRunnable method) {
		return of( labelName, (bPhoby,oPhony) -> method.run() );
	}

	/**
	 * Bare.
	 *
	 * @param <B> the generic type
	 * @return the smart label
	 */
	static <B> SmartLabel<B> bare() {
		return of( (bPhoby,oPhony) -> {} );
	}

	/**
	 * Bare.
	 *
	 * @param <B> the generic type
	 * @param labelName the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> bare(final String labelName) {
		return of( labelName, (bPhoby,oPhony) -> {} );
	}

	/**
	 * Peek.
	 *
	 * @param <B> the generic type
	 * @param <T> the generic type
	 * @return the smart label
	 */
	static <B extends Supplier<T>,T> SmartLabel<B> peek() {
		return of((b,future)->{
			try {
				((CompletableFuture<T>) future).complete(b.get());
			} catch (Exception e) {
				((CompletableFuture<T>) future).completeExceptionally(e);
			}
		});
	}

	/**
	 * Peek.
	 *
	 * @param <B> the generic type
	 * @param <T> the generic type
	 * @param name the name
	 * @return the smart label
	 */
	static <B extends Supplier<T>,T> SmartLabel<B> peek(String name) {
		return of(name,(b,future)->{
			try {
				((CompletableFuture<T>) future).complete(b.get());
			} catch (Exception e) {
				((CompletableFuture<T>) future).completeExceptionally(e);
			}
		});
	}

	private static <B> SmartLabel<B> transferTypesAndName(final SmartLabel<B> target, final SmartLabel<B> src) {
		SmartLabel<B> res = target.labelName(src.toString());
		for(int i = 0; i < src.getAcceptableTypes().size(); i++) {
			res = res.acceptType(src.getAcceptableTypes().get(i));
		}
		return res;
	}

	/**
	 * Identity.
	 *
	 * @return the smart label
	 */
	default SmartLabel<B> identity() {
		return this;
	}
	
	/**
	 * Label name.
	 *
	 * @param name the name
	 * @return the smart label
	 */
	default SmartLabel<B> labelName(final String name) {
		final SmartLabel<B> sl = this;
		return new SmartLabel<>() {
			final ArrayList<Class<?>> types = new ArrayList<>();
			{
				types.addAll(sl.getAcceptableTypes());
			}
            @Serial
            private static final long serialVersionUID = 5086346018176455134L;

            @Override
            public BiConsumer<B, Object> get() {
                return sl.get();
            }

            @Override
            public String toString() {
                return name;
            }

			@Override
			public List<Class<?>> getAcceptableTypes() {
				return types;
			}
		};
	}

	default SmartLabel<B> acceptType(Class<?> type) {
		final SmartLabel<B> sl = this;
		return new SmartLabel<>() {
			final ArrayList<Class<?>> types = new ArrayList<>();
			{
				types.addAll(sl.getAcceptableTypes());
				types.add(type);
			}

			@Serial
			private static final long serialVersionUID = 5086346018176455134L;

			@Override
			public BiConsumer<B, Object> get() {
				return sl.get();
			}

			@Override
			public String toString() {
				return sl.toString();
			}

			@Override
			public List<Class<?>> getAcceptableTypes() {
				return types;
			}
		};
	}
	/**
	 * Intercept.
	 *
	 * @param <T> the generic type
	 * @param clas the clas
	 * @param interceptor the interceptor
	 * @return the smart label
	 */
	default <T> SmartLabel<B> intercept(Class<T> clas, SerializableConsumer<T> interceptor) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((B bPhony,T o)->{
			if( o != null ) {
				if( clas.isAssignableFrom(o.getClass())) {
					interceptor.accept(o);
				} else {
					sl.get().accept(bPhony,o);
				}
			} else {
				sl.get().accept(bPhony,o);
			}
		}).acceptType(clas);
		return transferTypesAndName(res,this);
	}
	
	/**
	 * Intercept.
	 *
	 * @param <T> the generic type
	 * @param clas the clas
	 * @param interceptor the interceptor
	 * @return the smart label
	 */
	default <T> SmartLabel<B> intercept(Class<T> clas, SerializableBiConsumer<B,T> interceptor) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((B b,T o)->{
			if( o != null ) {
				if( clas.isAssignableFrom(o.getClass())) {
					interceptor.accept(b,o);
				} else {
					sl.get().accept(b,o);
				}
			} else {
				sl.get().accept(b,o);
			}
		}).acceptType(clas);
		return transferTypesAndName(res,this);
	}

	/**
	 * Intercept.
	 *
	 * @param <T> the generic type
	 * @param clas the clas
	 * @param interceptor the interceptor
	 * @return the smart label
	 */
	default <T> SmartLabel<B> intercept(Class<T> clas, SerializableRunnable interceptor) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((bPhony,oPhony)->{
			if( oPhony != null ) {
				if( clas.isAssignableFrom(oPhony.getClass())) {
					interceptor.run();
				} else {
					sl.get().accept(bPhony,oPhony);
				}
			} else {
				sl.get().accept(bPhony,oPhony);
			}
		});
		res = res.acceptType(clas);
		return transferTypesAndName(res,this);
	}
	
	/**
	 * And then.
	 *
	 * @param <T> the generic type
	 * @param after the after
	 * @return the smart label
	 */
	default <T> SmartLabel<B> andThen(SerializableBiConsumer<B,T> after) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((b,o) -> {
			sl.get().accept(b, o);
			after.accept(b, (T)o);
		});
		return transferTypesAndName(res,this);
	}
	
	/**
	 * Before.
	 *
	 * @param <T> the generic type
	 * @param before the before
	 * @return the smart label
	 */
	default <T> SmartLabel<B> before(SerializableBiConsumer<B,T> before) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((b,o) -> {
			before.accept(b, (T)o);
			sl.get().accept(b, o);
		});
		return transferTypesAndName(res,this);
	}
	
	/**
	 * And then.
	 *
	 * @param <T> the generic type
	 * @param after the after
	 * @return the smart label
	 */
	default <T> SmartLabel<B> andThen(SerializableConsumer<T> after) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.accept((T)o);
		});
		return transferTypesAndName(res,this);
	}
	
	/**
	 * Before.
	 *
	 * @param <T> the generic type
	 * @param before the before
	 * @return the smart label
	 */
	default <T> SmartLabel<B> before(SerializableConsumer<T> before) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((bPhony,o) -> {
			before.accept((T)o);
			sl.get().accept(bPhony, o);
		});
		return transferTypesAndName(res,this);
	}
	
	/**
	 * And then.
	 *
	 * @param after the after
	 * @return the smart label
	 */
	default SmartLabel<B> andThen(SerializableRunnable after) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.run();
		});
		return transferTypesAndName(res,this);
	}
	
	/**
	 * Before.
	 *
	 * @param before the before
	 * @return the smart label
	 */
	default SmartLabel<B> before(SerializableRunnable before) {
		SmartLabel<B> sl = this;
		SmartLabel<B> res = of((bPhony,o) -> {
			before.run();
			sl.get().accept(bPhony, o);
		});
		return transferTypesAndName(res,this);
	}

	/**
	 * Gets the payload.
	 *
	 * @param cart the cart
	 * @return the payload
	 */
	@SuppressWarnings("rawtypes")
	default Object getPayload(Cart cart) {
		return cart.getValue();
	}

	default List<Class<?>> getAcceptableTypes() {
		return Collections.EMPTY_LIST; //This list must be explicitly supported
	}

}
