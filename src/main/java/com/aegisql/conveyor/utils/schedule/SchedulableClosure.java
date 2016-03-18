package com.aegisql.conveyor.utils.schedule;

@FunctionalInterface
public interface SchedulableClosure {
	public void apply();
	
	default SchedulableClosure andThen(SchedulableClosure next) {
		return () -> {
			this.apply();
			next.apply();
		};
	}

	default SchedulableClosure compose(SchedulableClosure next) {
		return () -> {
			next.apply();
			this.apply();
		};
	}

}
