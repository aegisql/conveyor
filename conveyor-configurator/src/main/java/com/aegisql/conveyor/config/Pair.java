package com.aegisql.conveyor.config;

// TODO: Auto-generated Javadoc
/**
 * The Class Pair.
 *
 * @param <L> the generic type
 * @param <V> the value type
 */
public class Pair<L,V> {
	
	/** The label. */
	final L label;
	
	/** The value. */
	final V value;
	
	/**
	 * Instantiates a new pair.
	 *
	 * @param label the label
	 * @param value the value
	 */
	public Pair(L label, V value) {
		this.label = label;
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Pair[" + label + " = " + value + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	

}
