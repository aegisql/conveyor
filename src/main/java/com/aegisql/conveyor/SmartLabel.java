package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public interface SmartLabel<B> {
	BiConsumer<B, Object> getSetter();
}
