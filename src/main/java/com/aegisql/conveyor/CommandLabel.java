/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Enum Command.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
*/
public enum CommandLabel  implements SmartLabel<AssemblingConveyor> {
	
	/** The create build. */
	CREATE_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::createNow;
		}
	},
	
	/** The cancel build. */
	CANCEL_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::cancelNow;
		}
	},

	/** The acknowledge command. */
	ACK_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::acknowledge;
		}
	},

	/** The timeout build. */
	TIMEOUT_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::timeoutNow;
		}
	},

	/** Reschedule build. */
	RESCHEDULE_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::rescheduleNow;
		}
	},
	
	/** Check build readiness */
	CHECK_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return AssemblingConveyor::checkBuild;
		}
	};

	/** The setter. */
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<AssemblingConveyor, Object> get() {
		throw new AbstractMethodError("Unimplemented");
	}
	
}
