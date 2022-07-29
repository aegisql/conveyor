package com.aegisql.conveyor.config;

import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceProperty.
 */
public class PersistenceProperty {

	/** The is persistence property. */
	private final boolean isPersistenceProperty;
	
	/** The is default property. */
	private final boolean isDefaultProperty;
	
	/** The type. */
	private final String type;
	
	/** The schema. */
	private final String schema;
	
	/** The name. */
	private final String name;
	
	/** The property. */
	private final String property;
	
	/** The value. */
	private final Object value;

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PersistenceProperty [isPersistenceProperty=" + isPersistenceProperty + ", isDefaultProperty="
				+ isDefaultProperty + ", " + (name != null ? "name=" + name + ", " : "")
				+ (property != null ? "property=" + property : "") + "]";
	}

	/**
	 * Checks if is persistence property.
	 *
	 * @return true, if is persistence property
	 */
	public boolean isPersistenceProperty() {
		return isPersistenceProperty;
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
	 * Instantiates a new persistence property.
	 *
	 * @param isConveyorProperty the is conveyor property
	 * @param isDefaultProperty the is default property
	 * @param type the type
	 * @param schema the schema
	 * @param name the name
	 * @param property the property
	 * @param value the value
	 */
	private PersistenceProperty(boolean isConveyorProperty, boolean isDefaultProperty, String type, String schema,
			String name, String property, Object value) {
		this.isPersistenceProperty = isConveyorProperty;
		this.isDefaultProperty = isDefaultProperty;
		this.name = name;
		this.property = property;
		this.value = value;
		this.type = type;
		this.schema = schema;
	}

	/**
	 * Eval.
	 *
	 * @param propertyKey the property key
	 * @param value the value
	 * @param consumer the consumer
	 */
	public static void eval(String propertyKey, Object value, Consumer<PersistenceProperty> consumer) {
		if (propertyKey == null
				|| !propertyKey.toUpperCase().startsWith(ConveyorConfiguration.PERSISTENCE_PREFIX.toUpperCase())) {
			return;
		}
		if (value == null) {
			PersistenceProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		} else if (value instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) value;
			map.forEach(
					(part, val) -> eval(propertyKey + ConveyorConfiguration.PROPERTY_DELIMITER + part, val, consumer));
		} else if (value instanceof List) {
			List<Object> list = (List<Object>) value;
			list.forEach(val -> eval(propertyKey, val, consumer));
		} else {
			PersistenceProperty cp = evalProperty(propertyKey, value);
			consumer.accept(cp);
		}
	}

	/**
	 * Eval property.
	 *
	 * @param propertyKey the property key
	 * @param value the value
	 * @return the persistence property
	 */
	static PersistenceProperty evalProperty(String propertyKey, Object value) {

		String altDelim = "_";
		if (altDelim.equals(ConveyorConfiguration.PROPERTY_DELIMITER)) {
			altDelim = ".";
		}

		String prefix = ConveyorConfiguration.PERSISTENCE_PREFIX + ConveyorConfiguration.PROPERTY_DELIMITER;

		if (propertyKey == null || !propertyKey.toUpperCase().startsWith(prefix)) {
			return new PersistenceProperty(false, false, null, null, null, null, null);
		}

		propertyKey = propertyKey.replaceAll("archiveStrategy" + ConveyorConfiguration.PROPERTY_DELIMITER,
				"archiveStrategy" + altDelim);

		boolean isConveyorProperty = true;
		boolean isDefaultProperty = false;
		String name = null;
		String convProperty;
		String schema = null;
		String type = null;

		String[] parts = propertyKey.split(Pattern.quote(ConveyorConfiguration.PROPERTY_DELIMITER));

		if (parts.length == 2) {
			isDefaultProperty = true;
			convProperty = parts[1];
		} else if (parts.length == 3) {
			isDefaultProperty = true;
			type = parts[1];
			convProperty = parts[2];
		} else if (parts.length == 4) {
			isDefaultProperty = true;
			type = parts[1];
			schema = parts[2];
			convProperty = parts[3];
		} else {
			convProperty = parts[parts.length - 1];
			type = parts[1];
			schema = parts[2];
			String[] nameParts = new String[parts.length - 4];
			System.arraycopy(parts, 3, nameParts, 0, nameParts.length);
			name = String.join(ConveyorConfiguration.PROPERTY_DELIMITER, nameParts);
		}

		convProperty = convProperty.replaceAll(altDelim, ConveyorConfiguration.PROPERTY_DELIMITER);

		if(value instanceof String && value != null) {
			String str = value.toString();
			if(str.toUpperCase().startsWith(ConveyorConfiguration.JAVAPATH_PREFIX)) {
				// Process JavaPath here
				String remaining = str.substring(ConveyorConfiguration.JAVAPATH_PREFIX.length());
				value = ConveyorConfiguration.evalPath(remaining);
			}
		}

		return new PersistenceProperty(isConveyorProperty, isDefaultProperty, type, schema, name, convProperty, value);

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
		if (value != null) {
			return value.toString();
		} else {
			return null;
		}
	}

	public boolean getValueAsBoolean() {
		if(value==null) {
			return false;
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		} else if(value instanceof String) {
			return Boolean.valueOf((value.toString()));
		} else {
			throw new ConveyorRuntimeException("Expected boolean type, but found "+value.getClass());
		}
	}

	public Integer getValueAsInteger() {
		if(value == null) {
			return null;
		} else if(value instanceof Integer) {
			return (Integer) value;
		} else if(value instanceof String) {
			return Integer.valueOf((value.toString()));
		} else {
			throw new ConveyorRuntimeException("Expected integer type, but found "+value.getClass());
		}
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
	 * Builds the key.
	 *
	 * @return the string
	 */
	public String buildKey() {
		StringBuilder sb = new StringBuilder();
		if (getType() != null) {
			sb.append(getType());
			if (getSchema() != null) {
				sb.append('.').append(getSchema());
				if (getName() != null) {
					sb.append('.').append(getName());
				}
			}
		}
		return sb.toString();
	}

}
