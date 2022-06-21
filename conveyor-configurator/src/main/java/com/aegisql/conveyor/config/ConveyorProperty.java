package com.aegisql.conveyor.config;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorProperty.
 */
public class ConveyorProperty {

	/** The is conveyor property. */
	private final boolean isConveyorProperty;
	
	/** The is default property. */
	private final boolean isDefaultProperty;
	
	/** The name. */
	private final String name;
	
	/** The property. */
	private final String property;
	
	/** The value. */
	private final Object value;

	private static final TemplateEditor templateEditor = new TemplateEditor();

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ConveyorProperty [");
		return "ConveyorProperty [isConveyorProperty=" + isConveyorProperty + ", isDefaultProperty=" + isDefaultProperty
				+ ", " + (name != null ? "name=" + name + ", " : "") + (property != null ? "property=" + property : "")
				+ "]";
	}

	/**
	 * Checks if is conveyor property.
	 *
	 * @return true, if is conveyor property
	 */
	public boolean isConveyorProperty() {
		return isConveyorProperty;
	}

	/**
	 * Checks if is default property.
	 *
	 * @return true, if is default property
	 */
	public boolean isDefaultProperty() {
		return isDefaultProperty;
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
	 * Gets the property.
	 *
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Instantiates a new conveyor property.
	 *
	 * @param isConveyorProperty the is conveyor property
	 * @param isDefaultProperty the is default property
	 * @param name the name
	 * @param property the property
	 * @param value the value
	 */
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
	
	/**
	 * Eval.
	 *
	 * @param propertyKey the property key
	 * @param value the value
	 * @param consumer the consumer
	 */
	public static void eval(String propertyKey, Object value, Consumer<ConveyorProperty> consumer) {
		String processedValue = null;
		if(value != null) {
			processedValue = templateEditor.setVariables(propertyKey, value.toString());
		}
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(ConveyorConfiguration.PROPERTY_PREFIX.toUpperCase())){
			return;
		}
		if(processedValue != null && value instanceof String) {
			value = processedValue;
		}
		if(value == null) {
			ConveyorProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		} else if(value instanceof Map) {
			Map<String,Object> map = (Map<String, Object>) value;
			if(propertyKey.toLowerCase().endsWith(ConveyorConfiguration.PROPERTY_DELIMITER+"persistence")) {
				ConveyorProperty cNameProperty = ConveyorProperty.evalProperty(propertyKey, "");
				PersistenceProperty.eval("persistence", value, pp->{
					ConveyorProperty cpp = new ConveyorProperty(true, cNameProperty.isDefaultProperty, cNameProperty.getName(), "persistenceProperty", pp);
					consumer.accept(cpp);
				});
			} else {
				map.forEach((part,val) -> eval(propertyKey+ConveyorConfiguration.PROPERTY_DELIMITER+part,val,consumer));
			}
		} else if(value instanceof List) {
			List<Object> list = (List<Object>) value;
			list.forEach(val -> eval(propertyKey,val,consumer));
		} else {
			ConveyorProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		}
	}

	/**
	 * Eval property.
	 *
	 * @param propertyKey the property key
	 * @param value the value
	 * @return the conveyor property
	 */
	static ConveyorProperty evalProperty(String propertyKey, Object value) {
		
		
		String prefix = ConveyorConfiguration.PROPERTY_PREFIX + ConveyorConfiguration.PROPERTY_DELIMITER;
		
		if(propertyKey == null || ! propertyKey.toUpperCase().startsWith(prefix)) {
			return new ConveyorProperty(false, false, null, null, null);
		}
		boolean isConveyorProperty = true;
		boolean isDefaultProperty  = false;
		String name                = null;
		String convProperty;
		
		String[] parts = propertyKey.split(Pattern.quote(ConveyorConfiguration.PROPERTY_DELIMITER));

		if(parts.length == 2) {
			isDefaultProperty = true;
			convProperty = parts[1];
		} else {
			convProperty = parts[parts.length-1];
			String[] nameParts = new String[parts.length-2];
			System.arraycopy(parts, 1, nameParts, 0, nameParts.length);
			name = String.join(ConveyorConfiguration.PROPERTY_DELIMITER, nameParts);
		}
		
		
		return new ConveyorProperty(isConveyorProperty,isDefaultProperty,name,convProperty,value);
		
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Gets the value as string.
	 *
	 * @return the value as string
	 */
	public String getValueAsString() {
		if(value != null) {
			return value.toString();
		} else {
			return null;
		}
	}
	
}
