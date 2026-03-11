package com.aegisql.conveyor.persistence.jdbc.harness;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

public class Tester {

	private static final String TEST_DB_DIRECTORY_PROPERTY = "conveyor.persistence.test.db.dir";
	private static final File TEST_DB_ROOT = initializeTestDbRoot();

	//MYSQL_URL=jdbc:mysql://closet:3306/ MYSQL_HOST=closet MYSQL_USER=tester MYSQL_PASSWORD=root mvn test

	public Tester() {
	}

	private final static AtomicInteger idGen = new AtomicInteger(0);

	private static File initializeTestDbRoot() {
		String configuredDirectory = getEnvOrDefaultString(TEST_DB_DIRECTORY_PROPERTY, "test-dbs");
		File root = new File(configuredDirectory).getAbsoluteFile();
		if (!root.exists() && !root.mkdirs()) {
			throw new PersistenceException("Cannot create test database directory " + root.getAbsolutePath());
		}
		System.setProperty("derby.system.home", root.getAbsolutePath());
		System.setProperty("derby.stream.error.file", new File(root, "derby.log").getAbsolutePath());
		return root;
	}

	private static File resolveTestDbArtifact(String path) {
		String normalized = path == null ? "" : path.replaceFirst("^\\./", "");
		File file = new File(normalized);
		if (file.isAbsolute()) {
			return file;
		}
		return new File(TEST_DB_ROOT, normalized);
	}

	public static String testDbPath(String path) {
		return resolveTestDbArtifact(path).getPath();
	}

	public static String sqliteJdbcUrl(String path) {
		return "jdbc:sqlite:" + testDbPath(path);
	}

	public static void waitUntilArchived(Persistence<Integer> p, int testSize) {
		long prevParts = 0;
		long parts;
		int sameNumber = 0;
		while ((parts = p.getNumberOfParts()) > 0) {
			System.out.print("\r"+(100-100*parts/(3*testSize))+"%");
			if(prevParts == parts) {
				sameNumber++;
				if(sameNumber > 10) {
					System.out.println();
					throw new RuntimeException("Stuck on archiving with number of parts = "+parts);
				}
			}
			sleep(1000);
			prevParts = parts;
		}
		System.out.println("\r100%");
	}

	private static String LOCAL_MYSQL_URL = "jdbc:mysql://localhost:3306/";
	private static String LOCAL_MARIADB_URL = "jdbc:mariadb://localhost:3306/";
	private static String LOCAL_ORACLE_URL = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";
	private static String LOCAL_POSTGRES_URL = "jdbc:postgresql://localhost:5432/";

	private static String getEnvOrDefaultString(String param, String defValue) {
		String value = System.getenv(param);
		if(value != null) {
			return value;
		}
		value = System.getProperty(param);
		if(value != null) {
			return value;
		}
		return defValue;
	}

	private static int getEnvOrDefaultInteger(String param, int defValue) {
		String value = System.getenv(param);
		if(value != null) {
			return Integer.parseInt(value);
		}
		value = System.getProperty(param);
		if(value != null) {
			return Integer.parseInt(value);
		}
		return defValue;
	}

	public static int getPerfTestSize() {
		return getEnvOrDefaultInteger("PERF_TEST_SIZE",10000);
	}

	public static String getMysqlUrl() {
		return getEnvOrDefaultString("MYSQL_URL",LOCAL_MYSQL_URL);
	}

	public static String getMysqlHost() {
		return getEnvOrDefaultString("MYSQL_HOST","localhost");
	}

	public static int getMysqlPort() {
		return getEnvOrDefaultInteger("MYSQL_PORT",3306);
	}
	public static String getMysqlUser() {
		return getEnvOrDefaultString("MYSQL_USER","tester");
	}
	public static String getMysqlPassword() {
		return getEnvOrDefaultString("MYSQL_PASSWORD",null);
	}

	public static String getMariaDBUrl() {
		return getEnvOrDefaultString("MARIADB_URL",LOCAL_MARIADB_URL);
	}
	public static String getMariadbUser() {
		return getEnvOrDefaultString("MARIADB_USER","tester");
	}
	public static String getMariadbPassword() {
		return getEnvOrDefaultString("MARIADB_PASSWORD",null);
	}
	public static String getMariadbHost() {
		return getEnvOrDefaultString("MARIADB_HOST","localhost");
	}
	public static int getMariadbPort() {
		return getEnvOrDefaultInteger("MARIADB_PORT",3306);
	}

	public static String getOracleUrl() {
		return getEnvOrDefaultString("ORACLE_URL", LOCAL_ORACLE_URL);
	}

	public static String getOracleHost() {
		return getEnvOrDefaultString("ORACLE_HOST", "localhost");
	}

	public static int getOraclePort() {
		return getEnvOrDefaultInteger("ORACLE_PORT", 1521);
	}

	public static String getOracleServiceName() {
		return getEnvOrDefaultString("ORACLE_SERVICE", "FREEPDB1");
	}

	public static String getOracleUser() {
		return getEnvOrDefaultString("ORACLE_USER", "system");
	}

	public static String getOraclePassword() {
		return getEnvOrDefaultString("ORACLE_PASSWORD", "root");
	}

	public static String getOracleSchema() {
		return getEnvOrDefaultString("ORACLE_SCHEMA", getOracleUser().toUpperCase());
	}

	public static String getPostgresUrl() {
		return getEnvOrDefaultString("POSTGRES_URL",LOCAL_POSTGRES_URL);
	}
	public static String getPostgresUser() {
		return getEnvOrDefaultString("POSTGRES_USER","postgres");
	}
	public static String getPostgresPassword() {
		return getEnvOrDefaultString("POSTGRES_PASSWORD","root");
	}
	public static String getPostgresHost() {
		return getEnvOrDefaultString("POSTGRES_HOST","localhost");
	}
	public static int getPostgresPort() {
		return getEnvOrDefaultInteger("POSTGRES_PORT",5432);
	}

	public static Connection getConnection(String url, String user, String password) {
		try {
			return DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Connection getMySqlConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			return getConnection(getMysqlUrl(), getMysqlUser(), getMysqlPassword());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean testMySqlConnection() {
		try {
			Connection c = getMySqlConnection();
			if(c != null) {
				c.close();
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Connection getMariaDbConnection() {
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			return getConnection(getMariaDBUrl(), getMariadbUser(), getMariadbPassword());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean testMariaDbConnection() {
		try {
			Connection c = getMariaDbConnection();
			if(c != null) {
				c.close();
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean testPostgresConnection() {
		try {
			Connection c = getPostgresConnection();
			if(c != null) {
				c.close();
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Connection getPostgresConnection() {
		try {
			Class.forName("org.postgresql.Driver");
			return getConnection(getPostgresUrl(), getPostgresUser(), getPostgresPassword());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Connection getOracleConnection() {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			return getConnection(getOracleUrl(), getOracleUser(), getOraclePassword());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean testOracleConnection() {
		try {
			Connection c = getOracleConnection();
			if(c != null) {
				c.close();
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	public static void sleep(int i, int sleepNumber, double frac) {
		if(i % sleepNumber !=  0) {
			return;
		}
		int msec = (int) frac;
		double nsec = frac - msec;
		try {
			Thread.sleep(msec, (int) (999999.0 * nsec));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	
	public static void removeLocalMysqlDatabase(String database) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection connnection=DriverManager.getConnection(getMysqlUrl(),getMysqlUser(),getMysqlPassword());
			Statement st = connnection.createStatement();
			st.execute("DROP SCHEMA IF EXISTS "+database);
			st.close();
			connnection.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void removeLocalMariaDbDatabase(String database) {
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			Connection connnection=DriverManager.getConnection(getMariaDBUrl(),getMariadbUser(),getMariadbPassword());
			Statement st = connnection.createStatement();
			st.execute("DROP SCHEMA IF EXISTS "+database);
			st.close();
			connnection.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void removeLocalPostgresDatabase(String database) {
		try {
			Class.forName("org.postgresql.Driver");
			Connection connnection=DriverManager.getConnection(getPostgresUrl(),getPostgresUser(),getPostgresPassword());
			Statement st = connnection.createStatement();
			st.execute("DROP DATABASE IF EXISTS "+database);
			st.close();
			connnection.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static void removeLocalOracleTables(String... tables) {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			Connection connection = DriverManager.getConnection(getOracleUrl(), getOracleUser(), getOraclePassword());
			Statement st = connection.createStatement();
			for (String table : tables) {
				if (table == null || table.isBlank()) {
					continue;
				}
				try {
					st.execute("DROP TABLE " + table + " CASCADE CONSTRAINTS PURGE");
				} catch (SQLException e) {
					if (e.getErrorCode() != 942) {
						throw e;
					}
				}
			}
			st.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static void removeDirectory(String directory) {
		File f = resolveTestDbArtifact(directory);
		try {
			FileUtils.deleteDirectory(f);
			System.out.println("Directory "+f.getPath()+" has been deleted!");
		} catch (IOException e) {
			System.err.println("Problem occured when deleting the directory : " + f.getPath());
			e.printStackTrace();
		}

	}

	public static void removeFile(String directory) {
		File f = resolveTestDbArtifact(directory);
		FileUtils.deleteQuietly(f);
		FileUtils.deleteQuietly(new File(f.getPath() + "-wal"));
		FileUtils.deleteQuietly(new File(f.getPath() + "-shm"));
		FileUtils.deleteQuietly(new File(f.getPath() + "?cache=shared&mode=memory"));
		System.out.println("File "+f.getPath()+" has been deleted!");
	}

	public static String getTestMethod() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		StackTraceElement el = elements[2];
		return el.getMethodName();
	}

	public static String getTestClass() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		StackTraceElement el = elements[2];
		String className = el.getClassName();
		Class cls = null;
		try {
			cls = Class.forName(className);
			return cls.getSimpleName();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new PersistenceException("Cannot find class for "+className,e);
		}
	}

	public static int nextId() {
		return idGen.incrementAndGet();
	}

}
