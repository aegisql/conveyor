package com.aegisql.conveyor.delay;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayProvider.
 *
 * @param <K> the key type
 */
public class DelayProvider <K> {

	/** The boxes. */
	private final Map<Long,DelayBox<K>> boxes = new HashMap<>();
	
	/** The queue. */
	private final DelayQueue<DelayBox<K>> queue = new DelayQueue<>();
	
	/**
	 * Gets the box.
	 *
	 * @param expirationTime the expiration time
	 * @return the box
	 */
	public DelayBox<K> getBox(Long expirationTime) {
		var box = boxes.get(expirationTime);
		if(box == null) {
			box = new DelayBox<>(expirationTime);
			boxes.put(expirationTime, box);
			queue.add(box);
		}
		return box;
	}
	
	/**
	 * Gets the all expired keys.
	 *
	 * @return the all expired keys
	 */
	public List<K> getAllExpiredKeys() {
		var expired = new LinkedList<K>();
		DelayBox<K> box = null;
		while( (box = queue.poll()) != null ) {
			expired.addAll(box.getKeys());
			boxes.remove(box.getExpirationTime());
		}
		return expired;
	}

	/**
	 * Delayed size.
	 *
	 * @return the int
	 */
	public int delayedSize() {
		return queue.size();
	}
	
	/**
	 * Clear.
	 */
	public void clear() {
		boxes.clear();
		queue.clear();
	}
	
}
