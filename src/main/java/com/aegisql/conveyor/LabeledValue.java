package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class LabeledValue.
 *
 * @param <L> the generic type
 */
public class LabeledValue<L> {
	
	/** The label. */
	public final L label;
	
	/** The value. */
	public final Object value;
	
	/**
	 * Instantiates a new labeled value.
	 *
	 * @param label the label
	 * @param value the value
	 */
	public LabeledValue(L label, Object value) {
		this.label = label;
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LabeledValue [label=" + label + ", value=" + value + "]";
	}
	
}
