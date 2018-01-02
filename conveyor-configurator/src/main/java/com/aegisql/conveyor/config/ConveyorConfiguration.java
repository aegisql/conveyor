package com.aegisql.conveyor.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;

public class ConveyorConfiguration {

	private final static Logger LOG = LoggerFactory.getLogger(ConveyorConfiguration.class);

	private final static Lock lock = new ReentrantLock();

	public static void build(String conf, String... moreConf) throws IOException {

		Objects.requireNonNull(conf, "At least one configuration file must be provided");
		processConfFile(conf);
		if (moreConf != null) {
			for (String file : moreConf) {
				processConfFile(file);
			}
		}

	}

	private static void processConfFile(String file) throws IOException {

		lock.lock();
		try {
			if (file.toLowerCase().startsWith("classpath:")) {
				String fileSub = file.substring(10);
				file = ConveyorConfiguration.class.getClassLoader().getResource(fileSub).getPath();
			}

			if (file.toLowerCase().endsWith(".properties")) {
				processProperties(file);
			} else if (file.toLowerCase().endsWith(".yaml")) {
				processYaml(file);
			} else {
				throw new ConveyorConfigurationException("Unsupported file type " + file);
			}
		} finally {
			lock.unlock();
		}
	}

	private static void processYaml(String file) throws FileNotFoundException {
		Conveyor c = new AssemblingConveyor<>();
		c.setName("test1");
		Yaml yaml = new Yaml();
		FileReader reader = new FileReader(file);
		for (Object o : yaml.loadAll(reader)) {
			LOG.info("Object {} {}", o.getClass(), o);
		}

	}

	private static void processProperties(String file) throws IOException {
		Conveyor c = new AssemblingConveyor<>();
		c.setName("test2");
		Properties p = new Properties();
		FileReader reader = new FileReader(file);
		p.load(reader);
		for (Object o : p.entrySet()) {
			LOG.info("Object {} {}", o.getClass(), o);
		}

	}

}
