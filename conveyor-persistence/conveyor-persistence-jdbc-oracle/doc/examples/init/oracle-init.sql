-- Conveyor JDBC persistence initialization script
-- This script mirrors JdbcPersistenceBuilder.init() step order.
-- Review and extend it with grants, storage options, or engine-specific tuning as needed.
-- Engine: oracle
-- Key class: java.lang.Long
-- Database: XEPDB1
-- Schema: CONVEYOR_APP
-- Parts table: PART
-- Completed log table: COMPLETED_LOG
-- Suggested CLI usage:
-- sqlplus conveyor/<password>@//localhost:1521/XEPDB1 @oracle-init.sql

-- [1] Create database
-- Oracle service or pluggable database creation is outside Conveyor initialization scope.
-- Use an existing service such as XEPDB1 before running the remaining statements.

-- [2] Create schema
-- Oracle user/schema creation is outside Conveyor initialization scope.
-- Create user CONVEYOR_APP and grant required privileges before running the remaining statements.

-- [3] Create parts table
CREATE TABLE PART (ID NUMBER(19) PRIMARY KEY,LOAD_TYPE VARCHAR2(15 CHAR),CART_KEY NUMBER(19),CART_LABEL VARCHAR2(100 CHAR),CREATION_TIME TIMESTAMP,EXPIRATION_TIME TIMESTAMP,PRIORITY NUMBER(19) DEFAULT 0 NOT NULL,CART_VALUE BLOB,VALUE_TYPE VARCHAR2(255 CHAR),CART_PROPERTIES CLOB,ARCHIVED NUMBER(1) DEFAULT 0 NOT NULL,TRANSACTION_ID NUMBER(19) NOT NULL);

-- [4] Create parts table index
CREATE INDEX PART_IDX ON PART(CART_KEY);

-- [5] Create completed log table
CREATE TABLE COMPLETED_LOG (CART_KEY NUMBER(19) PRIMARY KEY,COMPLETION_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL);

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
-- Oracle user/schema cleanup is intentionally not emitted automatically.
-- Drop user CONVEYOR_APP manually if that is part of your environment workflow.

-- [6] Drop database
-- Oracle service or pluggable database cleanup is outside Conveyor initialization scope.
-- Clean up service XEPDB1 with your DBA-managed procedure if needed.

