package com.aegisql.conveyor.persistence.jdbc.archive;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;

public abstract class AbstractJdbcArchiver <K> implements Archiver<K> {

	protected final static Logger LOG = LoggerFactory.getLogger(DeleteArchiver.class);
	
	protected final Class<K> keyClass;
	protected final String partTable;
	protected final String completedLogTable;
	
	protected final String getExpiredParts;
	protected final String q;
	protected final String deleteFromPartsByIdSql;
	protected final String deleteFromPartsByCartKeySql;
	protected final String deleteFromCompletedSql;
	protected final String deleteExpiredPartsSql;
	protected final String deleteAllPartsSql;
	protected final String deleteAllCompletedSql;
	protected final String updatePartsByIdSql;
	protected final String updatePartsByCartKeySql;
	protected final String updateExpiredPartsSql;
	protected final String updateAllPartsSql;
	
	protected Persistence<K> persistence;

	public AbstractJdbcArchiver(Class<K> keyClass, String partTable, String completedLogTable) {
		this.keyClass = keyClass;
		this.partTable = partTable;
		this.completedLogTable = completedLogTable;
		if(Number.class.isAssignableFrom(keyClass)) {
			this.q = "";
		} else {
			this.q = "'";
		}
		this.getExpiredParts = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+" FROM " + partTable + " WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
		this.deleteFromPartsByIdSql      = "DELETE FROM "+partTable + " WHERE ID IN(?)";
		this.deleteFromPartsByCartKeySql = "DELETE FROM "+partTable + " WHERE CART_KEY IN(?)";
		this.deleteFromCompletedSql      = "DELETE FROM "+completedLogTable + " WHERE CART_KEY IN(?)";
		this.deleteExpiredPartsSql       = "DELETE FROM "+partTable + " WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
		this.deleteAllPartsSql           = "DELETE FROM "+partTable;
		this.deleteAllCompletedSql       = "DELETE FROM "+completedLogTable;
		
		this.updatePartsByIdSql          = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE ID IN(?)";
		this.updatePartsByCartKeySql     = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE CART_KEY IN(?)";		
		this.updateExpiredPartsSql       = "UPDATE "+partTable+" SET ARCHIVED = 1 WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
		this.updateAllPartsSql           = "UPDATE "+partTable+" SET ARCHIVED = 1";

	}
	
	protected String quote(Collection<K> keys) {
		StringBuilder sb = new StringBuilder();
		keys.forEach(id -> sb.append(q).append(id).append(q).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}

	protected String quoteLong(Collection<Long> keys) {
		StringBuilder sb = new StringBuilder();
		keys.forEach(id -> sb.append(id).append(",") );
		if(sb.lastIndexOf(",") > 0) {
			sb.deleteCharAt(sb.lastIndexOf(","));
		}
		return sb.toString();
	}

	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}
	
}
