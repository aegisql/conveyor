package com.aegisql.conveyor.parallel.utils.task_pool_conveyor;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.serial.SerializablePredicate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc

/**
 * The Class PartLoader.
 *
 * @param <K> the key type
 * @param <L> the generic type
 */
public final class TaskLoader<K,L> {

	/** The placer. */
	private final Function<TaskLoader<K,L>, CompletableFuture<Boolean>> placer;

	/** The creation time. */
	public final long creationTime;

	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/**  The priority. */
	public final long priority;

	/** The key. */
	public final K key;

	/** The key filter. */
	public final SerializablePredicate<K> filter;

	/** The label. */
	public final L label;

	/** The part value. */
	public final Supplier<?> valueSupplier;

	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();

	public final int attempts;

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param priority the priority
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 * @param filter the filter
	 * @param properties the properties
	 * @param attempts the attempts
	 */
	private TaskLoader(
			Function<TaskLoader<K,L>, CompletableFuture<Boolean>> placer,
			long creationTime,
			long expirationTime,
			long ttlMsec,
			long priority,
			K key,
			L label,
			Supplier<?> value,
			SerializablePredicate<K> filter,
			Map<String,Object> properties,
			int attempts) {
		this.placer         = placer;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.key            = key;
		this.label          = label;
		this.valueSupplier = value;
		this.filter         = filter;
		this.priority       = priority;
		this.properties.putAll(properties);
		this.attempts = attempts;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param priority the priority
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 * @param filter the filter
	 * @param properties the properties
	 * @param dumb the dumb
	 */
	@SuppressWarnings("SameParameterValue")
	private TaskLoader(
			Function<TaskLoader<K,L>, CompletableFuture<Boolean>> placer,
			long creationTime,
			long ttl,
			long priority,
			K key,
			L label,
			Supplier<?> value,
			SerializablePredicate<K> filter,
			Map<String,Object> properties,
			int attempts,
			boolean dumb) {
		this(placer,
				creationTime,
				creationTime + ttl,
				ttl,
				priority,
				key,
				label,
				value,
				filter,
				properties,
				attempts);
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public TaskLoader(Function<TaskLoader<K,L>, CompletableFuture<Boolean>> placer) {
		this(placer,System.currentTimeMillis(),0,0,0,null,null,null,null,Collections.EMPTY_MAP,1);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the part loader
	 */
	public TaskLoader<K,L> id(K k) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, k, label, valueSupplier, null/*either id or filter*/, properties, attempts);
	}

	/**
	 * Foreach.
	 *
	 * @return the part loader
	 */
	public TaskLoader<K,L> foreach() {
		return foreach(SerializablePredicate.ANY);
	}

	/**
	 * Foreach.
	 *
	 * @param f the f
	 * @return the part loader
	 */
	public TaskLoader<K,L> foreach(SerializablePredicate<K> f) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, null/*either id or filter*/, label, valueSupplier, f, properties, attempts);
	}
	
	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public TaskLoader<K,L> label(L l) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, l, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Value.
	 *
	 * @param v the v
	 * @return the task loader
	 */
	public TaskLoader<K,L> valueSupplier(Supplier<?> v) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, label, v, filter, properties, attempts);
	}

	/**
	 * Value.
	 *
	 * @param v the v
	 * @return the task loader
	 */
	public TaskLoader<K,L> valueSupplier(Runnable v) {
		return valueSupplier(()->{
			v.run();
			return Void.TYPE;
		});
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public TaskLoader<K,L> expirationTime(long et) {
		return new TaskLoader<>(placer, creationTime, et, 0, priority, key, label, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the part loader
	 */
	public TaskLoader<K,L> creationTime(long ct) {
		return new TaskLoader<>(placer, ct, expirationTime, 0, priority, key, label, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public TaskLoader<K,L> expirationTime(Instant instant) {
		return new TaskLoader<>(placer, creationTime, instant.toEpochMilli(), 0, priority, key, label, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public TaskLoader<K,L> creationTime(Instant instant) {
		return new TaskLoader<>(placer, instant.toEpochMilli(), expirationTime, 0, priority, key, label, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public TaskLoader<K,L> ttl(long time, TimeUnit unit) {
		return new TaskLoader<>(placer, creationTime, TimeUnit.MILLISECONDS.convert(time, unit), priority, key, label, valueSupplier, filter, properties, attempts, true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public TaskLoader<K,L> ttl(Duration duration) {
		return new TaskLoader<>(placer, creationTime, duration.toMillis(), priority, key, label, valueSupplier, filter, properties, attempts, true);
	}

	/**
	 * Priority.
	 *
	 * @param p the priority
	 * @return the part loader
	 */
	public TaskLoader<K,L> priority(long p) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, p, key, label, valueSupplier, filter, properties, attempts);
	}

	public TaskLoader<K,L> attempts(int attempts) {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, label, valueSupplier, filter, properties, attempts);
	}

	/**
	 * Increase priority part loader.
	 *
	 * @return the part loader
	 */
	public TaskLoader<K,L> increasePriority() {
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority + 1, key, label, valueSupplier, filter, properties, attempts);
	}


	/**
	 * Clear properties.
	 *
	 * @return the part loader
	 */
	public TaskLoader<K,L> clearProperties() {
		return new TaskLoader<K,L>(placer,creationTime,expirationTime,ttlMsec,priority,key,label, valueSupplier,filter,Collections.EMPTY_MAP, attempts);
	}

	/**
	 * Clear property.
	 *
	 * @param k the k
	 * @return the part loader
	 */
	public TaskLoader<K,L> clearProperty(String k) {
		Map<String, Object> newMap = new HashMap<>(properties);
		newMap.remove(k);
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, label, valueSupplier, filter, newMap, attempts);
	}

	/**
	 * Adds the property.
	 *
	 * @param k the k
	 * @param v the v
	 * @return the part loader
	 */
	public TaskLoader<K,L> addProperty(String k, Object v) {
		Map<String, Object> newMap = new HashMap<>(properties);
		newMap.put(k, v);
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, label, valueSupplier, filter, newMap, attempts);
	}

	/**
	 * Adds the properties.
	 *
	 * @param moreProperties the more properties
	 * @return the part loader
	 */
	public TaskLoader<K,L> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new TaskLoader<>(placer, creationTime, expirationTime, ttlMsec, priority, key, label, valueSupplier, filter, newMap, attempts);
	}
	
	/**
	 * Gets the property.
	 *
	 * @param <X> the generic type
	 * @param key the key
	 * @param cls the cls
	 * @return the property
	 */
	public <X> X getProperty(String key, Class<X> cls) {
		return (X) properties.get(key);
	}

	/**
	 * Gets the all properties.
	 *
	 * @return the all properties
	 */
	public Map<String,Object> getAllProperties() {
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Place.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> placeAsynchronous() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TaskLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", priority=" + priority + ", key=" + key + ", label=" + label + ", attempts=" + attempts + ", properties=" + properties + "]";
	}
	
	/**
	 * By conveyor name.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param name the name
	 * @return the part loader
	 */
	public static <K,L> TaskLoader<K,L> byConveyorName(String name) {
		return null;//Conveyor.byName(name).part();
	}

	/**
	 * Lazy supplier.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param name the name
	 * @return the supplier
	 */
	public static <K,L> Supplier<TaskLoader<K,L>> lazySupplier(String name) {
		return new Supplier<>() {
            TaskLoader<K, L> tl;

            @Override
            public TaskLoader<K, L> get() {
                if (tl == null) {
                    TaskPoolConveyor<K, L, ?> c = (TaskPoolConveyor<K, L, ?>) Conveyor.byName(name.startsWith("task_pool_")?name:"task_pool_"+name);
                    if (c != null) {
                        tl = c.task();
                    }
                }
                return tl;
            }
        };
	}

}
