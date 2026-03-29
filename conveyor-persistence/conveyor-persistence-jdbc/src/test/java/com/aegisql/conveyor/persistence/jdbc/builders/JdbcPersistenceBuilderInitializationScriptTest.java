package com.aegisql.conveyor.persistence.jdbc.builders;

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.init.JdbcInitializationScriptOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcPersistenceBuilderInitializationScriptTest {

	@Test
	public void generatesInitializationScriptInInitOrderWithCleanupSection() {
		JdbcPersistenceBuilder<Long> builder = new JdbcPersistenceBuilder<>(Long.class)
				.engineType("jdbc")
				.jdbcEngine(new ScriptTestEngine())
				.database("demo_db")
				.schema("demo_schema")
				.partTable("PART")
				.completedLogTable("COMPLETED_LOG")
				.addField(Long.class, "TRANSACTION_ID")
				.addUniqueFields("TRANSACTION_ID");

		String script = builder.initializationScript(new JdbcInitializationScriptOptions(true, "demo-init.sql"));

		assertContainsInOrder(
				script,
				"-- [1] Create database",
				"CREATE DATABASE demo_db;",
				"-- [2] Create schema",
				"CREATE SCHEMA demo_schema;",
				"-- [3] Create parts table",
				"CREATE TABLE PART",
				"TRANSACTION_ID BIGINT NOT NULL",
				"-- [4] Create parts table index",
				"CREATE INDEX PART_IDX ON PART(CART_KEY);",
				"-- [5] Create completed log table",
				"CREATE TABLE COMPLETED_LOG",
				"-- [6] Create unique index for TRANSACTION_ID",
				"CREATE UNIQUE INDEX PART_TRANSACTION_ID_IDX ON PART(TRANSACTION_ID);",
				"-- Optional cleanup section.",
				"-- DROP INDEX PART_TRANSACTION_ID_IDX;",
				"-- DROP TABLE PART;",
				"-- DROP DATABASE demo_db;"
		);
		assertTrue(script.contains("Run the script with your jdbc SQL client."));
	}

	private static void assertContainsInOrder(String value, String... expected) {
		int index = -1;
		for (String fragment : expected) {
			int next = value.indexOf(fragment, index + 1);
			assertTrue(next >= 0, "Expected fragment not found after index " + index + ": " + fragment);
			index = next;
		}
	}

	private static final class ScriptTestEngine extends GenericEngine<Long> {

		private ScriptTestEngine() {
			super(Long.class, null, true);
		}

		@Override
		protected String getFieldType(Class<?> fieldClass) {
			return switch (fieldClass.getName()) {
				case "java.lang.Long" -> "BIGINT NOT NULL";
				case "java.lang.Integer" -> "INT NOT NULL";
				default -> "VARCHAR(255) NOT NULL";
			};
		}

		@Override
		protected void init() {
			setConnectionUrlTemplateForInitDatabase("jdbc:test://{database}");
			setConnectionUrlTemplateForInitSchema("jdbc:test://{database}");
			setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:test://{database}");
			setConnectionUrlTemplate("jdbc:test://{database}");
		}
	}
}
