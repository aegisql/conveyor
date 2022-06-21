package com.aegisql.conveyor.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceProperties.
 */
public class PersistenceProperties {

	/** The is default. */
	private final boolean isDefault;
	
	/** The type. */
	private final String type;
	
	/** The schema. */
	private final String schema;
	
	/** The name. */
	private final String name;

	/** The properties. */
	private final Map<String,LinkedList<PersistenceProperty>> properties = new HashMap<>();
	
	/**
	 * Instantiates a new persistence properties.
	 *
	 * @param type the type
	 * @param schema the schema
	 * @param name the name
	 */
	public PersistenceProperties(String type, String schema, String name) {
		this.type   = type;
		this.schema = schema;
		this.name   = name;
		this.isDefault = type == null || schema == null || name == null;
	}
	
	/**
	 * Adds the property.
	 *
	 * @param pp the pp
	 */
	public void addProperty(PersistenceProperty pp) {
		LinkedList<PersistenceProperty> ppList = properties.computeIfAbsent(pp.getProperty(), k -> new LinkedList<>());
		ppList.add(pp);
	}

	/**
	 * Checks if is default.
	 *
	 * @return true, if is default
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, LinkedList<PersistenceProperty>> getProperties() {
		return properties;
	}

	/**
	 * Gets the level 0 key.
	 *
	 * @return the level 0 key
	 */
	public String getLevel0Key() {
		return "";
	}

	/**
	 * Gets the level 1 key.
	 *
	 * @return the level 1 key
	 */
	public String getLevel1Key() {
		return getType();
	}

	/**
	 * Gets the level 2 key.
	 *
	 * @return the level 2 key
	 */
	public String getLevel2Key() {
		return getType()+"."+getSchema();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PersistenceProperties [isDefault=" + isDefault + ", " + (type != null ? "type=" + type + ", " : "")
				+ (schema != null ? "schema=" + schema + ", " : "") + (name != null ? "name=" + name + ", " : "")
				+ (properties != null ? "properties=" + properties : "") + "]";
	}
	
	
	
}
