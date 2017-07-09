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
import java.util.function.Predicate;

import com.aegisql.conveyor.consumers.result.ResultConsumer;

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
	public final Predicate<K> filter;
	
	private final Map<String,Object> properties = new HashMap<>();
	
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
		this(placer,globalPlacer,System.currentTimeMillis(),0,0,null,consumer,null,Collections.EMPTY_MAP);
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
	 */
	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<ResultConsumer <K,OUT>> globalPlacer,
			long creationTime,
			long expirationTime,
			long ttlMsec,
			K key, 
			ResultConsumer <K,OUT> consumer,
			Predicate<K> filter,
			Map<String,Object> properties) {
		this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer;
		this.key            = key;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.filter         = filter;
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
	 * @param dumb the dumb
	 */
	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<ResultConsumer <K,OUT>> globalPlacer,
			long creationTime,
			long ttlMsec,
			K key, 
			ResultConsumer <K,OUT> consumer,
			Predicate<K> filter,
			Map<String,Object> properties,
			boolean dumb) {
		this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer;
		this.key            = key;
		this.creationTime   = creationTime;
		this.expirationTime = creationTime + ttlMsec;
		this.ttlMsec        = ttlMsec;
		this.filter         = filter;
		this.properties.putAll(properties);
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
				,properties);
	}

	/**
	 * Foreach.
	 *
	 * @param f the f
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K,OUT> foreach(Predicate<K> f) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				null/*either key or filter*/,
				this.consumer,
				f,
				properties);
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
				properties);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(long et) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,et,0,key,consumer,filter,properties);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(Instant instant) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,instant.toEpochMilli(),0,key,consumer,filter,properties);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer, creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,consumer,filter,properties,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(Duration duration) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,duration.toMillis(),key,consumer,filter,properties,true);
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
				properties
				);
	}

	public ResultConsumerLoader<K,OUT> clearProperties() {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,Collections.EMPTY_MAP);
	}

	public ResultConsumerLoader<K,OUT> clearProperty(String k) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.remove(k);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap);
	}

	public ResultConsumerLoader<K,OUT> addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap);
	}

	public ResultConsumerLoader<K,OUT> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,expirationTime,ttlMsec,key,consumer,filter,newMap);
	}
	
	public <X> X getProperty(String key, Class<X> cls) {
		return (X) properties.get(key);
	}

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
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", properties=" + properties + "]";
	}
	
	
	
}
