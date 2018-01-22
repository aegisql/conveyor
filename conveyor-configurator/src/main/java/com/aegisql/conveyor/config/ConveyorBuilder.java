package com.aegisql.conveyor.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;

@SuppressWarnings("rawtypes")
public class ConveyorBuilder implements Supplier<Conveyor>, Testing {

	//readiness
	private boolean allFilesRead = false;
	private Set<String> lParallel    = new LinkedHashSet<>();
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
	private Collection<Trio> forward                              = new LinkedList<>();
	private int parallelFactor                                    = 1;
	private String persistence                                    = null;
	
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
			LOG.info("Instantiate K-Balanced conveyor with parallelizm={}",parallelFactor);
		} else if(lParallel.size() > 1){
			LOG.info("Instantiate L-Balanced conveyor with parallelizm={}",lParallel);
			String[] lConveyors = new String[lParallel.size()];
			lConveyors = lParallel.toArray(lConveyors);
			instance = new LBalancedParallelConveyor<>(lConveyors);
		} else {
			instance = constructor.get();
			LOG.info("Instantiate {}",instance.getClass().getName());
		}
		
		if(persistence != null) {
			Persistence p = Persistence.byName(persistence);
			instance = new PersistentConveyor(p,instance);
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
		forward.forEach(f->{
			ForwardResult fr = ForwardResult.from(c).to((String)f.value1).label(f.label);
			if(f.value2 != null) {
				fr = fr.transformKey((Function) f.value2);
			}
			fr.bind();			
		});

		
		return c;
		} catch (Exception e) {
			LOG.error("Error constructing Conveyor",e);
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
		b.forward.add(value);
	}
	
	public static void supplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor supplier={}",s);
		Supplier<Conveyor> value = ConfigUtils.stringToConveyorSupplier.apply(s);
		b.constructor = value;
	}

	public static void persitence(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor persitence={}",s);
		b.persistence = s;
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
			b.lParallel.clear();
		} catch (Exception e) {
			String[] parts = s.split(",");
			if(parts.length > 0) {
				b.parallelFactor = 1;
				for(String part:parts) {
					String trimmed = part.trim();
					b.dependencies.add(trimmed);
					b.lParallel.add(trimmed);
				}
			} else {
				throw e;
			}
		}
	}
	
	@Override
	public String toString() {
		return "ConveyorBuilder [" + (dependencies != null ? "dependencies=" + dependencies + ", " : "")
				+ "allFilesRead=" + allFilesRead + ", " + (lParallel != null ? "lParallel=" + lParallel + ", " : "")
				+ (completed != null ? "completed=" + completed + ", " : "")
				+ (constructor != null ? "constructor=" + constructor + ", " : "")
				+ (idleHeartBeat != null ? "idleHeartBeat=" + idleHeartBeat + ", " : "")
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
				+ (cartPayloadAccessor != null ? "cartPayloadAccessor=" + cartPayloadAccessor + ", " : "")
				+ (forward != null ? "forward=" + forward + ", " : "") + "parallelFactor=" + parallelFactor + ", "
				+ (persistence != null ? "persistence=" + persistence : "") + "]";
	}

	@Override
	public boolean test() {
		return allFilesRead && dependencies.size() == 0;
	}
	
	
	
}
