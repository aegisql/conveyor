package com.aegisql.conveyor.utils.schedule;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
	

}
