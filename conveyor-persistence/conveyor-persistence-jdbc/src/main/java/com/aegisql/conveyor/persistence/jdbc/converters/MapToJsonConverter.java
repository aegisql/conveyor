package com.aegisql.conveyor.persistence.jdbc.converters;

import java.util.Map;

import com.aegisql.conveyor.persistence.converters.ObjectToJsonStringConverter;
// TODO: Auto-generated Javadoc
/**
 * The Class BlobConverter.
 *
 */
public class MapToJsonConverter extends ObjectToJsonStringConverter<Map> {

	
	/**
	 * Instantiates a new blob converter.
	 *
	 * @param conn the conn
	 */
	public MapToJsonConverter() {
		super(Map.class);
	}
	

}
