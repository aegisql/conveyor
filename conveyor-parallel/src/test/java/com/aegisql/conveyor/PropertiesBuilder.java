package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PropertiesBuilder implements Supplier<Map<String,Object>> {

	private Map<String,Object> map = new HashMap<>();
	
	private PropertiesBuilder() {
		
	}
	
	public static PropertiesBuilder putFirst(String key, Object val) {
		PropertiesBuilder mb = new PropertiesBuilder();
		mb.map.put(key, val);
		return mb;
	}

	public PropertiesBuilder put(String key, Object val) {
		this.map.put(key, val);
		return this;
	}

	@Override
	public Map<String, Object> get() {
		return map;
	}

}
