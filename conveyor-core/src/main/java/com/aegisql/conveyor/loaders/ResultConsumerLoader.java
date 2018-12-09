package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.serial.SerializablePredicate;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultConsumerLoader.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public final class ResultConsumerLoader<K,OUT> {

	/** The key. */
	public final K key;
	
	/** The consumer. */
	public final ResultConsumer <K,OUT> consumer;
	
	/** The placer. */
	public final Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;
	
	/** The ttl msec. */
	public final long ttlMsec;
	
	/** The global placer. */
	private final Consumer<ResultConsumer<K,OUT>> globalPlacer;
	
	/** The filter. */
	public final SerializablePredicate<K> filter;
	
	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();
	
	/**  The priority. */
	public final long priority;
	
	/**
	 * Instantiates a new result consumer loader.
	 *
	 * @param placer the placer
	 * @param globalPlacer the global placer
	 * @param consumer the consumer
	 */
	public ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer,
			Consumer<ResultConsumer <K,OUT>> globalPlacer,
			ResultConsumer <K,OUT> consumer
			) {
		this(placer,globalPlacer,System.currentTimeMillis(),0,0,null,consumer,null,Collections.EMPTY_MAP,0);
	}

	/**
	 * Instantiates a new result consumer loader.
	 *
	 * @param placer the placer
	 * @param globalPlacer the global placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 * @param consumer the consumer
	 * @param filter the filter
	 * @param properties the properties
	 * @param priority the priority
	 */
	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<ResultConsumer <K,OUT>> globalPlacer,
			long creationTime,
			long expirationTime,
			long ttlMsec,
			K key, 
			ResultConsumer <K,OUT> consumer,
			SerializablePredicate<K> filter,
			Map<String,Object> properties,
			long priority) {
		this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer;
		this.key            = key;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.filter         = filter;
		this.priority       = priority;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new result consumer loader.
	 *
	 * @param placer the placer
	 * @param globalPlacer the global placer
	 * @param creationTime the creation time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 * @param consumer the consumer
	 * @param filter the filter
	 * @param properties the properties
	 * @param priority the priority
	 * @param dumb the dumb
	 */
	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<ResultConsumer <K,OUT>> globalPlacer,
			long creationTime,
			long ttlMsec,
			K key, 
			ResultConsumer <K,OUT> consumer,
			SerializablePredicate<K> filter,
			Map<String,Object> properties,
			long priority,
			boolean dumb) {
		this(placer,
				globalPlacer,
				creationTime,
				creationTime + ttlMsec,
				ttlMsec,
				key,
				consumer,
				filter,
				properties,
				priority);
	}

	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> id(K k) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				k,
				this.consumer,
				null/*either key or filter*/
				,properties,
				priority);
	}

	/**
	 * Foreach.
	 *
	 * @param f the f
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> foreach(SerializablePredicate<K> f) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				null/*either key or filter*/,
				this.consumer,
				f,
				properties,
				priority);
	}

	/**
	 * Foreach.
	 *
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> foreach() {
		return foreach(k->true);
	}

	/**
	 * First.
	 *
	 * @param consumer the consumer
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> first(ResultConsumer <K,OUT> consumer) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				this.key,
				consumer,
				filter,
				properties,
				priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(long et) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,et,0,key,consumer,filter,properties,priority);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT>  creationTime(long ct) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,ct,expirationTime,0,key,consumer,filter,properties,priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(Instant instant) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,instant.toEpochMilli(),0,key,consumer,filter,properties,priority);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT>  creationTime(Instant instant) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,instant.toEpochMilli(),expirationTime,0,key,consumer,filter,properties,priority);
	}
	
	/**
	 * Priority.
	 *
	 * @param p the p
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT>  priority(long p) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,0,key,consumer,filter,properties,p);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer, creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,consumer,filter,properties,priority,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(Duration duration) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,duration.toMillis(),key,consumer,filter,properties,priority,true);
	}

	
	/**
	 * And then.
	 *
	 * @param consumer the consumer
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> andThen(ResultConsumer <K,OUT> consumer) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				this.key,
				this.consumer != null ? this.consumer.andThen(consumer) : consumer
				,this.filter,
				properties,
				priority
				);
	}

	/**
	 * Before.
	 *
	 * @param consumer the consumer
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> before(ResultConsumer <K,OUT> consumer) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				this.key,
				this.consumer != null ? consumer.andThen(this.consumer) : consumer
				,this.filter,
				properties,
				priority
				);
	}

	
	/**
	 * Clear properties.
	 *
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> clearProperties() {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,Collections.EMPTY_MAP,priority);
	}

	/**
	 * Clear property.
	 *
	 * @param k the k
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> clearProperty(String k) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.remove(k);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap,priority);
	}

	/**
	 * Adds the property.
	 *
	 * @param k the k
	 * @param v the v
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap,priority);
	}

	/**
	 * Adds the properties.
	 *
	 * @param moreProperties the more properties
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap,priority);
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
	 * Sets the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> set() {
		if(key == null && filter == null) {
			CompletableFuture<Boolean> ready = new CompletableFuture<>();
			ready.complete(true);
			globalPlacer.accept(consumer);
			return ready;
		} else {
			return placer.apply(this);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultConsumerLoader [" + (key==null?"default":"key="+key) + ", creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", priority="+priority+", properties=" + properties + "]";
	}
	
	public static <K,OUT> ResultConsumerLoader<K,OUT> byConveyorName(String name) {
		return Conveyor.byName(name).resultConsumer();
	}

	public static <K,OUT> ResultConsumerLoader<K,OUT> byConveyorName(String name,ResultConsumer<K,OUT> consumer) {
		return Conveyor.byName(name).resultConsumer(consumer);
	}

}
