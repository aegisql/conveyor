package com.aegisql.conveyor.cart;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Interface Cart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public interface Cart <K,V,L> extends Expireable, Serializable {
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
	public <X> X getProperty(String name, Class<X> cls); 

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
	
	
	public LoadType getLoadType();
	
	/**
	 * copy().
	 *
	 * @return the cart
	 */
	public Cart <K,V,L> copy();
}
