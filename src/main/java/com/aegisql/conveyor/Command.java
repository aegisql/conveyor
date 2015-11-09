package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public enum Command  implements SmartLabel<AssemblingConveyor> {
	
	REMOVE_KEY(AssemblingConveyor::removeNow),
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
