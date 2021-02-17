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

public class Tester {

	public Tester() {
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

	public static int getPerfTestSize() {
		String param = "PERF_TEST_SIZE";
		int size = 10000;
		
		String test = System.getenv(param);
		if(test != null) {
			try {
				size = Integer.parseInt(test);
				return size;
			}catch (Exception e) {
			}
		}
		test = System.getProperty(param);
		if(test != null) {
			try {
				size = Integer.parseInt(test);
				return size;
			}catch (Exception e) {
			}
		}
		return size;
	}


	public static String LOCAL_MYSQL_URL = "jdbc:mysql://localhost:3306/";
	public static String LOCAL_MARIADB_URL = "jdbc:mariadb://localhost:3306/";
	public static String LOCAL_POSTGRES_URL = "jdbc:postgresql://localhost:5432/";
	
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
			return getConnection(LOCAL_MYSQL_URL, "tester", "");
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
			return getConnection(LOCAL_MARIADB_URL, "tester", "");
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
			return getConnection(LOCAL_POSTGRES_URL, "postgres", "root");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean hasDriver(String driver) {
		try {
			return Class.forName(driver) != null;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static void sleep(long msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
		}
	}

	
	public static void removeLocalMysqlDatabase(String database) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection connnection=DriverManager.getConnection(LOCAL_MYSQL_URL,"tester","");
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
			Connection connnection=DriverManager.getConnection(LOCAL_MYSQL_URL,"tester","");
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
			Connection connnection=DriverManager.getConnection(LOCAL_POSTGRES_URL,"postgres","root");
			Statement st = connnection.createStatement();
			st.execute("DROP DATABASE IF EXISTS "+database);
			st.close();
			connnection.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static void removeDirectory(String directory) {
		File f = new File(directory);
		try {
			FileUtils.deleteDirectory(f);
			System.out.println("Directory "+directory+" has been deleted!");
		} catch (IOException e) {
			System.err.println("Problem occured when deleting the directory : " + directory);
			e.printStackTrace();
		}

	}

	public static void removeFile(String directory) {
		File f = new File(directory);
		FileUtils.deleteQuietly(f);
		System.out.println("File "+directory+" has been deleted!");
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
	
}
