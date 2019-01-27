package com.aegisql.conveyor.persistence.jdbc.builders;

public class GenericPersistenceSql implements DynamicPersistenceSql {
	
	public static final String ID = "ID";
	public static final String LOAD_TYPE = "LOAD_TYPE";
	public static final String CART_KEY = "CART_KEY";
	public static final String CART_LABEL = "CART_LABEL";
	public static final String CREATION_TIME = "CREATION_TIME";
	public static final String EXPIRATION_TIME = "EXPIRATION_TIME";
	public static final String CART_VALUE = "CART_VALUE";
	public static final String CART_PROPERTIES = "CART_PROPERTIES";
	public static final String VALUE_TYPE = "VALUE_TYPE";			
	public static final String PRIORITY = "PRIORITY";
	public static final String ARCHIVED = "ARCHIVED";

	protected final String schema;
	protected final String partTable;
	protected final String completedLogTable;
	
	public GenericPersistenceSql(String schema, String partTable, String completedLogTable) {
		this.schema = schema;
		this.partTable = partTable;
		this.completedLogTable = completedLogTable;
	}

	public GenericPersistenceSql(String partTable, String completedLogTable) {
		this(null,partTable,completedLogTable);
	}

	protected String saveCartQuery;
	protected String saveCompletedBuildKeyQuery;
	protected String getPartQuery;
	protected String getExpiredPartQuery;
	protected String getAllPartIdsQuery;
	protected String getAllUnfinishedPartIdsQuery;
	protected String getAllCompletedKeysQuery;
	protected String getAllStaticPartsQuery;
	protected String getNumberOfPartsQuery;
	
	@Override
	public String saveCartQuery() {
		if(saveCartQuery==null) {
			saveCartQuery = "INSERT INTO " + st(partTable) + "("
					+ID
					+","+LOAD_TYPE
					+","+CART_KEY
					+","+CART_LABEL
					+","+CREATION_TIME
					+","+EXPIRATION_TIME
					+","+CART_VALUE
					+","+CART_PROPERTIES
					+","+VALUE_TYPE					
					+","+PRIORITY
					+") VALUES (?,?,?,?,?,?,?,?,?,?)"
					;
		}
		return saveCartQuery;
	}

	@Override
	public String saveCompletedBuildKeyQuery() {
		if(saveCompletedBuildKeyQuery==null) {
			saveCompletedBuildKeyQuery = "INSERT INTO " + st(completedLogTable) + "("+CART_KEY+") VALUES( ? )";
		}
		return saveCompletedBuildKeyQuery;
	}

	@Override
	public String getPartQuery() {
		if(getPartQuery==null) {
			 getPartQuery = "SELECT "
					+CART_KEY
					+","+CART_VALUE
					+","+CART_LABEL
					+","+CREATION_TIME
					+","+EXPIRATION_TIME
					+","+LOAD_TYPE
					+","+CART_PROPERTIES
					+","+VALUE_TYPE
					+","+PRIORITY
					+" FROM " + st(partTable) 
					+" WHERE "+ID+" IN ( ? ) AND "+ARCHIVED+" = 0"
					;
		}
		return getPartQuery;
	}

	@Override
	public String getExpiredPartQuery() {
		if(getExpiredPartQuery==null) {
			getExpiredPartQuery = "SELECT "
					+CART_KEY
					+","+CART_VALUE
					+","+CART_LABEL
					+","+CREATION_TIME
					+","+EXPIRATION_TIME
					+","+LOAD_TYPE
					+","+CART_PROPERTIES
					+","+VALUE_TYPE
					+","+PRIORITY
					+" FROM " + st(partTable) 
					+" WHERE "+EXPIRATION_TIME+" > TIMESTAMP('19710101000000') AND "+EXPIRATION_TIME+" < CURRENT_TIMESTAMP"
					;
		}
		return getExpiredPartQuery;
	}

	@Override
	public String getAllPartIdsQuery() {
		if(getAllPartIdsQuery==null) {
			getAllPartIdsQuery = "SELECT "+ID
					+" FROM " + st(partTable) 
					+" WHERE "+CART_KEY+" = ? AND "+ARCHIVED+" = 0 ORDER BY "+ID+" ASC"
					;
		}
		return getAllPartIdsQuery;
	}

	@Override
	public String getAllUnfinishedPartIdsQuery() {
		if(getAllUnfinishedPartIdsQuery==null) {
			getAllUnfinishedPartIdsQuery = "SELECT "
					+CART_KEY
					+","+CART_VALUE
					+","+CART_LABEL
					+","+CREATION_TIME
					+","+EXPIRATION_TIME
					+","+LOAD_TYPE
					+","+CART_PROPERTIES
					+","+VALUE_TYPE
					+","+PRIORITY
					+" FROM " + st(partTable) 
					+" WHERE "+ARCHIVED+" = 0  AND "+LOAD_TYPE+" <> 'STATIC_PART' ORDER BY "+ID+" ASC"
					;
		}
		return getAllUnfinishedPartIdsQuery;
	}

	@Override
	public String getAllCompletedKeysQuery() {
		if(getAllCompletedKeysQuery==null) {
			getAllCompletedKeysQuery = "SELECT "+CART_KEY+" FROM "+st(completedLogTable);
		}
		return getAllCompletedKeysQuery;
	}

	@Override
	public String getAllStaticPartsQuery() {
		if(getAllStaticPartsQuery==null) {
			getAllStaticPartsQuery = "SELECT "
					+CART_KEY
					+","+CART_VALUE
					+","+CART_LABEL
					+","+CREATION_TIME
					+","+EXPIRATION_TIME
					+","+LOAD_TYPE
					+","+CART_PROPERTIES
					+","+VALUE_TYPE
					+","+PRIORITY
					+" FROM " + st(partTable) 
					+" WHERE "+ARCHIVED+" = 0 AND "+LOAD_TYPE+" = 'STATIC_PART' ORDER BY "+ID+" ASC";
		}
		return getAllStaticPartsQuery;
	}

	@Override
	public String getNumberOfPartsQuery() {
		if(getNumberOfPartsQuery==null) {
			getNumberOfPartsQuery = "SELECT COUNT(*) FROM " + st(partTable) + " WHERE "+ARCHIVED+" = 0";
		}
		return getNumberOfPartsQuery;
	}

	protected String st(String table) {
		if(schema == null) {
			return table;
		} else {
			return schema+"."+table;			
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GenericPersistenceSql [").append(st(partTable))
				.append(", ").append(st(completedLogTable)).append("]");
		return builder.toString();
	}
	
	

}
