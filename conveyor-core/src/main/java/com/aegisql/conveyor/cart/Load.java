package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.serial.SerializablePredicate;

import java.io.Serial;
import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Class Load.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class Load <K,V> implements Serializable {

	/** The Constant serialVersionUID. */
	@Serial
    private static final long serialVersionUID = 1L;
	
	/** The value. */
	private final V value;
	
	/** The filter. */
	private final SerializablePredicate<K> filter;
	
	/** The load type. */
	private final LoadType loadType;
	
	/**
	 * Instantiates a new load.
	 *
	 * @param value the value
	 * @param filter the filter
	 * @param loadType the load type
	 */
	public Load(V value, SerializablePredicate<K> filter, LoadType loadType) {
		this.value    = value;
		this.filter   = filter;
		this.loadType = loadType;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public V getValue() {
		return value;
	}

	/**
	 * Gets the filter.
	 *
	 * @return the filter
	 */
	public SerializablePredicate<K> getFilter() {
		return filter;
	}

	/**
	 * Gets the load type.
	 *
	 * @return the load type
	 */
	public LoadType getLoadType() {
		return loadType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Load [value=" + value + ", loadType=" + loadType + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loadType == null) ? 0 : loadType.hashCode());
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
		Load other = (Load) obj;
		if (loadType != other.loadType)
			return false;
		if (value == null) {
			return other.value == null;
		} else return value.equals(other.value);
	}

	
}