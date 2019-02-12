package com.aegisql.conveyor.persistence.jdbc.builders;

import java.util.LinkedHashMap;

public enum RestoreOrder {
	 NO_ORDER
	,BY_ID("ID","ASC")
	,BY_PRIORITY_AND_ID("PRIORITY","DESC","ID","ASC")
	;
	
	LinkedHashMap<String, String> order = new LinkedHashMap<>();
	
	public LinkedHashMap<String, String> getOrder() {
		return order;
	}
	
	RestoreOrder(String ... fields) {
		if(fields != null) {
			for(int i = 0; i < fields.length-1; i+=2) {
				String field = fields[i];
				String dir = fields[i+1];
				order.put(field, dir);
			}
		}
	}
	
}
