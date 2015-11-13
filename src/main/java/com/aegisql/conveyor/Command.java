package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public enum Command  implements SmartLabel<AssemblingConveyor> {
	
	CREATE_BUILD(AssemblingConveyor::createNow),
	CANCEL_BUILD(AssemblingConveyor::cancelNow),
	TIMEOUT_BUILD(AssemblingConveyor::timeoutNow);

	Command(BiConsumer<AssemblingConveyor, Object> setter) {
		this.setter = setter;
	}
	
	BiConsumer<AssemblingConveyor, Object> setter;
	
	@Override
	public BiConsumer<AssemblingConveyor, Object> getSetter() {
		return setter;
	}
	
}
