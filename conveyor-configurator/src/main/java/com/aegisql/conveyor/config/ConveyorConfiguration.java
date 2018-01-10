package com.aegisql.conveyor.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

public class ConveyorConfiguration {

	private final Map<String,Map<String,List<Object>>> properties = new LinkedHashMap<String, Map<String,List<Object>>>();
	
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
		properties.put(null, new LinkedHashMap<>());
	}

	public static void build(String conf, String... moreConf) throws IOException {

		Objects.requireNonNull(conf, "At least one configuration file must be provided");
		processConfFile(conf);
		if (moreConf != null) {
			for (String file : moreConf) {
				processConfFile(file);
			}
		}

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

	private static ConveyorConfiguration processYaml(String file) throws FileNotFoundException {
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

	private static ConveyorConfiguration processProperties(String file) throws IOException {
		ConveyorConfiguration cc = new ConveyorConfiguration();
		OrderedProperties p = new OrderedProperties();
		p.load(file);
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
				LOG.info("CONV DEFAULT PROPERTY NAME {}={}", propertyName,value);
			} else {
				String[] nameParts = new String[fields.length-2];
				for(int i = 1; i < fields.length-1; i++) {
					nameParts[i-1] = fields[i];
				}
				name = String.join("", nameParts);
				propertyName = fields[fields.length-1];
				LOG.info("CONV {} PROPERTY NAME {}={}", name,propertyName,value);
			}
			cc.applyPropertyValue(name, propertyName,value);
			
		}
		final Map<String,List<Object>> defaults = cc.properties.get(null);
		
		cc.properties.forEach((name,values)->{
			//apply defaults
			//????? do I want to merge it here
			for(String defProperty:defaults.keySet()) {
				if( ! values.containsKey(defProperty)) {
					values.put(defProperty, defaults.get(defProperty));
				}
			}
			if(name != null) {
				Conveyor c = null;
				try {
					c = Conveyor.byName(name);
				} catch (Exception e) {
				}
				if(c==null) {
					c = new AssemblingConveyor<>();
					c.setName(name);
				}
				final Conveyor conv = c;
				for(String property:values.keySet()) {
					switch (property) {
					case "idleHeartBeat":
						values.get("idleHeartBeat").forEach(obj->{
							long time1 = (long)obj;
							LOG.debug("Apply {}.setIdleHeartBeat({},TimeUnit.MILLISECONDS)",name,time1);
							conv.setIdleHeartBeat(time1,TimeUnit.MILLISECONDS);
						});
						break;
					case "defaultBuilderTimeout":
						values.get("defaultBuilderTimeout").forEach(obj->{
							long time2 = (long)obj;
							LOG.debug("Apply {}.setDefaultBuilderTimeout({},TimeUnit.MILLISECONDS)",name,time2);
							conv.setDefaultBuilderTimeout(time2,TimeUnit.MILLISECONDS);
						});
						break;
					case "rejectUnexpireableCartsOlderThan":
						values.get("rejectUnexpireableCartsOlderThan").forEach(obj->{
							long time3 = (long)obj;
							LOG.debug("Apply {}.rejectUnexpireableCartsOlderThan({},TimeUnit.MILLISECONDS)",name,time3);
							conv.rejectUnexpireableCartsOlderThan(time3,TimeUnit.MILLISECONDS);
						});
						break;
					case "expirationPostponeTime":
						values.get("expirationPostponeTime").forEach(obj->{
							long time4 = (long)obj;
							LOG.debug("Apply {}.expirationPostponeTime({},TimeUnit.MILLISECONDS)",name,time4);
							conv.setExpirationPostponeTime(time4,TimeUnit.MILLISECONDS);
						});
						break;
					case "enablePostponeExpiration":
						values.get("enablePostponeExpiration").forEach(obj->{
							boolean flag1 = (boolean)obj;
							LOG.debug("Apply {}.enablePostponeExpiration({})",name,flag1);
							conv.enablePostponeExpiration(flag1);
						});
						break;
					case "enablePostponeExpirationOnTimeout":
						values.get("enablePostponeExpirationOnTimeout").forEach(obj->{
							boolean flag2 = (boolean)obj;
							LOG.debug("Apply {}.enablePostponeExpirationOnTimeout({})",name,flag2);
							conv.enablePostponeExpirationOnTimeout(flag2);
						});
						break;
					case "autoAcknowledge":
						values.get("autoAcknowledge").forEach(obj->{
							boolean flag3 = (boolean)obj;
							LOG.debug("Apply {}.autoAcknowledge({})",name,flag3);
							conv.setAutoAcknowledge(flag3);
						});
						break;
					case "autoAcknowledgeOnStatus":
						values.get("autoAcknowledgeOnStatus").forEach(obj->{
							Status[] statuses = (Status[])obj;
							if(statuses.length != 0) {
								Status first  = statuses[0];
								Status[] more = null;
								if(statuses.length > 1) {
									more = new Status[statuses.length-1];
									for(int i = 1; i < statuses.length; i++) {
										more[i-1] = statuses[i];
									}
								}
								conv.autoAcknowledgeOnStatus(first, more);
							}
						});
						break;
					case "builderSupplier":
						values.get("builderSupplier").forEach(obj->{
							BuilderSupplier bs = (BuilderSupplier)obj;
							LOG.debug("Apply {}.builderSupplier({})",name,bs.getClass());
							conv.setBuilderSupplier(bs);
						});
						break;
					case "firstResultConsumer":
						values.get("firstResultConsumer").forEach(obj->{
							ResultConsumer rc = (ResultConsumer) obj;
							LOG.debug("Apply {}.firstResultConsumer({})",name,rc);
							conv.resultConsumer(rc).set();
						});
						break;
					case "nextResultConsumer":
						values.get("nextResultConsumer").forEach(obj->{
							ResultConsumer rc = (ResultConsumer) obj;
							LOG.debug("Apply {}.nextResultConsumer({})",name,rc);
							conv.resultConsumer().andThen(rc).set();
						});
						break;
					case "firstScrapConsumer":
						values.get("firstScrapConsumer").forEach(obj->{
							ScrapConsumer sc = (ScrapConsumer) obj;
							LOG.debug("Apply {}.firstScrapConsumer({})",name,sc);
							conv.scrapConsumer(sc).set();
						});
						break;
					case "nextScrapConsumer":
						values.get("nextScrapConsumer").forEach(obj->{
							ScrapConsumer sc = (ScrapConsumer) obj;
							LOG.debug("Apply {}.nextScrapConsumer({})",name,sc);
							conv.scrapConsumer().andThen(sc).set();
						});
						break;
					case "staticPart":
						values.get("staticPart").forEach(obj->{
							Pair pair = (Pair)obj;
							LOG.debug("Apply {}.staticPart({})",name,pair);
							conv.staticPart().label(pair.label).value(pair.value).place();
						});
						break;
					case "onTimeoutAction":
						values.get("onTimeoutAction").forEach(obj->{
							Consumer ta = (Consumer) obj;
							LOG.debug("Apply {}.onTimeoutAction({})",name,ta);
							conv.setOnTimeoutAction(ta);
						});
						break;
					case "defaultCartConsumer":
						values.get("defaultCartConsumer").forEach(obj->{
							LabeledValueConsumer ta = (LabeledValueConsumer) obj;
							LOG.debug("Apply {}.defaultCartConsumer({})",name,ta);
							conv.setDefaultCartConsumer(ta);
						});
						break;
					case "readinessEvaluator":
						values.get("readinessEvaluator").forEach(obj->{
							if(obj instanceof BiPredicate) {
								BiPredicate re = (BiPredicate) obj;
								LOG.debug("Apply {}.readinessEvaluator(BiPredicate {})",name,re);
								conv.setReadinessEvaluator(re);
							} else if(obj instanceof Predicate) {
								Predicate re = (Predicate) obj;
								LOG.debug("Apply {}.readinessEvaluator(Predicate {})",name,re);
								conv.setReadinessEvaluator(re);
							} else {
								throw new ConveyorConfigurationException("Unexpected readinessEvaluator type "+obj.getClass());
							}
						});
						break;
					case "addCartBeforePlacementValidator":
						values.get("addCartBeforePlacementValidator").forEach(obj->{
							Consumer cons = (Consumer) obj;
							LOG.debug("Apply {}.addCartBeforePlacementValidator({})",name,cons);
							conv.addCartBeforePlacementValidator(cons);
						});
						break;
					case "addBeforeKeyEvictionAction":
						values.get("addBeforeKeyEvictionAction").forEach(obj->{
							Consumer cons = (Consumer) obj;
							LOG.debug("Apply {}.addBeforeKeyEvictionAction({})",name,cons);
							conv.addBeforeKeyEvictionAction(cons);
						});
						break;
					case "addBeforeKeyReschedulingAction":
						values.get("addBeforeKeyReschedulingAction").forEach(obj->{
							BiConsumer cons = (BiConsumer) obj;
							LOG.debug("Apply {}.addBeforeKeyReschedulingAction({})",name,cons);
							conv.addBeforeKeyReschedulingAction(cons);
						});
						break;
					case "acceptLabels":
						values.get("acceptLabels").forEach(obj->{
							Object[] array = (Object[]) obj;
							LOG.debug("Apply {}.acceptLabels({})",name,array);
							conv.acceptLabels(array);
						});
						break;
					case "acknowledgeAction":
						values.get("acknowledgeAction").forEach(obj->{
							Consumer cons = (Consumer) obj;
							LOG.debug("Apply {}.acknowledgeAction({})",name,cons);
							conv.setAcknowledgeAction(cons);
						});
						break;
					case "cartPayloadAccessor":
						values.get("cartPayloadAccessor").forEach(obj->{
							Function f = (Function) obj;
							LOG.debug("Apply {}.cartPayloadAccessor({})",name,f);
							conv.setCartPayloadAccessor(f);;
						});
						break;
					default:
						LOG.warn("Unexpected property name {} in conveyor {} in {}",property,name,file);
					}
				}
			}
		});
		return cc;
	}

	private void applyPropertyValue(String name, String propertyName, String value) {
		Map<String,List<Object>> property = properties.get(name);
		if(property == null) {
			property = new LinkedHashMap<>();
			properties.put(name, property);
		}
		List<Object> list = property.get(propertyName);
		if(list == null) {
			list = new ArrayList<>();
			property.put(propertyName, list);
		}
		list.add(stringConverters.get(propertyName).apply(value));
	}

	@Override
	public String toString() {
		return "ConveyorConfiguration [properties=" + properties + "]";
	}
	
	

}
