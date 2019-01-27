package com.aegisql.conveyor.persistence.jdbc.builders;

public interface DynamicPersistenceSql {
	String saveCartQuery();
	String saveCompletedBuildKeyQuery();
	String getPartQuery();
	String getExpiredPartQuery();
	String getAllPartIdsQuery();
	String getAllUnfinishedPartIdsQuery();
	String getAllCompletedKeysQuery();
	String getAllStaticPartsQuery();
	String getNumberOfPartsQuery();
}
