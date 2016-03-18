package com.aegisql.conveyor.utils.schedule;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum Schedule implements SmartLabel<ScheduleBuilder<?>> {
	 EXECUTE_ONCE(ScheduleBuilder::setClosureOnce)
	,SCHEDULE_WITH_DELAY(ScheduleBuilder::setClosure)
	,SCHEDULE_AND_EXECUTE_NOW(ScheduleBuilder::setAndExecuteClosure)
	;

	BiConsumer<ScheduleBuilder<?>,Object> setter;
	
	<T> Schedule(BiConsumer<ScheduleBuilder<?>,T> setter) {
		this.setter = (BiConsumer<ScheduleBuilder<?>, Object>) setter;
	}
	
	@Override
	public BiConsumer<ScheduleBuilder<?>,Object> get() {
		return setter;
	}
}
