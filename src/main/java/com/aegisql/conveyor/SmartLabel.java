package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public interface SmartLabel<B,V> {
	BiConsumer<B, V> getSetter();
}
