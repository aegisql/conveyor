package com.aegisql.conveyor.config;

// TODO: Auto-generated Javadoc
/**
 * The Class Trio.
 *
 * @param <L> the generic type
 * @param <V> the value type
 * @param <T> the generic type
 */
public class Trio<L,V,T> {
	
	/** The label. */
	final L label;
	
	/** The value 1. */
	final V value1;
	
	/** The value 2. */
	final T value2;
	
	/**
	 * Instantiates a new trio.
	 *
	 * @param label the label
	 * @param value the value
	 * @param value2 the value 2
	 */
	public Trio(L label, V value, T value2) {
		this.label  = label;
		this.value1  = value;
		this.value2 = value2;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Trio [" + (label != null ? "label=" + label + ", " : "")
				+ (value1 != null ? "value1=" + value1 + ", " : "") + (value2 != null ? "value2=" + value2 : "") + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
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
		Trio other = (Trio) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (value1 == null) {
			if (other.value1 != null)
				return false;
		} else if (!value1.equals(other.value1))
			return false;
		if (value2 == null) {
			if (other.value2 != null)
				return false;
		} else if (!value2.equals(other.value2))
			return false;
		return true;
	}

}
