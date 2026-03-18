package com.aegisql.conveyor.utils.schedule;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.serial.SerializableBiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Enum Schedule.
 */
public enum Schedule implements SmartLabel<ScheduleBuilder<?>> {
	 
 	/** The execute once. */
 	EXECUTE_ONCE(ScheduleBuilder::setClosureOnce)
	,
/** The schedule with delay. */
SCHEDULE_WITH_DELAY(ScheduleBuilder::setClosure)
	,
/** The schedule and execute now. */
SCHEDULE_AND_EXECUTE_NOW(ScheduleBuilder::setAndExecuteClosure)
	;

	/** The setter. */
	final SerializableBiConsumer<ScheduleBuilder<?>,Object> setter;
	
	/**
	 * Instantiates a new schedule.
	 *
	 * @param <T> the generic type
	 * @param setter the setter
	 */
	<T> Schedule(SerializableBiConsumer<ScheduleBuilder<?>,T> setter) {
		this.setter = (SerializableBiConsumer<ScheduleBuilder<?>, Object>) setter;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public SerializableBiConsumer<ScheduleBuilder<?>,Object> get() {
		return setter;
	}
}
