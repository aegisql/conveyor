-- Conveyor JDBC persistence initialization script
-- This script mirrors JdbcPersistenceBuilder.init() step order.
-- Review and extend it with grants, storage options, or engine-specific tuning as needed.
-- Engine: mysql
-- Key class: java.lang.Long
-- Database: conveyor_db
-- Schema: <not set>
-- Parts table: PART
-- Completed log table: COMPLETED_LOG
-- Suggested CLI usage:
-- mysql -h localhost -P 3306 -u conveyor -p < mysql-init.sql

-- [1] Create database
CREATE DATABASE IF NOT EXISTS conveyor_db;

-- [2] Create parts table
CREATE TABLE IF NOT EXISTS PART (ID BIGINT PRIMARY KEY,LOAD_TYPE CHAR(15),CART_KEY BIGINT,CART_LABEL VARCHAR(100),CREATION_TIME DATETIME NOT NULL,EXPIRATION_TIME DATETIME NOT NULL,PRIORITY BIGINT NOT NULL DEFAULT 0,CART_VALUE BLOB,VALUE_TYPE VARCHAR(255),CART_PROPERTIES TEXT,ARCHIVED SMALLINT NOT NULL DEFAULT 0,TRANSACTION_ID BIGINT NOT NULL);

-- [3] Create parts table index
CREATE INDEX PART_IDX ON PART(CART_KEY);

-- [4] Create completed log table
CREATE TABLE IF NOT EXISTS COMPLETED_LOG (CART_KEY BIGINT PRIMARY KEY,COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);

-- [5] Create unique index for TRANSACTION_ID
CREATE UNIQUE INDEX PART_TRANSACTION_ID_IDX ON PART(TRANSACTION_ID);

-- Optional cleanup section.
-- The following statements are commented out intentionally.

-- [1] Drop unique index for TRANSACTION_ID
-- DROP INDEX PART_TRANSACTION_ID_IDX ON PART;

-- [2] Drop parts table index
-- DROP INDEX PART_IDX ON PART;

-- [3] Drop completed log table
-- DROP TABLE IF EXISTS COMPLETED_LOG;

-- [4] Drop parts table
-- DROP TABLE IF EXISTS PART;

-- [5] Drop database
-- DROP DATABASE IF EXISTS conveyor_db;

