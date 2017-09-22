package com.aegisql.conveyor.cart;

import java.io.Serializable;

import com.aegisql.conveyor.serial.SerializablePredicate;

public class Load <K,V> implements Serializable {

	private static final long serialVersionUID = 1L;
	private final V value;
	private final SerializablePredicate<K> filter;
	private final LoadType loadType;
	
	public Load(V value, SerializablePredicate<K> filter, LoadType loadType) {
		this.value    = value;
		this.filter   = filter;
		this.loadType = loadType;
	}

	public V getValue() {
		return value;
	}

	public SerializablePredicate<K> getFilter() {
		return filter;
	}

	public LoadType getLoadType() {
		return loadType;
	}

	@Override
	public String toString() {
		return "Load [value=" + value + ", loadType=" + loadType + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loadType == null) ? 0 : loadType.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Load other = (Load) obj;
		if (loadType != other.loadType)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	
}