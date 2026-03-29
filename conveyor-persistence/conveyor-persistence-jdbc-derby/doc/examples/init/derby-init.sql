-- Conveyor JDBC persistence initialization script
-- This script mirrors JdbcPersistenceBuilder.init() step order.
-- Review and extend it with grants, storage options, or engine-specific tuning as needed.
-- Engine: derby
-- Key class: java.lang.Long
-- Database: conveyor_db
-- Schema: conveyor_db
-- Parts table: PART
-- Completed log table: COMPLETED_LOG
-- Suggested CLI usage:
-- ij < derby-init.sql

-- [1] Create database
-- Derby database creation is driven by the JDBC URL create=true flag, not a standalone SQL statement.
-- Open database conveyor_db with create=true before running the remaining statements.

-- [2] Create schema
CREATE SCHEMA conveyor_db;

-- [3] Create parts table
CREATE TABLE PART (ID BIGINT PRIMARY KEY,LOAD_TYPE CHAR(15),CART_KEY BIGINT,CART_LABEL VARCHAR(100),CREATION_TIME TIMESTAMP,EXPIRATION_TIME TIMESTAMP,PRIORITY BIGINT NOT NULL DEFAULT 0,CART_VALUE BLOB,VALUE_TYPE VARCHAR(255),CART_PROPERTIES CLOB,ARCHIVED SMALLINT NOT NULL DEFAULT 0,TRANSACTION_ID BIGINT NOT NULL);

-- [4] Create parts table index
CREATE INDEX PART_IDX ON PART(CART_KEY);

-- [5] Create completed log table
CREATE TABLE COMPLETED_LOG (CART_KEY BIGINT PRIMARY KEY,COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);

-- [6] Create unique index for TRANSACTION_ID
CREATE UNIQUE INDEX PART_TRANSACTION_ID_IDX ON PART(TRANSACTION_ID);

-- Optional cleanup section.
-- The following statements are commented out intentionally.

-- [1] Drop unique index for TRANSACTION_ID
-- DROP INDEX PART_TRANSACTION_ID_IDX;

-- [2] Drop parts table index
-- DROP INDEX PART_IDX;

-- [3] Drop completed log table
-- DROP TABLE COMPLETED_LOG;

-- [4] Drop parts table
-- DROP TABLE PART;

-- [5] Drop schema
-- DROP SCHEMA conveyor_db;

-- [6] Drop database
-- Derby database cleanup is environment-specific and is not emitted as SQL.
-- Remove database conveyor_db using your Derby administration procedure if needed.

