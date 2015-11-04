package com.aegisql.conveyor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Delayed {

	public static enum Status{
		WAITING,TIMEED_OUT,READY,INVALID;
	}
	
	private final Builder<OUT> builder;
	private final LabeledValueConsumer<L, Object, Builder<OUT>> valueConsumer;
	private final BiFunction<Lot<K>, Builder<OUT>, Boolean> ready;
	
	private final  C initialCart;
	
	private int acceptCount = 0;
	private final long builderCreated;
	private final long builderExpiration;
	
	private Status status = Status.WAITING;
	private Throwable lastError;

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer, BiFunction<Lot<K>, Builder<OUT>, Boolean> ready) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Builder<OUT>>) cartConsumer;
		this.ready = ready;
		builderCreated = System.currentTimeMillis();
		builderExpiration = 0;
	}

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer, BiFunction<Lot<K>, Builder<OUT>, Boolean> ready, long expiration) {		
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Builder<OUT>>) cartConsumer;
		this.ready = ready;
		builderCreated = System.currentTimeMillis();
		builderExpiration = expiration;
	}

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer, BiFunction<Lot<K>, Builder<OUT>, Boolean> ready, long ttl, TimeUnit unit) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Builder<OUT>>) cartConsumer;
		this.ready = ready;
		builderCreated = System.currentTimeMillis();
		builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
	}

	public void accept(C cart) {
		Lot<K> lot = null;
		L label = null;
		Object value = null;
		if(cart == null) {
			 lot = new Lot<>(
						initialCart.getKey(),
						builderCreated,
						builderExpiration,
						initialCart.getCreationTime(),
						initialCart.getExpirationTime(),
						acceptCount
						);
			
		} else {
			 lot = new Lot<>(
						cart.getKey(),
						builderCreated,
						builderExpiration,
						cart.getCreationTime(),
						cart.getExpirationTime(),
						acceptCount
						);
			 label = cart.getLabel();
			 value = cart.getValue();
		}
		
		valueConsumer.accept(label, value, builder);
		acceptCount++;
	}

	public OUT build() {
		if( ! ready() ) {
			throw new IllegalStateException("Builder is not ready!");
		}
		return builder.build();
	}

	public boolean ready() {
		Lot<K> lot = null;
		 lot = new Lot<>(
					initialCart.getKey(),
					builderCreated,
					builderExpiration,
					initialCart.getCreationTime(),
					initialCart.getExpirationTime(),
					acceptCount
					);
		return ready.apply(lot, builder);
	}

	public int getAcceptCount() {
		return acceptCount;
	}

	@Override
	public int compareTo(Delayed o) {
		if( this.builderExpiration == ((BuildingSite<?,?,?,?>)o).builderExpiration) return 0;
		if( this.builderExpiration == 0 ) return 1;
		if( ((BuildingSite<?,?,?,?>)o).builderExpiration == 0 ) return -1;
		if( this.builderExpiration < ((BuildingSite<?,?,?,?>)o).builderExpiration) return -1;
		if( this.builderExpiration > ((BuildingSite<?,?,?,?>)o).builderExpiration) return +1;
		return 0;
	}

	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( this.builderExpiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = this.builderExpiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}

	public boolean expired() {
		return builderExpiration > 0 && builderExpiration <= System.currentTimeMillis();
	}

	public long getBuilderExpiration() {
		return builderExpiration;
	}

	public C getCart() {
		return initialCart;
	}

	public K getKey() {
		return initialCart.getKey();
	}
	
	public Throwable getLastError() {
		return lastError;
	}

	public void setLastError(Throwable lastError) {
		this.lastError = lastError;
	}

	@Override
	public String toString() {
		return "BuildingSite [" + (builder != null ? "builder=" + builder + ", " : "")
				+ (valueConsumer != null ? "cartConsumer=" + valueConsumer + ", " : "")
				+ (initialCart != null ? "initialCart=" + initialCart + ", " : "") + "acceptCount=" + acceptCount
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", "
				+ (lastError != null ? "lastError=" + lastError : "") + "]";
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	
	
}
