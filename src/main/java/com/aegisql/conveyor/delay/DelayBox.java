package com.aegisql.conveyor.delay;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.Expireable;

public class DelayBox <K> implements Delayed, Expireable {

	private final Set<K> keys = new LinkedHashSet<>();
	
	public void add(K key) {
		keys.add(key);
	}

	public void delete(K key) {
		keys.remove(key);
	}

	public Collection<K> getKeys() {
		return keys;
	}
	
	private final long expirationTime;
	
	private final Delayed delayed;
	
	DelayBox(long expirationTime) {
		this.expirationTime = expirationTime;
		this.delayed        = Expireable.toDelayed(this);
	}
	
	@Override
	public int compareTo(Delayed o) {
		return delayed.compareTo(o);
	}

	@Override
	public long getExpirationTime() {
		return expirationTime;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return delayed.getDelay(unit);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (expirationTime ^ (expirationTime >>> 32));
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
		DelayBox other = (DelayBox) obj;
		if (expirationTime != other.expirationTime)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DelayBox [keys=" + keys.size() + ", expirationTime=" + expirationTime + "]";
	}

	
	
}
