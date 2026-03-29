-- Conveyor JDBC persistence initialization script
-- This script mirrors JdbcPersistenceBuilder.init() step order.
-- Review and extend it with grants, storage options, or engine-specific tuning as needed.
-- Engine: postgres
-- Key class: java.lang.Long
-- Database: conveyor_db
-- Schema: conveyor_db
-- Parts table: PART
-- Completed log table: COMPLETED_LOG
-- Suggested CLI usage:
-- psql -h localhost -p 5432 -U conveyor -d conveyor_db -f postgres-init.sql

-- [1] Create database
CREATE DATABASE conveyor_db;

-- [2] Create schema
CREATE SCHEMA IF NOT EXISTS conveyor_db;

-- [3] Create parts table
CREATE TABLE IF NOT EXISTS PART (ID BIGINT PRIMARY KEY,LOAD_TYPE CHAR(15),CART_KEY BIGINT,CART_LABEL VARCHAR(100),CREATION_TIME TIMESTAMP,EXPIRATION_TIME TIMESTAMP,PRIORITY BIGINT NOT NULL DEFAULT 0,CART_VALUE BYTEA,VALUE_TYPE VARCHAR(255),CART_PROPERTIES TEXT,ARCHIVED SMALLINT NOT NULL DEFAULT 0,TRANSACTION_ID BIGINT NOT NULL);

-- [4] Create parts table index
CREATE INDEX IF NOT EXISTS PART_IDX ON PART(CART_KEY);

-- [5] Create completed log table
CREATE TABLE IF NOT EXISTS COMPLETED_LOG (CART_KEY BIGINT PRIMARY KEY,COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);

-- [6] Create unique index for TRANSACTION_ID
CREATE UNIQUE INDEX IF NOT EXISTS PART_TRANSACTION_ID_IDX ON PART(TRANSACTION_ID);

-- Optional cleanup section.
-- The following statements are commented out intentionally.

-- [1] Drop unique index for TRANSACTION_ID
-- DROP INDEX IF EXISTS PART_TRANSACTION_ID_IDX;

-- [2] Drop parts table index
-- DROP INDEX IF EXISTS PART_IDX;

-- [3] Drop completed log table
-- DROP TABLE IF EXISTS COMPLETED_LOG;

-- [4] Drop parts table
-- DROP TABLE IF EXISTS PART;

-- [5] Drop schema
-- DROP SCHEMA IF EXISTS conveyor_db;

-- [6] Drop database
-- DROP DATABASE IF EXISTS conveyor_db;

