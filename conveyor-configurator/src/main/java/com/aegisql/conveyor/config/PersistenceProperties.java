package com.aegisql.conveyor.config;

import java.util.HashMap;
import java.util.Map;

public class PersistenceProperties {

	private final boolean isDefault;
	private final String type;
	private final String schema;
	private final String name;

	private final Map<String,PersistenceProperty> properties = new HashMap<>();
	
	public PersistenceProperties(String type, String schema, String name) {
		this.type   = type;
		this.schema = schema;
		this.name   = name;
		this.isDefault = type == null || schema == null || name == null;
	}
	
	public void addProperty(PersistenceProperty pp) {
		properties.put(pp.getProperty(), pp);
	}

	public boolean isDefault() {
		return isDefault;
	}

	public String getType() {
		return type;
	}

	public String getSchema() {
		return schema;
	}

	public String getName() {
		return name;
	}

	public Map<String, PersistenceProperty> getProperties() {
		return properties;
	}

	public String getLevel0Key() {
		return "";
	}

	public String getLevel1Key() {
		return getType();
	}

	public String getLevel2Key() {
		return getType()+"."+getSchema();
	}
	
	@Override
	public String toString() {
		return "PersistenceProperties [isDefault=" + isDefault + ", " + (type != null ? "type=" + type + ", " : "")
				+ (schema != null ? "schema=" + schema + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (properties != null ? "properties=" + properties : "") + "]";
	}
	
	
	
}
