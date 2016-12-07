package com.aegisql.conveyor;

import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class SmartWrapper.
 *
 * @param <L> the generic type
 * @param <B> the generic type
 * @param <U> the generic type
 */
public class SmartWrapper<L,B,U> implements SmartLabel<B> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6276204389815296996L;

	/** The label. */
	private final L label;
	
	/** The consumer. */
	private final BiConsumer<B,U> consumer;

	/**
	 * Instantiates a new smart wrapper.
	 *
	 * @param label the label
	 * @param consumer the consumer
	 */
	public SmartWrapper(L label,BiConsumer<B,U> consumer) {
		this.label    = label;
		this.consumer = consumer;
	}

	/**
	 * Unwrap.
	 *
	 * @return the l
	 */
	public L unwrap() {
		return label;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#get()
	 */
	@Override
	public BiConsumer<B, Object> get() {
		return (BiConsumer<B, Object>) consumer;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SmartLabel[" + label + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
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
		if (consumer == null) {
			if (other.consumer != null)
				return false;
		} else if (!consumer.equals(other.consumer))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}

	
	
}
