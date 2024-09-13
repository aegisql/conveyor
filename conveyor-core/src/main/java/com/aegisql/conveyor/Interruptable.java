package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Interface Interruptable.
 */
public interface Interruptable {
	
	/**
	 * Interrupt.
	 *
	 * @param conveyorThread the conveyor thread
	 */
	void interrupt(Thread conveyorThread);

}
