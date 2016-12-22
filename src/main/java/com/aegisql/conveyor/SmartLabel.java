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
	
	static <B,T> SmartLabel<B> of(BiConsumer<B, T> method) {
		return ()->(b,t)->method.accept(b,(T)t);
	}
	static <B> SmartLabel<B> of(Consumer<B> method) {
		return () -> {
			return (b,oPhony) -> method.accept(b);
		};
	}
	static <B> SmartLabel<B> of(Runnable method) {
		return () -> {
			return (bPhoby,oPhony) -> method.run();
		};
	}
	default SmartLabel<B> identity() {
		return this;
	}
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
	default <T> SmartLabel<B> andThen(BiConsumer<B,T> after) {
		SmartLabel<B> sl = this;
		return ()->(b,o) -> {
			sl.get().accept(b, o);
			after.accept(b, (T)o);
		};
	}
	default <T> SmartLabel<B> before(BiConsumer<B,T> before) {
		SmartLabel<B> sl = this;
		return ()->(b,o) -> {
			before.accept(b, (T)o);
			sl.get().accept(b, o);
		};
	}
	default <T> SmartLabel<B> andThen(Consumer<T> after) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.accept((T)o);
		};
	}
	default <T> SmartLabel<B> before(Consumer<T> before) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			before.accept((T)o);
			sl.get().accept(bPhony, o);
		};
	}
	default SmartLabel<B> andThen(Runnable after) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			sl.get().accept(bPhony, o);
			after.run();
		};
	}
	default SmartLabel<B> before(Runnable before) {
		SmartLabel<B> sl = this;
		return ()->(bPhony,o) -> {
			before.run();
			sl.get().accept(bPhony, o);
		};
	}

	
	
}
