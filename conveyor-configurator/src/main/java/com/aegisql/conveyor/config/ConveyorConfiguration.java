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

import static com.aegisql.conveyor.config.ConveyorProperty.ConveyorPropertyType.CONVEYOR;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorConfiguration.
 */
public class ConveyorConfiguration {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ConveyorConfiguration.class);

	/** The Constant lock. */
	private final static Lock lock = new ReentrantLock();
	public static String DEFAULT_PERSISTENCE_NAME = "__PERSISTENCE__";

	/** The property prefix. */
	public static String PROPERTY_PREFIX = "CONVEYOR";
	
	/** The persistence prefix. */
	public static String PERSISTENCE_PREFIX = "PERSISTENCE";

	public static String METAINFO_PREFIX = "METAINFO";

	/** The property delimiter. */
	public static String PROPERTY_DELIMITER = ".";

	public static String JAVAPATH_PREFIX = "JAVAPATH:";

	/** The default timeout msec. */
	public static long DEFAULT_TIMEOUT_MSEC = 0;

	private final static ConfigurationPaths configurationPaths = new ConfigurationPaths();

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
		processConfParameter(conf);
		if (moreConf != null) {
			for (String file : moreConf) {
				processConfParameter(file);
			}
		}
		Conveyor<String, String, Conveyor> buildingConveyor  = getBuildingConveyor();
		Map<String,String> env = System.getenv();
		env.forEach((key,value)-> processPair(buildingConveyor, key, value));
		Properties p = System.getProperties();
		p.forEach((key,value)-> processPair(buildingConveyor, ""+key, ""+value));

		buildingConveyor.part().foreach().label("complete_configuration").value(true).place();
		buildingConveyor.completeAndStop().get();
		Conveyor.unRegister(buildingConveyor.getName());
	}

	private static void processConfParameter(String file) {

		lock.lock();
		try {
			if (file.toLowerCase().startsWith("classpath:") || file.toLowerCase().startsWith("cp:")) {
				String fileSub = file.substring(file.indexOf(":")+1);
				file = ConveyorConfiguration.class.getClassLoader().getResource(fileSub).getPath();
			}
			if (file.toLowerCase().startsWith("javapath:") || file.toLowerCase().startsWith("jp:")) {
				String className = file.substring(file.indexOf(":")+1);
				registerPath(className);
				return;
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

	static void registerPath(String className){
		configurationPaths.register(className);
	}

	/**
	 * Gets the building conveyor.
	 *
	 * @return the building conveyor
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Conveyor<String, String, Conveyor> getBuildingConveyor() {

		Conveyor<String, String, Conveyor> conveyorConfiguration;
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

			LabeledValueConsumer<String, ?, ConveyorBuilder> lvc = (l, v, b) -> LOG.info("Unprocessed value {}={}", l, v);

			conveyorConfiguration.setDefaultCartConsumer(lvc
					.<ConveyorProperty>when("javaPath", ConveyorBuilder::registerPath)
					.<ConveyorProperty>when("supplier", ConveyorBuilder::supplier)
					.<ConveyorProperty>when("defaultBuilderTimeout", ConveyorBuilder::defaultBuilderTimeout)
					.<ConveyorProperty>when("idleHeartBeat", ConveyorBuilder::idleHeartBeat)
					.<ConveyorProperty>when("rejectUnexpireableCartsOlderThan", ConveyorBuilder::rejectUnexpireableCartsOlderThan)
					.<ConveyorProperty>when("expirationPostponeTime", ConveyorBuilder::expirationPostponeTime)
					.<ConveyorProperty>when("staticPart", ConveyorBuilder::staticPart)
					.<ConveyorProperty>when("firstResultConsumer", ConveyorBuilder::firstResultConsumer)
					.<ConveyorProperty>when("nextResultConsumer", ConveyorBuilder::nextResultConsumer)
					.<ConveyorProperty>when("firstScrapConsumer", ConveyorBuilder::firstScrapConsumer)
					.<ConveyorProperty>when("nextScrapConsumer", ConveyorBuilder::nextScrapConsumer)
					.<ConveyorProperty>when("onTimeoutAction", ConveyorBuilder::timeoutAction)
					.<ConveyorProperty>when("defaultCartConsumer", ConveyorBuilder::defaultCartConsumer)
					.<ConveyorProperty>when("readinessEvaluator", ConveyorBuilder::readinessEvaluator)
					.<ConveyorProperty>when("builderSupplier", ConveyorBuilder::builderSupplier)
					.<ConveyorProperty>when("addBeforeKeyEvictionAction", ConveyorBuilder::addBeforeKeyEvictionAction)
					.<ConveyorProperty>when("addCartBeforePlacementValidator", ConveyorBuilder::addCartBeforePlacementValidator)
					.<ConveyorProperty>when("addBeforeKeyReschedulingAction", ConveyorBuilder::addBeforeKeyReschedulingAction)
					.<ConveyorProperty>when("acceptLabels", ConveyorBuilder::acceptLabels)
					.<ConveyorProperty>when("enablePostponeExpiration", ConveyorBuilder::enablePostponeExpiration)
					.<ConveyorProperty>when("enablePostponeExpirationOnTimeout",ConveyorBuilder::enablePostponeExpirationOnTimeout)
					.<ConveyorProperty>when("autoAcknowledge", ConveyorBuilder::autoAcknowledge)
					.<ConveyorProperty>when("acknowledgeAction", ConveyorBuilder::acknowledgeAction)
					.<ConveyorProperty>when("autoAcknowledgeOnStatus", ConveyorBuilder::autoAcknowledgeOnStatus)
					.<ConveyorProperty>when("cartPayloadAccessor", ConveyorBuilder::cartPayloadAccessor)
					.<ConveyorProperty>when("forward", ConveyorBuilder::forward)
					.<String>when("completed", ConveyorBuilder::completed)
					.<String>when("dependency", ConveyorBuilder::dependency)
					.<ConveyorProperty>when("parallel", ConveyorBuilder::parallel)
					.<ConveyorProperty>when("maxQueueSize", ConveyorBuilder::maxQueueSize)
					.<ConveyorProperty>when("priority", ConveyorBuilder::priority)
					.<ConveyorProperty>when("persistence", ConveyorBuilder::persitence)
					.<ConveyorProperty>when("readyWhenAccepted", ConveyorBuilder::readyWhen)
					.<ConveyorProperty>when("keyType", ConveyorBuilder::keyType)
					.<ConveyorProperty>when("labelType", ConveyorBuilder::labelType)
					.<ConveyorProperty>when("productType", ConveyorBuilder::productType)
					.<ConveyorProperty>when("supportedValueTypes", ConveyorBuilder::supportedValueTypes)
					.<PersistenceProperty>when("persistenceProperty", ConveyorBuilder::persistenceProperty)
					.when("complete_configuration", ConveyorBuilder::allFilesReadSuccessfully));
			conveyorConfiguration.part().id(DEFAULT_PERSISTENCE_NAME).label("builderSupplier").value(ConveyorProperty.NULL_PROPERTY).place();
			conveyorConfiguration.staticPart().label("dependency").value(DEFAULT_PERSISTENCE_NAME).place();
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
		Iterable iter = yaml.loadAll(reader);
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

		//Every property evaluated as ConveyorProperty
		ConveyorProperty.eval(key, obj, cp->{
			String strValue = cp.getValueAsString();
			if(CONVEYOR == cp.getConveyorPropertyType()) {
				if(cp.isDefaultProperty()) {
					if ("postInit".equals(cp.getProperty())) {
						buildingConveyor.resultConsumer().andThen(
								(ResultConsumer<String, Conveyor>) ConfigUtils.stringToResultConsumerSupplier.apply(strValue))
								.set();
					} else if ("postFailure".equals(cp.getProperty())) {
						buildingConveyor.scrapConsumer().andThen(
								(ScrapConsumer<String, ?>) ConfigUtils.stringToScrapConsumerSupplier.apply(strValue))
								.set();
					} else if ("persistenceProperty".equals(cp.getProperty())) {
						buildingConveyor.part().id(DEFAULT_PERSISTENCE_NAME).label("persistenceProperty").value(cp.getValue()).place();
					} else {
						buildingConveyor.staticPart().label(cp.getProperty()).value(cp).place();
						buildingConveyor.part().foreach().label(cp.getProperty()).value(cp).place();
					}
				} else {
					if("postInit".equals(cp.getProperty())) {
						buildingConveyor.resultConsumer()
								.andThen((ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(strValue)).id(cp.getConveyorName())
								.set();
					} else if ("persistenceProperty".equals(cp.getProperty())) {
						buildingConveyor.part().id(cp.getConveyorName()).label(cp.getProperty()).value(cp.getValue()).place();
					} else {
						buildingConveyor.part().id(cp.getConveyorName()).label(cp.getProperty()).value(cp).place();
					}

				}
			}
		});
		//Every property evaluated as a default PersistenceProperty
		PersistenceProperty.eval(key, obj, pp-> buildingConveyor.part().id(DEFAULT_PERSISTENCE_NAME).label("persistenceProperty").value(pp).place());
	}

	public static void registerBean(Object bean, String... names) {
		configurationPaths.registerBean(bean,names);
	}

	public static Object evalPath(String path) {
		return configurationPaths.evalPath(path);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ConveyorConfiguration";
	}

}
