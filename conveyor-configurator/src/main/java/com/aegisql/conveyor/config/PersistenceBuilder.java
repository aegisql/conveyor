package com.aegisql.conveyor.config;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.persistence.core.Persistence;

public class PersistenceBuilder implements Supplier<String>, Testing {
	
	private final static Logger LOG = LoggerFactory.getLogger(PersistenceBuilder.class);

	private String name;

	private boolean allFilesRead = false;
	
	@Override
	public String get() {
		return name;
	}
	
	public static void allFilesReadSuccessfully(PersistenceBuilder b, Boolean readOk) {
		LOG.debug("Applying allFilesReadSuccessfully={}",readOk);
		if(readOk) {
			b.allFilesRead  = readOk;
		} else {
			throw new ConveyorConfigurationException("Persistence initialization terminated because of file reading issue");
		}
	}

	@Override
	public boolean test() {
		return allFilesRead;
	}


}
