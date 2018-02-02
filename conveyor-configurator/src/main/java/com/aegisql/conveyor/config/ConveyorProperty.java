package com.aegisql.conveyor.config;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ConveyorProperty {

	private final boolean isConveyorProperty;
	private final boolean isDefaultProperty;
	private final String name;
	private final String property;
	private final Object value;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ConveyorProperty [");
		return "ConveyorProperty [isConveyorProperty=" + isConveyorProperty + ", isDefaultProperty=" + isDefaultProperty
				+ ", " + (name != null ? "name=" + name + ", " : "") + (property != null ? "property=" + property : "")
				+ "]";
	}

	public boolean isConveyorProperty() {
		return isConveyorProperty;
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

	private ConveyorProperty(
			boolean isConveyorProperty
			,boolean isDefaultProperty
			,String name
			,String property
			,Object value
			) {
		this.isConveyorProperty = isConveyorProperty;
		this.isDefaultProperty  = isDefaultProperty;
		this.name               = name;
		this.property           = property;
		this.value              = value;
	}
	
	public static void eval(String propertyKey, Object value, Consumer<ConveyorProperty> consumer) {
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(ConveyorConfiguration.PROPERTY_PREFIX.toUpperCase())){
			return;
		}
		if(value == null) {
			ConveyorProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		} else if(value instanceof Map) {
			Map<String,Object> map = (Map<String, Object>) value;
			map.forEach((part,val) -> eval(propertyKey+ConveyorConfiguration.PROPERTY_DELIMITER+part,val,consumer));
		} else if(value instanceof List) {
			List<Object> list = (List<Object>) value;
			list.forEach(val -> eval(propertyKey,val,consumer));
		} else {
			ConveyorProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		}
	}

	static ConveyorProperty evalProperty(String propertyKey, Object value) {
		
		
		String prefix = ConveyorConfiguration.PROPERTY_PREFIX + ConveyorConfiguration.PROPERTY_DELIMITER;
		
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(prefix)) {
			return new ConveyorProperty(false, false, null, null, null);
		}
		boolean isConveyorProperty = true;
		boolean isDefaultProperty  = false;
		String name                = null;
		String convProperty        = null;
		
		String[] parts = propertyKey.split(Pattern.quote(ConveyorConfiguration.PROPERTY_DELIMITER));

		if(parts.length == 2) {
			isDefaultProperty = true;
			convProperty = parts[1];
		} else {
			convProperty = parts[parts.length-1];
			String[] nameParts = new String[parts.length-2];
			for(int i = 0; i < nameParts.length; i++) {
				nameParts[i] = parts[i+1];
			}
			name = String.join(ConveyorConfiguration.PROPERTY_DELIMITER, nameParts);
		}
		
		
		return new ConveyorProperty(isConveyorProperty,isDefaultProperty,name,convProperty,value);
		
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
	
}
