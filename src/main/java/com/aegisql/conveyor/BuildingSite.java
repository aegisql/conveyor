package com.aegisql.conveyor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Builder<OUT>, Delayed {

	private final Builder<OUT> builder;
	private final BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer;
	
	private int acceptCount = 0;
	private final long builderCreated;
	private final long builderExpiration;

	
	public BuildingSite( Supplier<Builder<OUT>> builderSupplier, BiConsumer<Lot<K, ?, L>, Builder<OUT>> cartConsumer) {
		this.builder = builderSupplier.get() ;
		this.cartConsumer = cartConsumer;
		builderCreated = System.currentTimeMillis();
		builderExpiration = 0;
	}
	
	public void accept(C cart) {
		
		Lot<K, ?, L> lot = new Lot<>(
				cart.getKey(),
				cart.getValue(), 
				cart.getLabel(),
				builderCreated,
				0,
				cart.getCreationTime(),
				cart.getExpirationTime(),
				acceptCount
				);
		
		cartConsumer.accept(lot, builder);
		acceptCount++;
	}

	@Override
	public OUT build() {
		if( ! ready() ) {
			throw new IllegalStateException("Builder is not ready!");
		}
		return builder.build();
	}

	@Override
	public boolean ready() {
		return builder.ready();
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
}
