package com.aegisql.conveyor.persistence.jdbc.init;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One formatted script section emitted by a JDBC engine.
 *
 * @param title section title
 * @param lines SQL or note lines
 * @param executable true when the lines are meant to be executed as SQL
 */
public record JdbcScriptSection(String title, List<String> lines, boolean executable) {

	public JdbcScriptSection {
		Objects.requireNonNull(title, "title must not be null");
		Objects.requireNonNull(lines, "lines must not be null");
		lines = List.copyOf(new ArrayList<>(lines));
	}

	public static JdbcScriptSection sql(String title, String... lines) {
		return new JdbcScriptSection(title, List.of(lines), true);
	}

	public static JdbcScriptSection note(String title, String... lines) {
		return new JdbcScriptSection(title, List.of(lines), false);
	}
}
