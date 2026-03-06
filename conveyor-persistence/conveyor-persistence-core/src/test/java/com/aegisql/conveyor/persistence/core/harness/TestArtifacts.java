package com.aegisql.conveyor.persistence.core.harness;

import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.io.File;

public final class TestArtifacts {

	private static final String TEST_ARTIFACTS_DIRECTORY_PROPERTY = "conveyor.persistence.test.artifacts.dir";
	private static final File TEST_ARTIFACTS_ROOT = initializeRoot();

	private TestArtifacts() {
	}

	private static File initializeRoot() {
		String configuredDirectory = System.getProperty(TEST_ARTIFACTS_DIRECTORY_PROPERTY, "test-artifacts");
		File root = new File(configuredDirectory).getAbsoluteFile();
		if (!root.exists() && !root.mkdirs()) {
			throw new PersistenceException("Cannot create test artifacts directory " + root.getAbsolutePath());
		}
		return root;
	}

	public static File file(String path) {
		String normalized = path == null ? "" : path.replaceFirst("^\\./", "");
		File file = new File(normalized);
		File resolved = file.isAbsolute() ? file : new File(TEST_ARTIFACTS_ROOT, normalized);
		File parent = resolved.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new PersistenceException("Cannot create parent directory " + parent.getAbsolutePath());
		}
		return resolved;
	}

	public static String path(String path) {
		return file(path).getPath();
	}
}
