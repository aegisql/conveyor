/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.serial.SerializableBiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Enum Command.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public enum CommandLabel implements SmartLabel<AssemblingConveyor> {

	/** The create build. */
	CREATE_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.createNow(c, (Cart) o);
		}
	}

	/** The cancel build. */
	,
	CANCEL_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.cancelNow(c, (Cart) o);
		}
	}

	/** The timeout build. */
	,
	TIMEOUT_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.timeoutNow(c, (Cart) o);
		}
	}

	/** Reschedule build. */
	,
	RESCHEDULE_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.rescheduleNow(c, (Cart) o);
		}
	}

	/** Check build readiness. */
	,
	CHECK_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.checkBuild(c, (Cart) o);
		}
	}

	,
	/** The peek build. */
	PEEK_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.peekBuild(c, (Cart) o);
		}
	},
	/** The memento build. */
	MEMENTO_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.mementoBuild(c, (Cart) o);
		}
	},
	/** The restore build. */
	RESTORE_BUILD {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> AssemblingConveyor.restoreBuild(c, (Cart) o);
		}
	},
	
	SUSPEND {
		public SerializableBiConsumer<AssemblingConveyor, Object> get() {
			return (c, o) -> {};
		}
	};

	;

	/**
	 * The setter.
	 *
	 * @return the bi consumer
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public SerializableBiConsumer<AssemblingConveyor, Object> get() {
		throw new AbstractMethodError("Unimplemented");
	}

}
