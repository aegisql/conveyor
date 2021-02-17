package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// TODO: Auto-generated Javadoc
/**
 * The Interface Cart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public interface Cart <K,V,L> extends Expireable, Serializable, Comparable<Cart<K,?,?>> {
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey();
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public V getValue();

	/**
	 * Gets value.
	 *
	 * @param <X> the type parameter
	 * @param cls the cls
	 * @return the value
	 */
	default <X> X getValue(Class<X> cls) {
		var value = getValue();
		if(value == null) {
			return null;
		}
		return (X) value;
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public L getLabel();
	
	/**
	 * Gets the creation time.
	 *
	 * @return the creation time
	 */
	public long getCreationTime();

	/**
	 * Gets the cart creation nano time.
	 *
	 * @return the cart creation nano time
	 */
	public long getCartCreationNanoTime();

	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	public long getExpirationTime();
	
	/**
	 * Gets Future for the cart. get() methods return:
	 * true if were accepted by the builder
	 * false when offer or add return false
	 * exception if were rejected by builder 
	 *
	 * @return the future
	 */
	public CompletableFuture<Boolean> getFuture();
	
	/**
	 * Gets the scrap consumer.
	 *
	 * @return the scrap consumer
	 */
	public ScrapConsumer<K,Cart<K,V,L>> getScrapConsumer();
	
	/**
	 * Adds the serializable property.
	 *
	 * @param <X> the generic type
	 * @param name the name
	 * @param property the property
	 */
	public <X> void addProperty(String name, X property);
	
	/**
	 * Gets the property.
	 *
	 * @param <X> the generic type
	 * @param name the name
	 * @param cls the cls
	 * @return the property
	 */
	default <X> X getProperty(String name, Class<X> cls) {
		return (X) getAllProperties().get(name);
	}

	/**
	 * Gets the all properties.
	 *
	 * @return the all properties
	 */
	public Map<String,Object> getAllProperties(); 

	
	/**
	 * Clear property.
	 *
	 * @param name the name
	 */
	public void clearProperty(String name);
	
	
	/**
	 * Gets the load type.
	 *
	 * @return the load type
	 */
	public LoadType getLoadType();
	
	/**
	 * copy().
	 *
	 * @return the cart
	 */
	public Cart <K,V,L> copy();
	
	/**
	 * Gets the priority.
	 *
	 * @return the priority
	 */
	public long getPriority();
	
	
}
