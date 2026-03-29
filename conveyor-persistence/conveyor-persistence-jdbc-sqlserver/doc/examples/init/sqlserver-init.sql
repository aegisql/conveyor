-- Conveyor JDBC persistence initialization script
-- This script mirrors JdbcPersistenceBuilder.init() step order.
-- Review and extend it with grants, storage options, or engine-specific tuning as needed.
-- Engine: sqlserver
-- Key class: java.lang.Long
-- Database: conveyor_db
-- Schema: <not set>
-- Parts table: PART
-- Completed log table: COMPLETED_LOG
-- Suggested CLI usage:
-- sqlcmd -S localhost,1433 -d conveyor_db -U conveyor -P <password> -i sqlserver-init.sql

-- [1] Create database
IF DB_ID(N'conveyor_db') IS NULL CREATE DATABASE conveyor_db;

-- [2] Create parts table
IF OBJECT_ID(N'PART', N'U') IS NULL CREATE TABLE PART (ID BIGINT PRIMARY KEY,LOAD_TYPE CHAR(15),CART_KEY BIGINT,CART_LABEL VARCHAR(100),CREATION_TIME DATETIME2 NOT NULL,EXPIRATION_TIME DATETIME2 NOT NULL,PRIORITY BIGINT NOT NULL DEFAULT 0,CART_VALUE VARBINARY(MAX),VALUE_TYPE VARCHAR(255),CART_PROPERTIES VARCHAR(MAX),ARCHIVED SMALLINT NOT NULL DEFAULT 0,TRANSACTION_ID BIGINT NOT NULL);

-- [3] Create parts table index
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'PART_IDX' AND object_id = OBJECT_ID(N'PART')
)
CREATE INDEX PART_IDX ON PART(CART_KEY);

-- [4] Create completed log table
IF OBJECT_ID(N'COMPLETED_LOG', N'U') IS NULL CREATE TABLE COMPLETED_LOG (CART_KEY BIGINT PRIMARY KEY,COMPLETION_TIME DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP);

-- [5] Create unique index for TRANSACTION_ID
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'PART_TRANSACTION_ID_IDX' AND object_id = OBJECT_ID(N'PART')
)
CREATE UNIQUE INDEX PART_TRANSACTION_ID_IDX ON PART(TRANSACTION_ID);

-- Optional cleanup section.
-- The following statements are commented out intentionally.

-- [1] Drop unique index for TRANSACTION_ID
-- IF EXISTS (
--     SELECT 1
--     FROM sys.indexes
--     WHERE name = N'PART_TRANSACTION_ID_IDX' AND object_id = OBJECT_ID(N'PART')
-- )
-- DROP INDEX PART_TRANSACTION_ID_IDX ON PART;

-- [2] Drop parts table index
-- IF EXISTS (
--     SELECT 1
--     FROM sys.indexes
--     WHERE name = N'PART_IDX' AND object_id = OBJECT_ID(N'PART')
-- )
-- DROP INDEX PART_IDX ON PART;

-- [3] Drop completed log table
-- IF OBJECT_ID(N'COMPLETED_LOG', N'U') IS NOT NULL DROP TABLE COMPLETED_LOG;

-- [4] Drop parts table
-- IF OBJECT_ID(N'PART', N'U') IS NOT NULL DROP TABLE PART;

-- [5] Drop database
-- IF DB_ID(N'conveyor_db') IS NOT NULL DROP DATABASE conveyor_db;

