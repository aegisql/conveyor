package com.aegisql.conveyor.persistence.jdbc.converters;

public class StringLabelConverter extends StringConverter<String> {
	@Override
	public String fromPersistence(String p) {
		return p;
	}
	@Override
	public String conversionHint() {
		return "?:String";
	}
}
