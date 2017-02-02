package com.aegisql.conveyor.utils.schedule;

import java.util.function.Supplier;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.TestingState;
import com.aegisql.conveyor.TimeoutAction;

// TODO: Auto-generated Javadoc
/**
 * The Class ScheduleBuilder.
 *
 * @param <K> the key type
 */
public class ScheduleBuilder<K> implements Supplier<SchedulableClosure>, TimeoutAction, TestingState<K, Schedule>, Expireable{

	/** The closure. */
	private SchedulableClosure closure;
	
	/** The ready. */
	private boolean ready      = false;
	
	/** The reschedule. */
	private boolean reschedule = false;
	
	/**
	 * Instantiates a new schedule builder.
	 */
	
	private long expTime = 0;
	private long ttl = 0;
	
	public ScheduleBuilder(long ttl) {
		this.expTime = ttl+System.currentTimeMillis();
		this.ttl = ttl;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public SchedulableClosure get() {
		return closure;
	}
	
	/**
	 * Sets the closure.
	 *
	 * @param builder the builder
	 * @param closure the closure
	 */
	public static void setClosure(ScheduleBuilder builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(true);
	}

	/**
	 * Sets the and execute closure.
	 *
	 * @param <K> the key type
	 * @param builder the builder
	 * @param closure the closure
	 */
	public static <K> void setAndExecuteClosure(ScheduleBuilder<K> builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(true);
		builder.closure.apply();
	}

	/**
	 * Sets the closure once.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param closure the closure
	 */
	public static <T> void setClosureOnce(ScheduleBuilder<T> builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(false);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.TimeoutAction#onTimeout()
	 */
	@Override
	public void onTimeout() {
		closure.apply();
		if( reschedule ) {
			this.expTime = ttl+System.currentTimeMillis();
			ready = false;			
		} else {
			ready = true;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.function.Predicate#test(java.lang.Object)
	 */
	@Override
	public boolean test(State<K, Schedule> t) {
		return ready;
	}

	/**
	 * Checks if is reschedule.
	 *
	 * @return true, if is reschedule
	 */
	public boolean isReschedule() {
		return reschedule;
	}

	/**
	 * Sets the reschedule.
	 *
	 * @param reschedule the new reschedule
	 */
	public void setReschedule(boolean reschedule) {
		this.reschedule = reschedule;
	}

	@Override
	public long getExpirationTime() {
		return expTime;
	}

}
