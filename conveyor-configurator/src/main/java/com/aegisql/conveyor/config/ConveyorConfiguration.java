package com.aegisql.conveyor.config;

import java.io.FileReader;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;

public class ConveyorConfiguration {

	private final static Logger LOG = LoggerFactory.getLogger(ConveyorConfiguration.class);

	private final static Lock lock = new ReentrantLock();
	
	private final static Map<String,Function<String,Object>> stringConverters = new LinkedHashMap<>();
	
	static {
		stringConverters.put("idleHeartBeat", ConfigUtils.timeToMillsConverter);
		stringConverters.put("defaultBuilderTimeout", ConfigUtils.timeToMillsConverter);
		stringConverters.put("rejectUnexpireableCartsOlderThan", ConfigUtils.timeToMillsConverter);
		stringConverters.put("expirationPostponeTime", ConfigUtils.timeToMillsConverter);
		stringConverters.put("enablePostponeExpiration", Boolean::valueOf);
		stringConverters.put("enablePostponeExpirationOnTimeout", Boolean::valueOf);
		stringConverters.put("autoAcknowledge", Boolean::valueOf);
		stringConverters.put("autoAcknowledgeOnStatus", ConfigUtils.stringToStatusConverter);
		stringConverters.put("builderSupplier", ConfigUtils.stringToBuilderSupplier);
		stringConverters.put("firstResultConsumer", ConfigUtils.stringToResultConsumerSupplier);
		stringConverters.put("nextResultConsumer", ConfigUtils.stringToResultConsumerSupplier);
		stringConverters.put("firstScrapConsumer", ConfigUtils.stringToScrapConsumerSupplier);
		stringConverters.put("nextScrapConsumer", ConfigUtils.stringToScrapConsumerSupplier);
		stringConverters.put("staticPart", ConfigUtils.stringToLabelValuePairSupplier);
		stringConverters.put("onTimeoutAction", ConfigUtils.stringToConsumerSupplier);
		stringConverters.put("defaultCartConsumer", ConfigUtils.stringToLabeledValueConsumerSupplier);
		stringConverters.put("readinessEvaluator", ConfigUtils.stringToReadinessEvaluatorSupplier);
		stringConverters.put("addCartBeforePlacementValidator", ConfigUtils.stringToConsumerSupplier);
		stringConverters.put("addBeforeKeyEvictionAction", ConfigUtils.stringToConsumerSupplier);
		stringConverters.put("addBeforeKeyReschedulingAction", ConfigUtils.stringToBiConsumerSupplier);
		stringConverters.put("acceptLabels", ConfigUtils.stringToLabelArraySupplier);
		stringConverters.put("acknowledgeAction", ConfigUtils.stringToConsumerSupplier);
		stringConverters.put("cartPayloadAccessor", ConfigUtils.stringToCartPayloadFunctionSupplier);
		
	}
	
	ConveyorConfiguration() {
	}

	public static void build(String conf, String... moreConf) throws Exception {

		Objects.requireNonNull(conf, "At least one configuration file must be provided");
		processConfFile(conf);
		if (moreConf != null) {
			for (String file : moreConf) {
				processConfFile(file);
			}
		}
		getBuildingConveyor().part().foreach().label("complete_configuration").place();
		getBuildingConveyor().completeAndStop().get();
	}

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
			LOG.info("COMPLETE {}",cc);
		} catch(Exception e) {
			throw new ConveyorConfigurationException("Error while processing file "+file,e);
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Conveyor<String,String,Conveyor> getBuildingConveyor() {
		
		Conveyor<String,String,Conveyor> instance = null;
		try {
			instance = Conveyor.byName("conveyorConfigurationBuilder");
			if(! instance.isRunning() ) {
				throw new RuntimeException("conveyorConfigurationBuilder is not running");
			}
		} catch(RuntimeException e) {
			instance = new AssemblingConveyor<>();
			instance.setBuilderSupplier(ConveyorBuilder::new);
			instance.setName("conveyorConfigurationBuilder");
			instance.resultConsumer(new ConveyorNameSetter()).set();
			instance.setIdleHeartBeat(Duration.ofMillis(100));
			
			instance.setDefaultCartConsumer(Conveyor.getConsumerFor(instance, ConveyorBuilder.class)
					.<String>match(".*", (b,s)->LOG.info("Unprocessed value {}",s))
					.<String>when("defaultBuilderTimeout", (b,s)->{ConveyorBuilder.defaultBuilderTimeout(b,s);})
					.<String>when("idleHeartBeat", (b,s)->{ConveyorBuilder.idleHeartBeat(b,s);})
					.<String>when("rejectUnexpireableCartsOlderThan", (b,s)->{ConveyorBuilder.rejectUnexpireableCartsOlderThan(b,s);})
					.<String>when("expirationPostponeTime", (b,s)->{ConveyorBuilder.expirationPostponeTime(b,s);})
					.<String>when("staticPart", (b,s)->{ConveyorBuilder.staticPart(b,s);})
					.<String>when("firstResultConsumer", (b,s)->{ConveyorBuilder.firstResultConsumer(b,s);})
					.<String>when("nextResultConsumer", (b,s)->{ConveyorBuilder.nextResultConsumer(b,s);})
					.<String>when("firstScrapConsumer", (b,s)->{ConveyorBuilder.firstScrapConsumer(b,s);})
					.<String>when("nextScrapConsumer", (b,s)->{ConveyorBuilder.nextScrapConsumer(b,s);})
					.<String>when("onTimeoutAction", (b,s)->{ConveyorBuilder.timeoutAction(b,s);})
					.<String>when("defaultCartConsumer", (b,s)->{ConveyorBuilder.defaultCartConsumer(b,s);})
					.<String>when("readinessEvaluator", (b,s)->{ConveyorBuilder.readinessEvaluator(b,s);})
					.<String>when("builderSupplier", (b,s)->{ConveyorBuilder.builderSupplier(b,s);})
					.<String>when("addBeforeKeyEvictionAction", (b,s)->{ConveyorBuilder.addBeforeKeyEvictionAction(b,s);})
					.<String>when("addCartBeforePlacementValidator", (b,s)->{ConveyorBuilder.addCartBeforePlacementValidator(b,s);})
					.<String>when("addBeforeKeyReschedulingAction", (b,s)->{ConveyorBuilder.addBeforeKeyReschedulingAction(b,s);})
					.<String>when("acceptLabels", (b,s)->ConveyorBuilder.acceptLabels(b,s))
					.<String>when("enablePostponeExpiration", (b,s)->ConveyorBuilder.enablePostponeExpiration(b,s))
					.<String>when("enablePostponeExpirationOnTimeout", (b,s)->ConveyorBuilder.enablePostponeExpirationOnTimeout(b,s))
					.<String>when("autoAcknowledge", (b,s)->ConveyorBuilder.autoAcknowledge(b,s))
					.<String>when("acknowledgeAction", (b,s)->ConveyorBuilder.acknowledgeAction(b,s))
					.<String>when("autoAcknowledgeOnStatus", (b,s)->ConveyorBuilder.autoAcknowledgeOnStatus(b,s))
					.<String>when("cartPayloadAccessor", (b,s)->ConveyorBuilder.cartPayloadAccessor(b,s))
					
					.when("complete_configuration", ()->LOG.info("complete_configuration received"))
					);
			
			instance.setReadinessEvaluator(Conveyor.getTesterFor(instance).accepted("complete_configuration"));
			
		}
		
		return instance;
	}
	
	private static ConveyorConfiguration processYaml(String file) throws Exception {
		ConveyorConfiguration cc = new ConveyorConfiguration();
		Conveyor c = new AssemblingConveyor<>();
		c.setName("test1");
		Yaml yaml = new Yaml();
		FileReader reader = new FileReader(file);
		for (Object o : yaml.loadAll(reader)) {
			LOG.info("YAML Object {} {}", o.getClass(), o);
		}
		return cc;
	}

	private static ConveyorConfiguration processProperties(String file) throws Exception {
		ConveyorConfiguration cc = new ConveyorConfiguration();
		OrderedProperties p = new OrderedProperties();
		p.load(file);
		Conveyor<String,String,Conveyor> buildingConveyor = getBuildingConveyor();
		for (Pair<String, String> o : p) {
			String key   = o.label;
			String value = o.value;
			if( ! key.toUpperCase().startsWith("CONVEYOR.") ){
				continue;
			}
			String[] fields = key.split("\\.");
			String name         = null;
			String propertyName = null;
			if(fields.length == 2) {
				name = null;
				propertyName = fields[1];
			} else {
				String[] nameParts = new String[fields.length-2];
				for(int i = 1; i < fields.length-1; i++) {
					nameParts[i-1] = fields[i];
				}
				name = String.join("", nameParts);
				propertyName = fields[fields.length-1];
			}
			if(name == null) {
				buildingConveyor.staticPart().label(propertyName).value(value).place();
				buildingConveyor.part().foreach().label(propertyName).value(value).place();
			} else {
				buildingConveyor.part().id(name).label(propertyName).value(value).place();
			}
		}
		return cc;
	}

	@Override
	public String toString() {
		return "ConveyorConfiguration";
	}
	
	

}
