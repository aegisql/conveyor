package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public interface SmartLabel<L> {
	BiConsumer<L, Object> getSetter();
}
