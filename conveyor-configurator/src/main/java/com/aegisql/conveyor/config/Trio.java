package com.aegisql.conveyor.config;

public class Trio<L,V,T> {
	final L label;
	final V value1;
	final T value2;
	
	public Trio(L label, V value, T value2) {
		this.label  = label;
		this.value1  = value;
		this.value2 = value2;
	}

	@Override
	public String toString() {
		return "Trio [" + (label != null ? "label=" + label + ", " : "")
				+ (value1 != null ? "value1=" + value1 + ", " : "") + (value2 != null ? "value2=" + value2 : "") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
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
