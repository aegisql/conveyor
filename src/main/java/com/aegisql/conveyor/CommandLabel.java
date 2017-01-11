/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.cart.Cart;

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
			return (c,o)->AssemblingConveyor.createNow(c, (Cart)o);
		}
	},
	
	/** The cancel build. */
	CANCEL_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.cancelNow(c, (Cart)o);
		}
	},

	/** The acknowledge command. */
	ACK_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.acknowledge(c, (Cart)o);
		}
	},

	/** The timeout build. */
	TIMEOUT_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.timeoutNow(c, (Cart)o);
		}
	},

	/** Reschedule build. */
	RESCHEDULE_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.rescheduleNow(c, (Cart)o);
		}
	},
	
	/**  Check build readiness. */
	CHECK_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.checkBuild(c, (Cart)o);
		}
	},

	/**  iterate foreach build in the queue. */
	FOREACH_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.foreachBuild(c, (Cart)o);
		}
	},

	/**  Check build readiness. */
	FUTURE_BUILD {
		public BiConsumer<AssemblingConveyor, Object> get() {
			return (c,o)->AssemblingConveyor.futureBuild(c, (Cart)o);
		}
	};

	/**
	 *  The setter.
	 *
	 * @return the bi consumer
	 */
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<AssemblingConveyor, Object> get() {
		throw new AbstractMethodError("Unimplemented");
	}
	
}
