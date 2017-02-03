package com.aegisql.conveyor.utils.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.loaders.PartLoader;

// TODO: Auto-generated Javadoc
/**
 * The Class SimpleScheduler.
 *
 * @param <K> the key type
 */
public class SimpleScheduler<K> extends AssemblingConveyor<K, Schedule, SchedulableClosure> {

	/**
	 * Instantiates a new simple scheduler.
	 */
	public SimpleScheduler() {
		super();
		this.setName("SchedulingConveyor");
		this.setIdleHeartBeat(1, TimeUnit.SECONDS);
		this.setResultConsumer(bin -> {
			LOG.debug("Task complete {}",bin);
		});
		this.setScrapConsumer(bin -> {
			LOG.error("Scheduled event failure {}", bin);
		});
		this.addCartBeforePlacementValidator(cart -> {
			Objects.requireNonNull(cart.getValue());
		});
		this.addCartBeforePlacementValidator(cart -> {
			if (! ((cart instanceof CreatingCart) || (cart.getValue() instanceof SchedulableClosure)) ) {
				throw new IllegalArgumentException(
						"Scheduler accepts only SchedulableClosure values, but received " + cart.getValue().getClass());
			}
		});
		this.enablePostponeExpiration(true);
		this.enablePostponeExpirationOnTimeout(true);
	}
	
	@Override
	public <X> PartLoader<K, Schedule, X, SchedulableClosure, Boolean> part() {
		return new PartLoader<K,Schedule,X,SchedulableClosure,Boolean>(cl -> {
			ScheduleBuilder<K> builder = new ScheduleBuilder<K>(cl.ttlMsec);
			BuilderSupplier<SchedulableClosure> bs = BuilderSupplier.of(builder);
			CompletableFuture<Boolean> f1 = build().id(cl.key).expirationTime(cl.expirationTime).supplier(bs).create();
			CompletableFuture<Boolean> f2 = place(new ShoppingCart<K,Object,Schedule>(cl.key, cl.partValue, cl.label, cl.expirationTime));
			return f1.thenCombine(f2, (v1,v2)->
				{
					return v1 && v2;
				}
			);
		});
	}

//	@Override
//	public <X> PartLoader<K, Schedule, X, SchedulableClosure, Boolean> part() {
//		return new PartLoader<K,Schedule,X,SchedulableClosure,Boolean>(cl -> {
//			SchedulableClosure closure = (SchedulableClosure)cl.partValue;
//			Schedule label = cl.label;
//			switch (label) {
//			case SCHEDULE_AND_EXECUTE_NOW:
//				final AtomicBoolean firstRun = new AtomicBoolean(true);
//				closure = () -> {
//					if( firstRun.get() ) {
//						((SchedulableClosure) cl.partValue).apply();
//						firstRun.set( false );
//					} else {
//						place(new ShoppingCart<K,Object,Schedule>(cl.key, cl.partValue, Schedule.SCHEDULE_WITH_DELAY, cl.expirationTime));
//					}
//				};
//				break;
//			case SCHEDULE_WITH_DELAY:
//				closure = closure.andThen(() -> {
//					place(new ShoppingCart<K,Object,Schedule>(cl.key, cl.partValue, Schedule.SCHEDULE_WITH_DELAY, cl.expirationTime));
//					//add(key, value, label, executionTime);
//				});
//				break;
//			default:
//				break;
//			}
//			return place(new ShoppingCart<K,Object,Schedule>(cl.key, cl.partValue, cl.label, cl.expirationTime));
//		});
//	}
//
//	
//	protected CompletableFuture<Boolean> add(K key, SchedulableClosure value, Schedule label, long executionTime) {
//		SchedulableClosure closure = (SchedulableClosure) value;
//		switch (label) {
//		case SCHEDULE_AND_EXECUTE_NOW:
//			final AtomicBoolean firstRun = new AtomicBoolean(true);
//			closure = () -> {
//				if( firstRun.get() ) {
//					((SchedulableClosure) value).apply();
//					firstRun.set( false );
//				} else {
//					add(key, value, Schedule.SCHEDULE_WITH_DELAY, executionTime);
//				}
//			};
//			break;
//		case SCHEDULE_WITH_DELAY:
//			closure = closure.andThen(() -> {
//				add(key, value, label, executionTime);
//			});
//			break;
//		default:
//			break;
//		}
//		return super.place(new ShoppingCart<K,SchedulableClosure,Schedule>(key,closure, label, executionTime));
//	}
//
//	protected CompletableFuture<Boolean> add(K key, SchedulableClosure value, Schedule label, Duration duration) {
//		SchedulableClosure closure = (SchedulableClosure) value;
//		switch (label) {
//		case SCHEDULE_AND_EXECUTE_NOW:
//			final AtomicBoolean firstRun = new AtomicBoolean(true);
//			closure = () -> {
//				if( firstRun.get() ) {
//					((SchedulableClosure) value).apply();
//					firstRun.set( false );
//				} else {
//					add(key, value, Schedule.SCHEDULE_WITH_DELAY, duration);
//				}
//			};
//			break;
//		case SCHEDULE_WITH_DELAY:
//			closure = closure.andThen(() -> {
//				add(key, value, label, duration);
//			});
//			break;
//		default:
//			break;
//		}
//		return super.place(new ShoppingCart<K,SchedulableClosure,Schedule>(key,closure, label, duration));
//	}
//
//	protected CompletableFuture<Boolean> add(K key, SchedulableClosure value, Schedule label, Instant instant) {
//		if( ! label.equals(Schedule.EXECUTE_ONCE)) {
//			LOG.warn("Add without TTL or Duration can only be executed once, while you requested {}",label);
//		}
//		return super.place(new ShoppingCart<K,SchedulableClosure,Schedule>(key, value, label, instant));
//	}
	

}
