package com.aegisql.conveyor.delay;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.Expireable;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayBox.
 *
 * @param <K> the key type
 */
public class DelayBox <K> implements Delayed, Expireable {

	/** The keys. */
	private final Set<K> keys = new LinkedHashSet<>();
	
	/**
	 * Adds the.
	 *
	 * @param key the key
	 */
	public void add(K key) {
		keys.add(key);
	}

	/**
	 * Delete.
	 *
	 * @param key the key
	 */
	public void delete(K key) {
		keys.remove(key);
	}

	/**
	 * Gets the keys.
	 *
	 * @return the keys
	 */
	public Collection<K> getKeys() {
		return keys;
	}
	
	/** The expiration time. */
	private final long expirationTime;
	
	/** The delayed. */
	private final Delayed delayed;
	
	/**
	 * Instantiates a new delay box.
	 *
	 * @param expirationTime the expiration time
	 */
	DelayBox(long expirationTime) {
		this.expirationTime = expirationTime;
		this.delayed        = Expireable.toDelayed(this);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		return delayed.compareTo(o);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Expireable#getExpirationTime()
	 */
	@Override
	public long getExpirationTime() {
		return expirationTime;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return delayed.getDelay(unit);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (expirationTime ^ (expirationTime >>> 32));
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
		DelayBox other = (DelayBox) obj;
		if (expirationTime != other.expirationTime)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DelayBox [keys=" + keys.size() + ", expirationTime=" + expirationTime + "]";
	}

	
	
}
