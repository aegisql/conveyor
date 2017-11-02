package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

/**
	 * The Enum ArchiveStrategy.
	 */
	public enum ArchiveStrategy {
		
		/** The custom. */
		CUSTOM, 
 /** The delete. */
 //set strategy
		DELETE,
		
		/** The set archived. */
		SET_ARCHIVED,
		
		/** The move to schema table. */
		MOVE_TO_SCHEMA_TABLE, 
 /** The move to file. */
 //schema,table
		MOVE_TO_FILE, 
 /** The no action. */
 //path,file
		NO_ACTION //external archive strategy
	}