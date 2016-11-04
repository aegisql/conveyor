package com.aegisql.conveyor.utils.schedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aegisql.conveyor.AssemblingConveyor;

public class SimpleScheduler<K> extends AssemblingConveyor<K, Schedule, SchedulableClosure> {

	public SimpleScheduler() {
		super();
		this.setName("SchedulingConveyor");
		this.setBuilderSupplier(ScheduleBuilder::new);
		this.setIdleHeartBeat(1, TimeUnit.SECONDS);
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
	public <V> CompletableFuture<Boolean> add(K key, V value, Schedule label, long ttl, TimeUnit unit) {
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
	public <V> CompletableFuture<Boolean> add(K key, V value, Schedule label, Duration duration) {
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
	public <V> CompletableFuture<Boolean> offer(K key, V value, Schedule label, long ttl, TimeUnit unit) {
		return add(key,value,label,ttl,unit);
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, Schedule label, Duration duration) {
		return add(key,value,label,duration);
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, Schedule label) {
		throw new UnsupportedOperationException("Scheduler must have execution interval parameter");
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, Schedule label, long expirationTime) {
		if( ! label.equals(Schedule.EXECUTE_ONCE)) {
			LOG.warn("Add without TTL or Duration can only be executed once, while you requested {}",label);
		}
		return super.add(key, value, Schedule.EXECUTE_ONCE, expirationTime);
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, Schedule label, Instant instant) {
		if( ! label.equals(Schedule.EXECUTE_ONCE)) {
			LOG.warn("Add without TTL or Duration can only be executed once, while you requested {}",label);
		}
		return super.add(key, value, Schedule.EXECUTE_ONCE, instant);
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, Schedule label) {
		throw new UnsupportedOperationException("Scheduler must have execution interval parameter");
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, Schedule label, long expirationTime) {
		if( ! label.equals(Schedule.EXECUTE_ONCE)) {
			LOG.warn("Offer without TTL or Duration can only be executed once, while you requested {}",label);
		}
		return super.offer(key, value, Schedule.EXECUTE_ONCE, expirationTime);
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, Schedule label, Instant instant) {
		if( ! label.equals(Schedule.EXECUTE_ONCE)) {
			LOG.warn("Offer without TTL or Duration can only be executed once, while you requested {}",label);
		}
		return super.offer(key, value, Schedule.EXECUTE_ONCE, instant);
	}

	
}
