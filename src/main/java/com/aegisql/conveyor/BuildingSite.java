package com.aegisql.conveyor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Delayed {

	private final Builder<OUT> builder;
	private final BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer;
	private final  C initialCart;
	
	private int acceptCount = 0;
	private final long builderCreated;
	private final long builderExpiration;
	
	private Throwable lastError;

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.cartConsumer = cartConsumer;
		builderCreated = System.currentTimeMillis();
		builderExpiration = 0;
	}

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer, long expiration) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.cartConsumer = cartConsumer;
		builderCreated = System.currentTimeMillis();
		builderExpiration = expiration;
	}

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer, long ttl, TimeUnit unit) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.cartConsumer = cartConsumer;
		builderCreated = System.currentTimeMillis();
		builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
	}

	public void accept(C cart) {
		Lot<K, ?, L> lot = null;
		if(cart == null) {
			 lot = new Lot<>(
						initialCart.getKey(),
						null, 
						null,
						builderCreated,
						builderExpiration,
						initialCart.getCreationTime(),
						initialCart.getExpirationTime(),
						acceptCount
						);
			
		} else {
			 lot = new Lot<>(
						cart.getKey(),
						cart.getValue(), 
						cart.getLabel(),
						builderCreated,
						builderExpiration,
						cart.getCreationTime(),
						cart.getExpirationTime(),
						acceptCount
						);
		}
		
		cartConsumer.accept(lot, builder);
		acceptCount++;
	}

	public OUT build() {
		if( ! ready() ) {
			throw new IllegalStateException("Builder is not ready!");
		}
		return builder.build();
	}

	public boolean ready() {
		Lot<K, ?, L> lot = null;
		 lot = new Lot<>(
					initialCart.getKey(),
					null, 
					null,
					builderCreated,
					builderExpiration,
					initialCart.getCreationTime(),
					initialCart.getExpirationTime(),
					acceptCount
					);
		return builder.ready(lot);
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

	public Throwable getLastError() {
		return lastError;
	}

	public void setLastError(Throwable lastError) {
		this.lastError = lastError;
	}

	@Override
	public String toString() {
		return "BuildingSite [" + (builder != null ? "builder=" + builder + ", " : "")
				+ (cartConsumer != null ? "cartConsumer=" + cartConsumer + ", " : "")
				+ (initialCart != null ? "initialCart=" + initialCart + ", " : "") + "acceptCount=" + acceptCount
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", "
				+ (lastError != null ? "lastError=" + lastError : "") + "]";
	}

	
	
}
