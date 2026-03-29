package com.aegisql.conveyor.persistence.jdbc;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InitConveyorPersistenceTest {

	@Test
	public void writesScriptFromYamlConfiguration() throws Exception {
		Path tempDir = Files.createTempDirectory("conveyor-init-script");
		Path config = tempDir.resolve("init.yml");
		Path output = tempDir.resolve("generated.sql");
		Files.writeString(config, """
				mode: script
				type: derby-memory
				keyClass: java.lang.Long
				schema: DEMO_DB
				partTable: PART
				completedLogTable: COMPLETED_LOG
				includeCleanup: true
				additionalFields:
				  - type: java.lang.Long
				    name: TRANSACTION_ID
				uniqueFields:
				  - [TRANSACTION_ID]
				output: %s
				""".formatted(output.toString()), StandardCharsets.UTF_8);

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		int exit = InitConveyorPersistence.run(
				new String[]{"--config", config.toString()},
				new PrintStream(stdout, true, StandardCharsets.UTF_8),
				new PrintStream(stderr, true, StandardCharsets.UTF_8)
		);

		assertEquals(0, exit);
		String script = Files.readString(output, StandardCharsets.UTF_8);
		assertTrue(script.contains("Derby memory databases are created when the memory URL is opened."));
		assertTrue(script.contains("CREATE TABLE PART"));
		assertTrue(script.contains("TRANSACTION_ID BIGINT NOT NULL"));
		assertTrue(script.contains("-- DROP TABLE PART;"));
	}

	@Test
	public void initializesDerbyMemoryPersistenceFromRuntimeOptions() throws Exception {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		int exit = InitConveyorPersistence.run(
				new String[]{
						"--mode", "init",
						"--type", "derby-memory",
						"--key-class", "java.lang.Long",
						"--schema", "CLI_INIT_DB",
						"--part-table", "PART",
						"--completed-log-table", "COMPLETED_LOG"
				},
				new PrintStream(stdout, true, StandardCharsets.UTF_8),
				new PrintStream(stderr, true, StandardCharsets.UTF_8)
		);

		assertEquals(0, exit);
		assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("Initialization completed"));
	}
}
