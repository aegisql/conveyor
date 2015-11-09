package com.aegisql.conveyor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Delayed {

	public static enum Status{
		WAITING,TIMEED_OUT,READY,INVALID;
	}
	
	private final Builder<OUT> builder;
	private final LabeledValueConsumer<L, Object, Builder<OUT>> valueConsumer;
	private final BiPredicate<Lot<K>, Builder<OUT>> ready;
	
	private final  C initialCart;
	
	private int acceptCount = 0;
	private final long builderCreated;
	private final long builderExpiration;
	
	private Status status = Status.WAITING;
	private Throwable lastError;
	
	private final Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	Delayed delayKeeper;

	public BuildingSite( C cart, Supplier<Builder<OUT>> builderSupplier, LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer, BiPredicate<Lot<K>, Builder<OUT>> ready, long ttl, TimeUnit unit) {
		this.initialCart = cart;
		this.builder = builderSupplier.get() ;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Builder<OUT>>) cartConsumer;
		if(builder instanceof Predicate) {
			this.ready = (lot,builder) -> {
				return ((Predicate<Lot<K>>)builder).test(lot);
			};
		} else {
			this.ready = ready;
		}
		this.eventHistory.put(cart.getLabel(), new AtomicInteger(1));
		if(builder instanceof Delayed) {
			builderCreated = System.currentTimeMillis();
			builderExpiration = 0;
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					return ((Delayed)builder).compareTo( (Delayed) ((BuildingSite)o).builder );
				}
				@Override
				public long getDelay(TimeUnit unit) {
					return ((Delayed) builder).getDelay(unit);
				}
			};
		} else if ( cart.getExpirationTime() > 0 ) {
			builderCreated = cart.getCreationTime();
			builderExpiration = cart.getExpirationTime();
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					return cart.compareTo( ((BuildingSite)o).initialCart );
				}
				@Override
				public long getDelay(TimeUnit unit) {
					return cart.getDelay(unit);
				}
				
			};
		} else {
			builderCreated = System.currentTimeMillis();
			builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			delayKeeper = new Delayed() {
				@Override
				public int compareTo(Delayed o) {
					if( builderCreated < ((BuildingSite<?,?,?,?>)o).builderCreated) {
						return -1;
					}
					if( builderCreated > ((BuildingSite<?,?,?,?>)o).builderCreated) {
						return +1;
					}
					return 0;
				}

				@Override
				public long getDelay(TimeUnit unit) {
			        long delta;
					if( builderExpiration == 0 ) {
						delta = Long.MAX_VALUE;
					} else {
						delta = builderExpiration - System.currentTimeMillis();
					}
			        return unit.convert(delta, TimeUnit.MILLISECONDS);
				}
			};
		}
		
	}

	public void accept(C cart) {
		L label = null;
		Object value = null;
		if(cart != null) {
			 label = cart.getLabel();
			 value = cart.getValue();
		}
		
		if( label == null || ! (label instanceof SmartLabel) ) {
			valueConsumer.accept(label, value, builder);
		} else {
			((SmartLabel)label).getSetter().accept(builder,value);
		}
		
		if(label != null) {
			acceptCount++;
			if(eventHistory.containsKey(label)) {
				eventHistory.get(label).incrementAndGet();
			} else {
				eventHistory.put(label, new AtomicInteger(1));
			}
		}
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
		return ready.test(lot, builder);
	}

	public int getAcceptCount() {
		return acceptCount;
	}

	@Override
	public int compareTo(Delayed o) {
		return delayKeeper.compareTo(o);
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return this.delayKeeper.getDelay(unit);	}

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
				+ (initialCart != null ? "initialCart=" + initialCart + ", " : "") + "acceptCount=" + acceptCount
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", "
				+ (status != null ? "status=" + status + ", " : "")
				+ (lastError != null ? "lastError=" + lastError + ", " : "")
				+ (eventHistory != null ? "eventHistory=" + eventHistory : "") + "]";
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
}
