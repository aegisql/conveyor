package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AcknowledgeStatus <K> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final K key;
	private final Status status;
	private final Map<String,Object> properties = new HashMap<String, Object>();
	public AcknowledgeStatus(K key, Status status, Map<String, Object> pm) {
		super();
		this.key = key;
		this.status = status;
		if(pm != null) {
			pm.forEach( (k,v) -> properties.put(k, v) );
		}
	}
	public K getKey() {
		return key;
	}
	public Status getStatus() {
		return status;
	}
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	public <T> T gepProperty(String key,Class<T> clas) {
		return(T) properties.get(key);
	}
	@Override
	public String toString() {
		return "AcknowledgeStatus [key=" + key + ", status=" + status + ", properties=" + properties + "]";
	}
	
}
