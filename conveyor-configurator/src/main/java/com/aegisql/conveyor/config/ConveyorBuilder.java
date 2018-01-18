package com.aegisql.conveyor.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.ForwardResult.ForwardingConsumer;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;

@SuppressWarnings("rawtypes")
public class ConveyorBuilder implements Supplier<Conveyor>, Testing {

	//readiness
	private boolean allFilesRead = false;
	private Set<String> dependencies = new HashSet<>();
	private Set<String> completed    = new HashSet<>();
	
	//setters
	private static final long serialVersionUID = 1L;
	private Supplier<Conveyor> constructor                        = AssemblingConveyor::new;
	private Duration idleHeartBeat                                = null;
	private Duration defaultBuilderTimeout                        = null;
	private Duration rejectUnexpireableCartsOlderThan             = null;
	private Duration expirationPostponeTime                       = null;
	private Collection<Pair> staticParts                          = new LinkedList<>();
	private ResultConsumer firstResultConsumer                    = null;
	private Collection<ResultConsumer> nextResultConsumers        = new LinkedList<>();
	private ScrapConsumer firstScrapConsumer                      = null;
	private Collection<ScrapConsumer> nextScrapConsumers          = new LinkedList<>();
	private Consumer timeoutAction                                = null;
	private LabeledValueConsumer defaultCartConsumer              = null;
	private BiPredicate readinessEvaluatorBiP                     = null;
	private Predicate readinessEvaluatorP                         = null;
	private BuilderSupplier builderSupplier                       = null;
	private Collection<Consumer> addCartBeforePlacementValidator  = new LinkedList<>();
	private Collection<Consumer> addBeforeKeyEvictionAction       = new LinkedList<>();
	private Collection<BiConsumer> addBeforeKeyReschedulingAction = new LinkedList<>();
	private Object[] acceptLabels                                 = null;
	private Boolean enablePostponeExpiration                      = null;
	private Boolean enablePostponeExpirationOnTimeout             = null;
	private Boolean autoAcknowledge                               = null;
	private Consumer acknowledgeAction                            = null;
	private Status[] autoAcknowledgeOnStatus                      = null;
	private Function cartPayloadAccessor                          = null;
//TODO:
//forwardResultTo: (Conveyor<K2,L2,OUT2> destination, [Function<ProductBin<K,OUT>,K2>keyConverter,] L2 label) 
	private Trio forward;
	private int parallelFactor = 1;

/*
* conveyor.idleHeartBeat = 1.5 SECONDS
* conveyor.test2.defaultBuilderTimeout = 1 SECONDS
* conveyor.test2.rejectUnexpireableCartsOlderThan = 10000
* conveyor.test2.staticPart = \
	label = com.aegisql.conveyor.config.harness.NameLabel.FIRST;\
	value1 = "preffix-";
* conveyor.test2.staticPart = \
	label = com.aegisql.conveyor.config.harness.NameLabel.LAST;\
	value1 = "-suffix";
* conveyor.test2.firstResultConsumer = new com.aegisql.conveyor.consumers.result.LogResult()
* conveyor.test2.nextResultConsumer = com.aegisql.conveyor.config.ConfigUtilsTest.rCounter
* conveyor.test2.firstScrapConsumer = new com.aegisql.conveyor.consumers.scrap.LogScrap()
* conveyor.test2.nextScrapConsumer = com.aegisql.conveyor.config.ConfigUtilsTest.sCounter
* conveyor.test2.onTimeoutAction = com.aegisql.conveyor.config.ConfigUtilsTest.timeoutAction
* conveyor.test2.defaultCartConsumer = com.aegisql.conveyor.config.ConfigUtilsTest.lvc
* conveyor.test2.readinessEvaluator = com.aegisql.conveyor.config.ConfigUtilsTest.biPredRE; //BiPredicate
* conveyor.test2.readinessEvaluator = com.aegisql.conveyor.config.ConfigUtilsTest.predRE; //Predicate
* conveyor.test2.builderSupplier = com.aegisql.conveyor.config.harness.StringSupplier("test2"); //JavaScript
* conveyor.test2.addCartBeforePlacementValidator = com.aegisql.conveyor.config.ConfigUtilsTest.cartValidator1
* conveyor.test2.addCartBeforePlacementValidator = com.aegisql.conveyor.config.ConfigUtilsTest.cartValidator2
* conveyor.test2.addBeforeKeyEvictionAction = com.aegisql.conveyor.config.ConfigUtilsTest.beforeEviction
* conveyor.test2.addBeforeKeyReschedulingAction = com.aegisql.conveyor.config.ConfigUtilsTest.beforeReschedule
* conveyor.test2.acceptLabels = "A","B","C"
* conveyor.test2.acceptLabels = com.aegisql.conveyor.config.harness.NameLabel.FIRST,com.aegisql.conveyor.config.harness.NameLabel.LAST
#forwardResultTo
* conveyor.test2.enablePostponeExpiration = false
* conveyor.enablePostponeExpirationOnTimeout = false
* conveyor.test2.expirationPostponeTime = 1000
* conveyor.test2.autoAcknowledge = true
* conveyor.test2.acknowledgeAction = com.aegisql.conveyor.config.ConfigUtilsTest.acknowledgeAction
* conveyor.test2.autoAcknowledgeOnStatus = READY,TIMED_OUT,CANCELED
* conveyor.test2.cartPayloadAccessor = com.aegisql.conveyor.config.ConfigUtilsTest.payloadFunction
*/

	private final static Logger LOG = LoggerFactory.getLogger(ConveyorBuilder.class);
		
	private <T> void setIfNotNull(T value,Consumer<T> consumer) {
		if(value != null) {
			consumer.accept(value);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Conveyor get() {
		try {
		LOG.debug("{}",this);
		Conveyor instance = null;
		
		if(parallelFactor > 1) {
			instance = new KBalancedParallelConveyor(constructor, parallelFactor);
		} else {
			instance = constructor.get();
		}
		final Conveyor c = instance;
		
		setIfNotNull(idleHeartBeat, c::setIdleHeartBeat);
		setIfNotNull(defaultBuilderTimeout, c::setDefaultBuilderTimeout);
		setIfNotNull(rejectUnexpireableCartsOlderThan, c::rejectUnexpireableCartsOlderThan);
		setIfNotNull(expirationPostponeTime, c::setExpirationPostponeTime);
		setIfNotNull(firstResultConsumer, rc -> c.resultConsumer(rc).set() );
		setIfNotNull(firstScrapConsumer, rc -> c.scrapConsumer(rc).set() );
		setIfNotNull(timeoutAction, c::setOnTimeoutAction);
		setIfNotNull(defaultCartConsumer, c::setDefaultCartConsumer);
		setIfNotNull(readinessEvaluatorP, c::setReadinessEvaluator);
		setIfNotNull(readinessEvaluatorBiP, c::setReadinessEvaluator);
		setIfNotNull(builderSupplier, c::setBuilderSupplier);
		setIfNotNull(acceptLabels, c::acceptLabels);
		setIfNotNull(enablePostponeExpiration, c::enablePostponeExpiration);
		setIfNotNull(enablePostponeExpirationOnTimeout, c::enablePostponeExpirationOnTimeout);
		setIfNotNull(autoAcknowledge, c::setAutoAcknowledge);
		setIfNotNull(acknowledgeAction, c::setAcknowledgeAction);
		setIfNotNull(cartPayloadAccessor, c::setCartPayloadAccessor);
		
		if(autoAcknowledgeOnStatus != null && autoAcknowledgeOnStatus.length != 0) {
			Status first  = autoAcknowledgeOnStatus[0];
			Status[] more = null;
			if(autoAcknowledgeOnStatus.length > 1) {
				more = new Status[autoAcknowledgeOnStatus.length-1];
				for(int i = 1; i < autoAcknowledgeOnStatus.length; i++) {
					more[i-1] = autoAcknowledgeOnStatus[i];
				}
			}
			c.autoAcknowledgeOnStatus(first, more);
		}		
		nextResultConsumers.forEach(rc -> c.resultConsumer().andThen(rc).set() );
		nextScrapConsumers.forEach(rc -> c.scrapConsumer().andThen(rc).set() );
		staticParts.forEach(pair -> c.staticPart().label(pair.label).value(pair.value).place() );
		addCartBeforePlacementValidator.forEach(pv -> c.addCartBeforePlacementValidator(pv) );
		addBeforeKeyEvictionAction.forEach(pv -> c.addBeforeKeyEvictionAction(pv) );
		addBeforeKeyReschedulingAction.forEach(ra -> c.addBeforeKeyReschedulingAction(ra) );
		if(forward != null) {
			ForwardResult fr = ForwardResult.from(c).to((String)forward.value1).label(forward.label);
			if(forward.value2 != null) {
				fr = fr.transformKey((Function) forward.value2);
			}
			fr.bind();
		}
		
		return c;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static void idleHeartBeat(ConveyorBuilder b, String s) {
		LOG.debug("Applying idleHeartBeat={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.idleHeartBeat = Duration.ofMillis(value.longValue());
	}
	
	public static void defaultBuilderTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultBuilderTimeout={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.defaultBuilderTimeout = Duration.ofMillis(value.longValue());
	}
	
	public static void rejectUnexpireableCartsOlderThan(ConveyorBuilder b, String s) {
		LOG.debug("Applying rejectUnexpireableCartsOlderThan={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.rejectUnexpireableCartsOlderThan = Duration.ofMillis(value.longValue());
	}
	
	public static void expirationPostponeTime(ConveyorBuilder b, String s) {
		LOG.debug("Applying expirationPostponeTime={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.expirationPostponeTime = Duration.ofMillis(value.longValue());
	}
	
	public static void staticPart(ConveyorBuilder b, String s) {
		LOG.debug("Applying staticPart={}",s);
		Pair value = (Pair) ConfigUtils.stringToLabelValuePairSupplier.apply(s);
		b.staticParts.add(value);
	}

	public static void firstResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstResultConsumer={}",s);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
		b.firstResultConsumer = value;
	}

	public static void nextResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextResultConsumer={}",s);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
		b.nextResultConsumers.add(value);
	}

	public static void firstScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstScrapConsumer={}",s);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
		b.firstScrapConsumer = value;
	}

	public static void nextScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextScrapConsumer={}",s);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
		b.nextScrapConsumers.add(value);
	}

	public static void timeoutAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying timeoutAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.timeoutAction = value;
	}

	public static void acknowledgeAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying acknowledgeAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.acknowledgeAction = value;
	}

	public static void addCartBeforePlacementValidator(ConveyorBuilder b, String s) {
		LOG.debug("Applying addCartBeforePlacementValidator={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addCartBeforePlacementValidator.add(value);
	}
	
	public static void addBeforeKeyEvictionAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyEvictionAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addBeforeKeyEvictionAction.add(value);
	}

	public static void defaultCartConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultCartConsumer={}",s);
		LabeledValueConsumer value = (LabeledValueConsumer) ConfigUtils.stringToLabeledValueConsumerSupplier.apply(s);
		b.defaultCartConsumer = value;
	}
	
	public static void readinessEvaluator(ConveyorBuilder b, String s) {
		LOG.debug("Applying readinessEvaluator={}",s);
		Object obj = ConfigUtils.stringToReadinessEvaluatorSupplier.apply(s);
		if(obj instanceof BiPredicate) {
			BiPredicate re = (BiPredicate) obj;
			b.readinessEvaluatorBiP = re;
			b.readinessEvaluatorP   = null;
		} else if(obj instanceof Predicate) {
			Predicate re = (Predicate) obj;
			b.readinessEvaluatorBiP = null;
			b.readinessEvaluatorP   = re;
		} else {
			throw new ConveyorConfigurationException("Unexpected readinessEvaluator type "+obj.getClass());
		}
	}

	public static void builderSupplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying builderSupplier={}",s);
		BuilderSupplier value = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply(s);
		b.builderSupplier = value;
	}

	public static void addBeforeKeyReschedulingAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyReschedulingAction={}",s);
		BiConsumer value = (BiConsumer) ConfigUtils.stringToBiConsumerSupplier.apply(s);
		b.addBeforeKeyReschedulingAction.add(value);
	}
	
	public static void acceptLabels(ConveyorBuilder b, String s) {
		LOG.debug("Applying acceptLabels={}",s);
		Object[] value = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(s);
		b.acceptLabels = value;
	}

	public static void enablePostponeExpiration(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpiration={}",s);
		Boolean value = Boolean.valueOf(s);
		b.enablePostponeExpiration = value;
	}

	public static void enablePostponeExpirationOnTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpirationOnTimeout={}",s);
		Boolean value = Boolean.valueOf(s);
		b.enablePostponeExpirationOnTimeout = value;
	}

	public static void autoAcknowledge(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledge={}",s);
		Boolean value = Boolean.valueOf(s);
		b.autoAcknowledge = value;
	}
	
	public static void autoAcknowledgeOnStatus(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledgeOnStatus={}",s);
		Status[] value = (Status[]) ConfigUtils.stringToStatusConverter.apply(s);
		b.autoAcknowledgeOnStatus = value;
	}

	public static void cartPayloadAccessor(ConveyorBuilder b, String s) {
		LOG.debug("Applying cartPayloadAccessor={}",s);
		Function value = (Function) ConfigUtils.stringToCartPayloadFunctionSupplier.apply(s);
		b.cartPayloadAccessor = value;
	}

	public static void forward(ConveyorBuilder b, String s) {
		LOG.debug("Applying forward={}",s);
		Trio value = (Trio) ConfigUtils.stringToForwardTrioSupplier.apply(s);
		b.forward = value;
	}
	
	public static void supplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor supplier={}",s);
		Supplier<Conveyor> value = ConfigUtils.stringToConveyorSupplier.apply(s);
		b.constructor = value;
	}
	
	//Readiness management
	public static void allFilesReadSuccessfully(ConveyorBuilder b, Boolean readOk) {
		LOG.debug("Applying allFilesReadSuccessfully={}",readOk);
		if(readOk) {
			b.allFilesRead = readOk;
		} else {
			throw new ConveyorConfigurationException("Conveyor initialization terminated because of file reading issue");
		}
	}

	public static void dependency(ConveyorBuilder b, String s) {
		LOG.debug("Applying dependency={}",s);
		String[] parts = s.split(",");
		for(String p:parts) {
			String clean = p.trim();
			if( ! "".equals(clean) ) {
				b.dependencies.add(clean);
			}
		}
	}

	public static void completed(ConveyorBuilder b, String s) {
		LOG.debug("Applying completed={}",s);
		if( b.dependencies.remove(s) ) {
			b.completed.add(s);
		}
	}
	
	public static void parallel(ConveyorBuilder b, String s) {
		LOG.debug("Applying parallel={}",s);
		try {
			Integer pf = Integer.parseInt(s.split("\\s+")[0]);
			b.parallelFactor = pf;
		} catch (Exception e) {
			LOG.warn("Unimplemented for {}",s,e);
			// TODO: non-numeral parallel
		}
	}
	
	@Override
	public String toString() {
		return "ConveyorBuilder [" + (idleHeartBeat != null ? "idleHeartBeat=" + idleHeartBeat + ", " : "")
				+ (defaultBuilderTimeout != null ? "defaultBuilderTimeout=" + defaultBuilderTimeout + ", " : "")
				+ (rejectUnexpireableCartsOlderThan != null
						? "rejectUnexpireableCartsOlderThan=" + rejectUnexpireableCartsOlderThan + ", "
						: "")
				+ (expirationPostponeTime != null ? "expirationPostponeTime=" + expirationPostponeTime + ", " : "")
				+ (staticParts != null ? "staticParts=" + staticParts + ", " : "")
				+ (firstResultConsumer != null ? "firstResultConsumer=" + firstResultConsumer + ", " : "")
				+ (nextResultConsumers != null ? "nextResultConsumers=" + nextResultConsumers + ", " : "")
				+ (firstScrapConsumer != null ? "firstScrapConsumer=" + firstScrapConsumer + ", " : "")
				+ (nextScrapConsumers != null ? "nextScrapConsumers=" + nextScrapConsumers + ", " : "")
				+ (timeoutAction != null ? "timeoutAction=" + timeoutAction + ", " : "")
				+ (defaultCartConsumer != null ? "defaultCartConsumer=" + defaultCartConsumer + ", " : "")
				+ (readinessEvaluatorBiP != null ? "readinessEvaluatorBiP=" + readinessEvaluatorBiP + ", " : "")
				+ (readinessEvaluatorP != null ? "readinessEvaluatorP=" + readinessEvaluatorP + ", " : "")
				+ (builderSupplier != null ? "builderSupplier=" + builderSupplier + ", " : "")
				+ (addCartBeforePlacementValidator != null
						? "addCartBeforePlacementValidator=" + addCartBeforePlacementValidator + ", "
						: "")
				+ (addBeforeKeyEvictionAction != null
						? "addBeforeKeyEvictionAction=" + addBeforeKeyEvictionAction + ", "
						: "")
				+ (addBeforeKeyReschedulingAction != null
						? "addBeforeKeyReschedulingAction=" + addBeforeKeyReschedulingAction + ", "
						: "")
				+ (acceptLabels != null ? "acceptLabels=" + Arrays.toString(acceptLabels) + ", " : "")
				+ (enablePostponeExpiration != null ? "enablePostponeExpiration=" + enablePostponeExpiration + ", "
						: "")
				+ (enablePostponeExpirationOnTimeout != null
						? "enablePostponeExpirationOnTimeout=" + enablePostponeExpirationOnTimeout + ", "
						: "")
				+ (autoAcknowledge != null ? "autoAcknowledge=" + autoAcknowledge + ", " : "")
				+ (acknowledgeAction != null ? "acknowledgeAction=" + acknowledgeAction + ", " : "")
				+ (autoAcknowledgeOnStatus != null
						? "autoAcknowledgeOnStatus=" + Arrays.toString(autoAcknowledgeOnStatus) + ", "
						: "")
				+ (cartPayloadAccessor != null ? "cartPayloadAccessor=" + cartPayloadAccessor : "") + "]";
	}

	@Override
	public boolean test() {
		return allFilesRead && dependencies.size() == 0;
	}
	
	
	
}
