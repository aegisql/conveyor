package com.aegisql.conveyor.persistence.jdbc.harness;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class Tester {

	public Tester() {
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
