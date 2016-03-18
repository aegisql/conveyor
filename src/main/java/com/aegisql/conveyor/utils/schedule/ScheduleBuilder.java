package com.aegisql.conveyor.utils.schedule;

import java.util.function.Supplier;

import com.aegisql.conveyor.State;
import com.aegisql.conveyor.TestingState;
import com.aegisql.conveyor.TimeoutAction;

public class ScheduleBuilder <K> implements Supplier<SchedulableClosure>, TimeoutAction, TestingState<K, Schedule> {

	private SchedulableClosure closure;
	private boolean ready      = false;
	private boolean reschedule = false;
	
	public ScheduleBuilder() {
		
	}
	
	@Override
	public SchedulableClosure get() {
		return closure;
	}
	
	public static void setClosure(ScheduleBuilder builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(true);
	}

	public static <K> void setAndExecuteClosure(ScheduleBuilder<K> builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(true);
		builder.closure.apply();
	}

	public static <T> void setClosureOnce(ScheduleBuilder<T> builder, Object closure) {
		builder.closure    = (SchedulableClosure) closure;
		builder.setReschedule(false);
	}

	@Override
	public void onTimeout() {
		ready = true;
	}

	@Override
	public boolean test(State<K, Schedule> t) {
		return ready;
	}

	public boolean isReschedule() {
		return reschedule;
	}

	public void setReschedule(boolean reschedule) {
		this.reschedule = reschedule;
	}

}
