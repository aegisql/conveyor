package com.aegisql.conveyor.utils.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.Cart;

public class SimpleScheduler<K> extends AssemblingConveyor<K, Schedule, SchedulableClosure> {

	public SimpleScheduler() {
		super();
		this.setName("SchedulingConveyor");
		this.setBuilderSupplier(ScheduleBuilder::new);
		this.setExpirationCollectionIdleInterval(1, TimeUnit.SECONDS);
		this.setResultConsumer(bin -> {
			bin.product.apply();
		});
		this.setScrapConsumer(bin -> {
			LOG.error("Scheduled event failure {}", bin);
		});
		this.setKeepCartsOnSite(true);
		this.addCartBeforePlacementValidator(cart -> {
			Objects.requireNonNull(cart.getValue());
		});
		this.addCartBeforePlacementValidator(cart -> {
			if (!(cart.getValue() instanceof SchedulableClosure)) {
				throw new IllegalArgumentException(
						"Scheduler accepts only SchedulableClosure values, but received " + cart.getValue().getClass());
			}
		});
	}

	@Override
	public <V> boolean add(K key, V value, Schedule label, long ttl, TimeUnit unit) {
		SchedulableClosure closure = (SchedulableClosure) value;
		switch (label) {
		case SCHEDULE_AND_EXECUTE_NOW:
			final AtomicBoolean firstRun = new AtomicBoolean(true);
			closure = () -> {
				if( firstRun.get() ) {
					((SchedulableClosure) value).apply();
					firstRun.set( false );
				} else {
					add(key, value, Schedule.SCHEDULE_WITH_DELAY, ttl, unit);
				}
			};
			break;
		case SCHEDULE_WITH_DELAY:
			closure = closure.andThen(() -> {
				add(key, value, label, ttl, unit);
			});
			break;
		default:
			break;
		}
		return super.add(key, closure, label, ttl, unit);
	}

	@Override
	public <V> boolean add(K key, V value, Schedule label, Duration duration) {
		SchedulableClosure closure = (SchedulableClosure) value;
		switch (label) {
		case SCHEDULE_AND_EXECUTE_NOW:
			final AtomicBoolean firstRun = new AtomicBoolean(true);
			closure = () -> {
				if( firstRun.get() ) {
					((SchedulableClosure) value).apply();
					firstRun.set( false );
				} else {
					add(key, value, Schedule.SCHEDULE_WITH_DELAY, duration);
				}
			};
			break;
		case SCHEDULE_WITH_DELAY:
			closure = closure.andThen(() -> {
				add(key, value, label, duration);
			});
			break;
		default:
			break;
		}
		return super.add(key, closure, label, duration);
	}

	@Override
	public <V> boolean offer(K key, V value, Schedule label, long ttl, TimeUnit unit) {
		try {
			return add(key,value,label,ttl,unit);
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public <V> boolean offer(K key, V value, Schedule label, Duration duration) {
		try {
			return add(key,value,label,duration);
		} catch(Exception e) {
			return false;
		}
	}

}
