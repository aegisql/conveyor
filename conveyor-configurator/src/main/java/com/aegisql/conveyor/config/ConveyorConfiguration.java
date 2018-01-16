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
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

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
		getBuildingConveyor().part().foreach().label("complete_configuration").value(true).place();
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
			instance.resultConsumer(new ConveyorNameSetter(instance)).set();
			instance.setIdleHeartBeat(Duration.ofMillis(100));
			
			LabeledValueConsumer<String, ?, ConveyorBuilder> lvc = (l,v,b)->{
				LOG.info("Unprocessed value {}={}",l,v);
			};
			
			instance.setDefaultCartConsumer(lvc
					.<String>when("defaultBuilderTimeout", ConveyorBuilder::defaultBuilderTimeout )
					.<String>when("idleHeartBeat", ConveyorBuilder::idleHeartBeat )
					.<String>when("rejectUnexpireableCartsOlderThan", ConveyorBuilder::rejectUnexpireableCartsOlderThan )
					.<String>when("expirationPostponeTime", ConveyorBuilder::expirationPostponeTime )
					.<String>when("staticPart", ConveyorBuilder::staticPart )
					.<String>when("firstResultConsumer", ConveyorBuilder::firstResultConsumer )
					.<String>when("nextResultConsumer", ConveyorBuilder::nextResultConsumer )
					.<String>when("firstScrapConsumer", ConveyorBuilder::firstScrapConsumer )
					.<String>when("nextScrapConsumer", ConveyorBuilder::nextScrapConsumer )
					.<String>when("onTimeoutAction", ConveyorBuilder::timeoutAction )
					.<String>when("defaultCartConsumer", ConveyorBuilder::defaultCartConsumer )
					.<String>when("readinessEvaluator", ConveyorBuilder::readinessEvaluator )
					.<String>when("builderSupplier", ConveyorBuilder::builderSupplier )
					.<String>when("addBeforeKeyEvictionAction", ConveyorBuilder::addBeforeKeyEvictionAction )
					.<String>when("addCartBeforePlacementValidator", ConveyorBuilder::addCartBeforePlacementValidator )
					.<String>when("addBeforeKeyReschedulingAction", ConveyorBuilder::addBeforeKeyReschedulingAction )
					.<String>when("acceptLabels", ConveyorBuilder::acceptLabels)
					.<String>when("enablePostponeExpiration", ConveyorBuilder::enablePostponeExpiration )
					.<String>when("enablePostponeExpirationOnTimeout", ConveyorBuilder::enablePostponeExpirationOnTimeout )
					.<String>when("autoAcknowledge", ConveyorBuilder::autoAcknowledge )
					.<String>when("acknowledgeAction", ConveyorBuilder::acknowledgeAction )
					.<String>when("autoAcknowledgeOnStatus", ConveyorBuilder::autoAcknowledgeOnStatus )
					.<String>when("cartPayloadAccessor", ConveyorBuilder::cartPayloadAccessor )
					.<String>when("forward", ConveyorBuilder::forward )
					.<String>when("completed", ConveyorBuilder::completed )
					.<String>when("dependency", ConveyorBuilder::dependency )
					.<Boolean>when("complete_configuration", ConveyorBuilder::allFilesReadSuccessfully )
					);
						
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
				if("postInit".equals(propertyName)) {
					buildingConveyor.resultConsumer().andThen((ResultConsumer<String, Conveyor>) ConfigUtils.stringToResultConsumerSupplier.apply(value)).set();
				} else {
					buildingConveyor.staticPart().label(propertyName).value(value).place();
					buildingConveyor.part().foreach().label(propertyName).value(value).place();
				}
			} else {
				if("postInit".equals(propertyName)) {
					buildingConveyor.resultConsumer().andThen((ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(value)).id(name).set();
				} else {
					buildingConveyor.part().id(name).label(propertyName).value(value).place();
				}
			}
		}
		return cc;
	}

	@Override
	public String toString() {
		return "ConveyorConfiguration";
	}
	
	

}
