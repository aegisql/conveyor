package com.aegisql.conveyor.config;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class PersistenceProperty {

	private final boolean isPersistenceProperty;
	private final boolean isDefaultProperty;
	private final String type;
	private final String schema;
	private final String name;
	private final String property;
	private final Object value;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("PersistenceProperty [");
		return "PersistenceProperty [isPersistenceProperty=" + isPersistenceProperty + ", isDefaultProperty=" + isDefaultProperty
				+ ", " + (name != null ? "name=" + name + ", " : "") + (property != null ? "property=" + property : "")
				+ "]";
	}

	public boolean isPersistenceProperty() {
		return isPersistenceProperty;
	}

	public boolean isDefaultProperty() {
		return isDefaultProperty;
	}

	public String getName() {
		return name;
	}

	public String getProperty() {
		return property;
	}

	private PersistenceProperty(
			boolean isConveyorProperty
			,boolean isDefaultProperty
			,String type
			,String schema
			,String name
			,String property
			,Object value
			) {
		this.isPersistenceProperty = isConveyorProperty;
		this.isDefaultProperty  = isDefaultProperty;
		this.name               = name;
		this.property           = property;
		this.value              = value;
		this.type               = type;
		this.schema             = schema;
	}
	
	public static void eval(String propertyKey, Object value, Consumer<PersistenceProperty> consumer) {
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(ConveyorConfiguration.PERSISTENCE_PREFIX.toUpperCase())){
			return;
		}
		if(value == null) {
			PersistenceProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		} else if(value instanceof Map) {
			Map<String,Object> map = (Map<String, Object>) value;
			map.forEach((part,val) -> eval(propertyKey+ConveyorConfiguration.PROPERTY_DELIMITER+part,val,consumer));
		} else if(value instanceof List) {
			List<Object> list = (List<Object>) value;
			list.forEach(val -> eval(propertyKey,val,consumer));
		} else {
			PersistenceProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		}
	}

	static PersistenceProperty evalProperty(String propertyKey, Object value) {
		
		String altDelim = "_";
		if( altDelim.equals(ConveyorConfiguration.PROPERTY_DELIMITER)) {
			altDelim = ".";
		}
		
		
		String prefix = ConveyorConfiguration.PERSISTENCE_PREFIX + ConveyorConfiguration.PROPERTY_DELIMITER;
		
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(prefix)) {
			return new PersistenceProperty(false, false, null, null, null, null,null);
		}
		
		propertyKey = propertyKey.replaceAll("archiveStrategy"+ConveyorConfiguration.PROPERTY_DELIMITER, "archiveStrategy"+altDelim);

		boolean isConveyorProperty = true;
		boolean isDefaultProperty  = false;
		String name                = null;
		String convProperty        = null;
		String schema              = null;
		String type                = "derby";
		
		String[] parts = propertyKey.split(Pattern.quote(ConveyorConfiguration.PROPERTY_DELIMITER));

		if(parts.length == 2) {
			isDefaultProperty = true;
			convProperty = parts[1];
		} else if(parts.length == 3) {
			isDefaultProperty = true;
			type         = parts[1];
			convProperty = parts[2];
		} else if(parts.length == 4) {
			isDefaultProperty = true;
			type         = parts[1];
			schema       = parts[2];
			convProperty = parts[3];
		} else {
			convProperty = parts[parts.length-1];
			schema       = parts[2];
			String[] nameParts = new String[parts.length-4];
			for(int i = 0; i < nameParts.length; i++) {
				nameParts[i] = parts[i+3];
			}
			name = String.join(ConveyorConfiguration.PROPERTY_DELIMITER, nameParts);
		}
		
		convProperty = convProperty.replaceAll(altDelim, ConveyorConfiguration.PROPERTY_DELIMITER);
		
		return new PersistenceProperty(isConveyorProperty,isDefaultProperty,type, schema, name,convProperty,value);
		
	}

	public Object getValue() {
		return value;
	}
	
	public String getValueAsString() {
		if(value != null) {
			return value.toString();
		} else {
			return null;
		}
	}

	public String getType() {
		return type;
	}

	public String getSchema() {
		return schema;
	}
	
}
