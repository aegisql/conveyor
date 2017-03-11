/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
	static <B,T> SmartLabel<B> of(BiConsumer<B, T> method) {
		return ()->(b,t)->method.accept(b,(T)t);
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
	static <B,T> SmartLabel<B> of(final String labelName, BiConsumer<B, T> method) {
		return new SmartLabel<B>() {
			private static final long serialVersionUID = 1L;
			@Override
			public BiConsumer<B, Object> get() {
				return (BiConsumer<B, Object>) method;
			}
			@Override
			public String toString() {
				return labelName;
			}
		};
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(Consumer<B> method) {
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
	static <B> SmartLabel<B> of(final String labelName, Consumer<B> method) {
		return of( labelName, (b,oPhony) -> method.accept(b) );
	}

	/**
	 * Of.
	 *
	 * @param <B> the generic type
	 * @param method the method
	 * @return the smart label
	 */
	static <B> SmartLabel<B> of(Runnable method) {
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
	static <B> SmartLabel<B> of(final String labelName, Runnable method) {
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
	 * Identity.
	 *
	 * @return the smart label
	 */
	default SmartLabel<B> identity() {
		return this;
	}
	
	default SmartLabel<B> labelName(final String name) {
		final SmartLabel<B> sl = this;
		return new SmartLabel<B>() {
			
			private static final long serialVersionUID = 5086346018176455134L;
			@Override
			public BiConsumer<B, Object> get() {
				return sl.get();
			}
			@Override
			public String toString() {
				return name;
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
	default <T> SmartLabel<B> intercept(Class<T> clas, Consumer<T> interceptor) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o)->{
			if( o != null ) {
				if( clas.isAssignableFrom(o.getClass())) {
					interceptor.accept((T)o);
				} else {
					sl.get().accept(bPhony,o);					
				}
			} else {
				sl.get().accept(bPhony,o);
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
	default <T> SmartLabel<B> intercept(Class<T> clas, BiConsumer<B,T> interceptor) {
		SmartLabel<B> sl = this;
		return ()->(b,o)->{
			if( o != null ) {
				if( clas.isAssignableFrom(o.getClass())) {
					interceptor.accept(b,(T)o);
				} else {
					sl.get().accept(b,o);					
				}
			} else {
				sl.get().accept(b,o);
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
	default <T> SmartLabel<B> intercept(Class<T> clas, Runnable interceptor) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,oPhony)->{
			if( oPhony != null ) {
				if( clas.isAssignableFrom(oPhony.getClass())) {
					interceptor.run();
				} else {
					sl.get().accept(bPhony,oPhony);					
				}
			} else {
				sl.get().accept(bPhony,oPhony);
			}
		};
	}
	
	/**
	 * And then.
	 *
	 * @param <T> the generic type
	 * @param after the after
	 * @return the smart label
	 */
	default <T> SmartLabel<B> andThen(BiConsumer<B,T> after) {
		SmartLabel<B> sl = this;
		return ()->(b,o) -> {
			sl.get().accept(b, o);
			after.accept(b, (T)o);
		};
	}
	
	/**
	 * Before.
	 *
	 * @param <T> the generic type
	 * @param before the before
	 * @return the smart label
	 */
	default <T> SmartLabel<B> before(BiConsumer<B,T> before) {
		SmartLabel<B> sl = this;
		return ()->(b,o) -> {
			before.accept(b, (T)o);
			sl.get().accept(b, o);
		};
	}
	
	/**
	 * And then.
	 *
	 * @param <T> the generic type
	 * @param after the after
	 * @return the smart label
	 */
	default <T> SmartLabel<B> andThen(Consumer<T> after) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.accept((T)o);
		};
	}
	
	/**
	 * Before.
	 *
	 * @param <T> the generic type
	 * @param before the before
	 * @return the smart label
	 */
	default <T> SmartLabel<B> before(Consumer<T> before) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			before.accept((T)o);
			sl.get().accept(bPhony, o);
		};
	}
	
	/**
	 * And then.
	 *
	 * @param after the after
	 * @return the smart label
	 */
	default SmartLabel<B> andThen(Runnable after) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.run();
		};
	}
	
	/**
	 * Before.
	 *
	 * @param before the before
	 * @return the smart label
	 */
	default SmartLabel<B> before(Runnable before) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			before.run();
			sl.get().accept(bPhony, o);
		};
	}

	
	
}
