package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public enum Command  implements SmartLabel<AssemblingConveyor> {
	
	CANCEL_KEY(AssemblingConveyor::cancelNow),
	TIMEOUT_KEY(AssemblingConveyor::timeoutNow);

	Command(BiConsumer<AssemblingConveyor, Object> setter) {
		this.setter = setter;
	}
	
	BiConsumer<AssemblingConveyor, Object> setter;
	
	@Override
	public BiConsumer<AssemblingConveyor, Object> getSetter() {
		return setter;
	}
	
}
