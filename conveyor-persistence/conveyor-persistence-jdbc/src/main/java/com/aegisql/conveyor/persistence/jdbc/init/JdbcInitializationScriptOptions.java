package com.aegisql.conveyor.persistence.jdbc.init;

/**
 * Script-rendering options for JDBC persistence initialization.
 *
 * @param includeCleanupSection include commented cleanup SQL at the end of the script
 * @param scriptFileNameHint file name shown in generated CLI usage examples
 */
public record JdbcInitializationScriptOptions(boolean includeCleanupSection, String scriptFileNameHint) {

	public static final String DEFAULT_SCRIPT_FILE = "conveyor-persistence-init.sql";

	public JdbcInitializationScriptOptions {
		scriptFileNameHint = (scriptFileNameHint == null || scriptFileNameHint.isBlank())
				? DEFAULT_SCRIPT_FILE
				: scriptFileNameHint;
	}

	public static JdbcInitializationScriptOptions defaults() {
		return new JdbcInitializationScriptOptions(false, DEFAULT_SCRIPT_FILE);
	}
}
