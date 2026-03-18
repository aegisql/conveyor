package com.aegisql.conveyor.utils.schedule;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.loaders.PartLoader;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
		this.resultConsumer().first(bin -> LOG.debug("Task complete {}",bin)).set();
		this.scrapConsumer().first(LogScrap.debug(this)).set();
		this.addCartBeforePlacementValidator(cart -> Objects.requireNonNull(cart.getValue()));
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
	public PartLoader<K, Schedule> part() {
		return new PartLoader<>(cl -> {
            ScheduleBuilder<K> builder = new ScheduleBuilder<>(cl.ttlMsec);
            BuilderSupplier<SchedulableClosure> bs = BuilderSupplier.of(builder);
            CompletableFuture<Boolean> f1 = build().id(cl.key).expirationTime(cl.expirationTime).supplier(bs).create();
            CompletableFuture<Boolean> f2 = place(new ShoppingCart<>(cl.key, cl.partValue, cl.label, cl.expirationTime));
            return f1.thenCombine(f2, (v1, v2) ->
                    v1 && v2
            );
        });
	}
	

}
