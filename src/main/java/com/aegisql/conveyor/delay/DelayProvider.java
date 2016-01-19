package com.aegisql.conveyor.delay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;

public class DelayProvider <K> {

	private final Map<Long,DelayBox<K>> boxes = new HashMap<>();
	
	private final DelayQueue<DelayBox<K>> queue = new DelayQueue<>();
	
	public DelayBox<K> getBox(Long expirationTime) {
		DelayBox<K> box = boxes.get(expirationTime);
		if(box == null) {
			box = new DelayBox<>(expirationTime);
			boxes.put(expirationTime, box);
			queue.add(box);
		}
		return box;
	}
	
	List<K> getAllExpiredKeys() {
		List<K> expired = new ArrayList<>();
		DelayBox<K> box = null;
		while( (box = queue.poll()) != null ) {
			expired.addAll(box.getKeys());
			boxes.remove(box.getExpirationTime());
		}
		return expired;
	}

	public int delayedSize() {
		return queue.size();
	}
	
}
