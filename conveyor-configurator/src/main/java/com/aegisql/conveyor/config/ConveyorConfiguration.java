package com.aegisql.conveyor.config;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorConfiguration.
 */
public class ConveyorConfiguration {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ConveyorConfiguration.class);

	/** The Constant lock. */
	private final static Lock lock = new ReentrantLock();

	/** The script engine. */
	public static String SCRIPT_ENGINE = "nashorn";

	/** The property prefix. */
	public static String PROPERTY_PREFIX = "CONVEYOR";
	
	/** The persistence prefix. */
	public static String PERSISTENCE_PREFIX = "PERSISTENCE";

	/** The property delimiter. */
	public static String PROPERTY_DELIMITER = ".";

	/** The default timeout msec. */
	public static long DEFAULT_TIMEOUT_MSEC = 0;

	/**
	 * Instantiates a new conveyor configuration.
	 */
	private ConveyorConfiguration() {
	}

	/**
	 * Builds the.
	 *
	 * @param conf the conf
	 * @param moreConf the more conf
	 * @throws Exception the exception
	 */
	public static void build(String conf, String... moreConf) throws Exception {

		Objects.requireNonNull(conf, "At least one configuration file must be provided");
		processConfFile(conf);
		if (moreConf != null) {
			for (String file : moreConf) {
				processConfFile(file);
			}
		}
		Conveyor<String, String, Conveyor> buildingConveyor  = getBuildingConveyor();
		Map<String,String> env = System.getenv();
		env.forEach((key,value)->{
			processPair(buildingConveyor, key, value);
		});
		Properties p = System.getProperties();
		p.forEach((key,value)->{
			processPair(buildingConveyor, ""+key, ""+value);
		});

		buildingConveyor.part().foreach().label("complete_configuration").value(true).place();
		buildingConveyor.completeAndStop().get();
		Conveyor.unRegister(buildingConveyor.getName());
	}

	/**
	 * Process conf file.
	 *
	 * @param file the file
	 */

	private static TemplateEditor templateEditor = new TemplateEditor();

	private static void processConfFile(String file) {

		lock.lock();
		try {
			if (file.toLowerCase().startsWith("classpath:")) {
				String fileSub = file.substring(10);
				file = ConveyorConfiguration.class.getClassLoader().getResource(fileSub).getPath();
			}
			ConveyorConfiguration cc;
			if (file.toLowerCase().endsWith(".properties")) {
				cc = processProperties(file);
			} else if (file.toLowerCase().endsWith(".yaml") || file.toLowerCase().endsWith(".yml")) {
				cc = processYaml(file);
			} else {
				throw new ConveyorConfigurationException("Unsupported file type " + file);
			}
			LOG.info("COMPLETE {}", cc);
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Error while processing file " + file, e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Gets the building conveyor.
	 *
	 * @return the building conveyor
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Conveyor<String, String, Conveyor> getBuildingConveyor() {

		Conveyor<String, String, Conveyor> conveyorConfiguration = null;
		try {
			conveyorConfiguration = Conveyor.byName("conveyorConfigurationBuilder");
			if (!conveyorConfiguration.isRunning()) {
				throw new RuntimeException("conveyorConfigurationBuilder is not running");
			}
		} catch (RuntimeException e) {
			conveyorConfiguration = new AssemblingConveyor<>();
			conveyorConfiguration.setBuilderSupplier(ConveyorBuilder::new);
			conveyorConfiguration.setName("conveyorConfigurationBuilder");
			conveyorConfiguration.resultConsumer(new ConveyorNameSetter(conveyorConfiguration)).set();
			conveyorConfiguration.setIdleHeartBeat(Duration.ofMillis(100));
			conveyorConfiguration.setDefaultBuilderTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MSEC));

			LabeledValueConsumer<String, ?, ConveyorBuilder> lvc = (l, v, b) -> {
				LOG.info("Unprocessed value {}={}", l, v);
			};

			conveyorConfiguration.setDefaultCartConsumer(lvc
					.<String>when("supplier", ConveyorBuilder::supplier)
					.<String>when("defaultBuilderTimeout", ConveyorBuilder::defaultBuilderTimeout)
					.<String>when("idleHeartBeat", ConveyorBuilder::idleHeartBeat)
					.<String>when("rejectUnexpireableCartsOlderThan", ConveyorBuilder::rejectUnexpireableCartsOlderThan)
					.<String>when("expirationPostponeTime", ConveyorBuilder::expirationPostponeTime)
					.<String>when("staticPart", ConveyorBuilder::staticPart)
					.<String>when("firstResultConsumer", ConveyorBuilder::firstResultConsumer)
					.<String>when("nextResultConsumer", ConveyorBuilder::nextResultConsumer)
					.<String>when("firstScrapConsumer", ConveyorBuilder::firstScrapConsumer)
					.<String>when("nextScrapConsumer", ConveyorBuilder::nextScrapConsumer)
					.<String>when("onTimeoutAction", ConveyorBuilder::timeoutAction)
					.<String>when("defaultCartConsumer", ConveyorBuilder::defaultCartConsumer)
					.<String>when("readinessEvaluator", ConveyorBuilder::readinessEvaluator)
					.<String>when("builderSupplier", ConveyorBuilder::builderSupplier)
					.<String>when("addBeforeKeyEvictionAction", ConveyorBuilder::addBeforeKeyEvictionAction)
					.<String>when("addCartBeforePlacementValidator", ConveyorBuilder::addCartBeforePlacementValidator)
					.<String>when("addBeforeKeyReschedulingAction", ConveyorBuilder::addBeforeKeyReschedulingAction)
					.<String>when("acceptLabels", ConveyorBuilder::acceptLabels)
					.<String>when("enablePostponeExpiration", ConveyorBuilder::enablePostponeExpiration)
					.<String>when("enablePostponeExpirationOnTimeout",ConveyorBuilder::enablePostponeExpirationOnTimeout)
					.<String>when("autoAcknowledge", ConveyorBuilder::autoAcknowledge)
					.<String>when("acknowledgeAction", ConveyorBuilder::acknowledgeAction)
					.<String>when("autoAcknowledgeOnStatus", ConveyorBuilder::autoAcknowledgeOnStatus)
					.<String>when("cartPayloadAccessor", ConveyorBuilder::cartPayloadAccessor)
					.<String>when("forward", ConveyorBuilder::forward)
					.<String>when("completed", ConveyorBuilder::completed)
					.<String>when("dependency", ConveyorBuilder::dependency)
					.<String>when("parallel", ConveyorBuilder::parallel)
					.<String>when("maxQueueSize", ConveyorBuilder::maxQueueSize)
					.<String>when("priority", ConveyorBuilder::priority)
					.<String>when("persistence", ConveyorBuilder::persitence)
					.<String>when("readyWhenAccepted", ConveyorBuilder::readyWhen)
					.<PersistenceProperty>when("persistenceProperty", ConveyorBuilder::persistenceProperty)
					.<Boolean>when("complete_configuration", ConveyorBuilder::allFilesReadSuccessfully));
			conveyorConfiguration.part().id("__PERSISTENCE__").label("builderSupplier").value("null").place();
			conveyorConfiguration.staticPart().label("dependency").value("__PERSISTENCE__").place();
		}
			
		
		return conveyorConfiguration;
	}

	/**
	 * Process yaml.
	 *
	 * @param file the file
	 * @return the conveyor configuration
	 * @throws Exception the exception
	 */
	private static ConveyorConfiguration processYaml(String file) throws Exception {
		ConveyorConfiguration cc = new ConveyorConfiguration();
		Yaml yaml = new Yaml();
		FileReader reader = new FileReader(file);
		Conveyor<String, String, Conveyor> buildingConveyor = getBuildingConveyor();
		Iterable iter = (Iterable) yaml.loadAll(reader);
		for (Object o : iter) {
			LOG.info("YAML Iter {} {}", o.getClass(), o);
			Map<String, Object> map = (Map<String, Object>) o;
			map.forEach((key, value) -> {
				LOG.info("Value {} {} {}", key, value.getClass(), value);
				processPair(buildingConveyor, key, value);
			});
		}
		return cc;
	}

	/**
	 * Process properties.
	 *
	 * @param file the file
	 * @return the conveyor configuration
	 * @throws Exception the exception
	 */
	private static ConveyorConfiguration processProperties(String file) throws Exception {
		ConveyorConfiguration cc = new ConveyorConfiguration();
		OrderedProperties p = new OrderedProperties();
		p.load(file);
		Conveyor<String, String, Conveyor> buildingConveyor = getBuildingConveyor();
		for (Pair<String, String> o : p) {
			String key = o.label;
			String value = o.value;
			processPair(buildingConveyor, key, value);
		}
		return cc;
	}

	/**
	 * Process pair.
	 *
	 * @param buildingConveyor the building conveyor
	 * @param key the key
	 * @param obj the obj
	 */
	private static void processPair(Conveyor<String, String, Conveyor> buildingConveyor, String key, Object obj) {
		ConveyorProperty.eval(key, obj, cp->{
			String value = cp.getValueAsString();

			if(cp.isConveyorProperty()) {
				if(cp.isDefaultProperty()) {
					if ("postInit".equals(cp.getProperty())) {
						buildingConveyor.resultConsumer().andThen(
								(ResultConsumer<String, Conveyor>) ConfigUtils.stringToResultConsumerSupplier.apply(value))
								.set();
					} else if ("postFailure".equals(cp.getProperty())) {
						buildingConveyor.scrapConsumer().andThen(
								(ScrapConsumer<String, ?>) ConfigUtils.stringToScrapConsumerSupplier.apply(value))
								.set();
					} else if ("persistenceProperty".equals(cp.getProperty())) {
						buildingConveyor.part().id("__PERSISTENCE__").label("persistenceProperty").value(cp.getValue()).place();
					} else {
						buildingConveyor.staticPart().label(cp.getProperty()).value(value).place();
						buildingConveyor.part().foreach().label(cp.getProperty()).value(value).place();
					}
				} else {
					if("postInit".equals(cp.getProperty())) {
						buildingConveyor.resultConsumer()
								.andThen((ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(value)).id(cp.getName())
								.set();
					} else if ("persistenceProperty".equals(cp.getProperty())) {
						buildingConveyor.part().id(cp.getName()).label(cp.getProperty()).value(cp.getValue()).place();
					} else {
						buildingConveyor.part().id(cp.getName()).label(cp.getProperty()).value(value).place();
					}

				}
			}
		});
		
		PersistenceProperty.eval(key, obj, pp->{
			buildingConveyor.part().id("__PERSISTENCE__").label("persistenceProperty").value(pp).place();
		});
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ConveyorConfiguration";
	}

}
