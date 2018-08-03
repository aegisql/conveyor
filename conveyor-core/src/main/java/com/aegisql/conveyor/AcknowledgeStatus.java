package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class AcknowledgeStatus.
 *
 * @param <K> the key type
 */
public class AcknowledgeStatus <K> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The key. */
	private final K key;
	
	/** The status. */
	private final Status status;
	
	/** The properties. */
	private final Map<String,Object> properties = new HashMap<String, Object>();
	
	/**
	 * Instantiates a new acknowledge status.
	 *
	 * @param key the key
	 * @param status the status
	 * @param pm the pm
	 */
	public AcknowledgeStatus(K key, Status status, Map<String, Object> pm) {
		super();
		this.key = key;
		this.status = status;
		if(pm != null) {
			pm.forEach( (k,v) -> properties.put(k, v) );
		}
	}
	
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey() {
		return key;
	}
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	/**
	 * Gep property.
	 *
	 * @param <T> the generic type
	 * @param key the key
	 * @param clas the clas
	 * @return the t
	 */
	public <T> T gepProperty(String key,Class<T> clas) {
		return(T) properties.get(key);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AcknowledgeStatus [key=" + key + ", status=" + status + ", properties=" + properties + "]";
	}
	
}
