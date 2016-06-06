package com.aegisql.conveyor;

import java.util.function.BiConsumer;

public class SmartWrapper<L,B,U> implements SmartLabel<B> {

	private static final long serialVersionUID = 6276204389815296996L;

	private final L label;
	private final BiConsumer<B,U> consumer;

	public SmartWrapper(L label,BiConsumer<B,U> consumer) {
		this.label = label;
		this.consumer = consumer;
	}
	
	public L unwrap() {
		return label;
	}
	
	@Override
	public BiConsumer<B, Object> get() {
		return (BiConsumer<B, Object>) consumer;
	}

	@Override
	public String toString() {
		return "SmartLabel[" + label + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmartWrapper other = (SmartWrapper) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
}
