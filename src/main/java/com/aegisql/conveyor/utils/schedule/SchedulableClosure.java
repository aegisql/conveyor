package com.aegisql.conveyor.utils.schedule;

// TODO: Auto-generated Javadoc
/**
 * The Interface SchedulableClosure.
 */
@FunctionalInterface
public interface SchedulableClosure {
	
	/**
	 * Apply.
	 */
	public void apply();
	
	/**
	 * And then.
	 *
	 * @param next the next
	 * @return the schedulable closure
	 */
	default SchedulableClosure andThen(SchedulableClosure next) {
		return () -> {
			this.apply();
			next.apply();
		};
	}

	/**
	 * Compose.
	 *
	 * @param next the next
	 * @return the schedulable closure
	 */
	default SchedulableClosure compose(SchedulableClosure next) {
		return () -> {
			next.apply();
			this.apply();
		};
	}

}
