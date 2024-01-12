package com.aegisql.conveyor.config;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.meta.ConveyorMetaInfoBuilder;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration.BinaryLogConfigurationBuilder;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.builders.RestoreOrder;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import org.graalvm.shadowed.org.jcodings.util.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.*;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorBuilder.
 */
@SuppressWarnings("rawtypes")
public class ConveyorBuilder implements Supplier<Conveyor>, Testing {

	/** The all files read. */
	// readiness
	private boolean allFilesRead = false;

	/** The l parallel. */
	private final Set<String> lParallel = new LinkedHashSet<>();

	/** The dependencies. */
	private final Set<String> dependencies = new HashSet<>();

	/** The completed. */
	private final Set<String> completed = new HashSet<>();

	/** The Constant serialVersionUID. */
	// setters
	@Serial
	private static final long serialVersionUID = 1L;

	/** The constructor. */
	private Function<ConveyorMetaInfo,Conveyor> constructor = mi->{
		if(mi==null) {
			return new AssemblingConveyor();
		} else {
			return new AssemblingConveyorMI(mi);
		}
	};

	/** The idle heart beat. */
	private Duration idleHeartBeat = null;

	/** The default builder timeout. */
	private Duration defaultBuilderTimeout = null;

	/** The reject unexpireable carts older than. */
	private Duration rejectUnexpireableCartsOlderThan = null;

	/** The expiration postpone time. */
	private Duration expirationPostponeTime = null;

	/** The static parts. */
	private final Collection<Pair> staticParts = new LinkedList<>();

	/** The first result consumer. */
	private ResultConsumer firstResultConsumer = null;

	/** The next result consumers. */
	private final Collection<ResultConsumer> nextResultConsumers = new LinkedList<>();

	/** The first scrap consumer. */
	private ScrapConsumer firstScrapConsumer = null;

	/** The next scrap consumers. */
	private final Collection<ScrapConsumer> nextScrapConsumers = new LinkedList<>();

	/** The timeout action. */
	private Consumer timeoutAction = null;

	/** The default cart consumer. */
	private LabeledValueConsumer defaultCartConsumer = null;

	/** The readiness evaluator bi P. */
	private BiPredicate readinessEvaluatorBiP = null;

	/** The readiness evaluator P. */
	private Predicate readinessEvaluatorP = null;

	/** The builder supplier. */
	private BuilderSupplier builderSupplier = null;

	/** The add cart before placement validator. */
	private final Collection<Consumer> addCartBeforePlacementValidator = new LinkedList<>();

	/** The add before key eviction action. */
	private final Collection<Consumer> addBeforeKeyEvictionAction = new LinkedList<>();

	/** The add before key rescheduling action. */
	private final Collection<BiConsumer> addBeforeKeyReschedulingAction = new LinkedList<>();

	/** The accepted labels. */
	private final Set acceptedLabels = new HashSet<>();

	/** The readiness tester. */
	private ReadinessTester readinessTester = null;

	/** The ready labels. */
	private final Map<Object, Integer> readyLabels = new HashMap<>();

	/** The enable postpone expiration. */
	private Boolean enablePostponeExpiration = null;

	/** The enable postpone expiration on timeout. */
	private Boolean enablePostponeExpirationOnTimeout = null;

	/** The auto acknowledge. */
	private Boolean autoAcknowledge = null;

	/** The acknowledge action. */
	private Consumer acknowledgeAction = null;

	/** The auto acknowledge on status. */
	private Status[] autoAcknowledgeOnStatus = null;

	/** The cart payload accessor. */
	private Function cartPayloadAccessor = null;

	/** The forward. */
	private final Collection<Trio> forward = new LinkedList<>();

	/** The parallel factor. */
	private int parallelFactor = 1;
	
	/** The max queue size. */
	private int maxQueueSize = 0;

	/** The persistence. */
	private String persistence = null;

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ConveyorBuilder.class);

	/** The persistence properties. */
	private final Map<String, PersistenceProperties> persistenceProperties = new TreeMap<>((o1, o2) -> {
		String l1 = o1.toLowerCase();
		String l2 = o2.toLowerCase();
		return -l1.compareTo(l2);
	});

	/** The default properties. */
	private final Map<String, PersistenceProperties> defaultPersistenceProperties = new TreeMap<>();

	private Boolean enablePriorityQueue = false;
	private Class keyType;
	private Class labelType;
	private Class productType;
	private List labels = new ArrayList();
	private Map<Object,List<Class>> supportedValueTypes = new HashMap<>();
	private ConveyorMetaInfoBuilder metaInfoBuilder = new ConveyorMetaInfoBuilder();

	/**
	 * Sets the if not null.
	 *
	 * @param <T>
	 *            the generic type
	 * @param value
	 *            the value
	 * @param consumer
	 *            the consumer
	 */
	private <T> void setIfNotNull(T value, Consumer<T> consumer) {
		if (value != null) {
			consumer.accept(value);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.function.Supplier#get()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Conveyor get() {
		try {
			LOG.debug("{}", this);
			Conveyor instance;

			for (String key : persistenceProperties.keySet()) {
				PersistenceProperties pp = persistenceProperties.get(key);

				PersistenceProperties pp0 = defaultPersistenceProperties.get(pp.getLevel0Key());
				PersistenceProperties pp1 = defaultPersistenceProperties.get(pp.getLevel1Key());
				PersistenceProperties pp2 = defaultPersistenceProperties.get(pp.getLevel2Key());

				pp.merge(pp2);
				pp.merge(pp1);
				pp.merge(pp0);


				persistence = pp.buildPersistence();
				LOG.debug("Created Persistence {}",persistence);
				
			}

			metaInfoBuilder = metaInfoBuilder.keyType(keyType);
			metaInfoBuilder = metaInfoBuilder.labelType(labelType);
			metaInfoBuilder = metaInfoBuilder.productType(productType);
			metaInfoBuilder = metaInfoBuilder.addLabels(supportedValueTypes.keySet());
			for(var es:supportedValueTypes.entrySet()) {
				metaInfoBuilder = metaInfoBuilder.supportedTypes(es.getKey(),es.getValue());
			}

			ConveyorMetaInfo metaInfo = null;
			try {
				metaInfo = metaInfoBuilder.get();
				LOG.info("MetaInfo configuration found: {}",metaInfo);
			} catch (Exception e) {
				LOG.warn("MetaInfo configuration not found or incomplete. Ignoring");
			}

			if (parallelFactor > 1) {
				ConveyorMetaInfo finalMetaInfo = metaInfo;
				instance = new KBalancedParallelConveyor(()->constructor.apply(finalMetaInfo), parallelFactor);
				LOG.info("Instantiate K-Balanced conveyor with parallelizm={}", parallelFactor);
			} else if (lParallel.size() > 1) {
				LOG.info("Instantiate L-Balanced conveyor with parallelizm={}", lParallel);
				String[] lConveyors = new String[lParallel.size()];
				lConveyors = lParallel.toArray(lConveyors);
				instance = new LBalancedParallelConveyor<>(lConveyors);
			} else {
				instance = constructor.apply(metaInfo);
				LOG.info("Instantiate {}", instance.getClass().getName());
			}

			if (persistence != null) {
				Persistence p = Persistence.byName(persistence);
				instance = new PersistentConveyor(p.copy(), instance);
			}

			if(metaInfo != null) {
				//instance = new ConveyorMetaInfoWrapper(instance,metaInfo);
			}

			final Conveyor c = instance;

			setIfNotNull(builderSupplier, c::setBuilderSupplier);
			setIfNotNull(idleHeartBeat, c::setIdleHeartBeat);
			setIfNotNull(defaultBuilderTimeout, c::setDefaultBuilderTimeout);
			setIfNotNull(rejectUnexpireableCartsOlderThan, c::rejectUnexpireableCartsOlderThan);
			setIfNotNull(expirationPostponeTime, c::setExpirationPostponeTime);
			setIfNotNull(firstResultConsumer, rc -> c.resultConsumer(rc).set());
			setIfNotNull(firstScrapConsumer, rc -> c.scrapConsumer(rc).set());
			setIfNotNull(timeoutAction, c::setOnTimeoutAction);
			setIfNotNull(defaultCartConsumer, c::setDefaultCartConsumer);
			setIfNotNull(enablePostponeExpiration, c::enablePostponeExpiration);
			setIfNotNull(enablePostponeExpirationOnTimeout, c::enablePostponeExpirationOnTimeout);
			setIfNotNull(autoAcknowledge, c::setAutoAcknowledge);
			setIfNotNull(acknowledgeAction, c::setAcknowledgeAction);
			setIfNotNull(cartPayloadAccessor, c::setCartPayloadAccessor);

			if (readinessTester != null) {
				for (Object label : readyLabels.keySet()) {
					if (label == null) {
						LOG.debug("Ready when accept count {}", readyLabels.get(null));
						readinessTester = readinessTester.accepted(readyLabels.get(null));
					} else {
						LOG.debug("Ready when accept {} times {}", label, readyLabels.get(label));
						readinessTester = readinessTester.accepted(label, readyLabels.get(label));
					}
				}
				if (readinessEvaluatorP != null) {
					readinessTester = readinessTester.andThen(readinessEvaluatorP);
				}
				if (readinessEvaluatorBiP != null) {
					readinessTester = readinessTester.andThen(readinessEvaluatorBiP);
				}
				c.setReadinessEvaluator(readinessTester);
			} else {
				setIfNotNull(readinessEvaluatorP, c::setReadinessEvaluator);
				setIfNotNull(readinessEvaluatorBiP, c::setReadinessEvaluator);
			}

			if (autoAcknowledgeOnStatus != null && autoAcknowledgeOnStatus.length != 0) {
				Status first = autoAcknowledgeOnStatus[0];
				Status[] more = null;
				if (autoAcknowledgeOnStatus.length > 1) {
					more = new Status[autoAcknowledgeOnStatus.length - 1];
					System.arraycopy(autoAcknowledgeOnStatus, 1, more, 0, autoAcknowledgeOnStatus.length - 1);
				}
				c.autoAcknowledgeOnStatus(first, more);
			}
			nextResultConsumers.forEach(rc -> c.resultConsumer().andThen(rc).set());
			nextScrapConsumers.forEach(rc -> c.scrapConsumer().andThen(rc).set());
			staticParts.forEach(pair -> c.staticPart().label(pair.label).value(pair.value).place());
			addCartBeforePlacementValidator.forEach(c::addCartBeforePlacementValidator);
			addBeforeKeyEvictionAction.forEach(c::addBeforeKeyEvictionAction);
			addBeforeKeyReschedulingAction.forEach(c::addBeforeKeyReschedulingAction);
			forward.forEach(f -> {
				ForwardResult fr = ForwardResult.from(c).to((String) f.value1).label(f.label);
				if (f.value2 != null) {
					fr = fr.transformKey((Function) f.value2);
				}
				fr.bind();
			});
			if (acceptedLabels.size() > 0) {
				Object[] acceptLabels = new Object[acceptedLabels.size()];
				acceptLabels = acceptedLabels.toArray(acceptLabels);
				c.acceptLabels(acceptLabels);
			}

			return c;
		} catch (Exception e) {
			LOG.error("Error constructing Conveyor", e);
			throw new ConveyorConfigurationException(e);
		}
	}

	/**
	 * Idle heart beat.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void idleHeartBeat(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.idleHeartBeat = cp.getValueAsDuration();
	}

	/**
	 * Default builder timeout.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void defaultBuilderTimeout(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.defaultBuilderTimeout = cp.getValueAsDuration();
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param b
	 *            the b
	 * @param cp  the cp
	 */
	public static void rejectUnexpireableCartsOlderThan(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.rejectUnexpireableCartsOlderThan = cp.getValueAsDuration();
	}

	/**
	 * Expiration postpone time.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void expirationPostponeTime(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.expirationPostponeTime = cp.getValueAsDuration();
	}

	/**
	 * Static part.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void staticPart(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Pair value = (Pair) ConfigUtils.stringToLabelValuePairSupplier.apply(cp.getValueAsString());
		b.staticParts.add(value);
	}

	/**
	 * First result consumer.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void firstResultConsumer(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.firstResultConsumer = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Next result consumer.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void nextResultConsumer(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(cp.getValueAsString());
		b.nextResultConsumers.add(value);
	}

	/**
	 * First scrap consumer.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void firstScrapConsumer(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.firstScrapConsumer = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Next scrap consumer.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void nextScrapConsumer(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(cp.getValueAsString());
		b.nextScrapConsumers.add(value);
	}

	/**
	 * Timeout action.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void timeoutAction(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.timeoutAction = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Acknowledge action.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void acknowledgeAction(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.acknowledgeAction = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Adds the cart before placement validator.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void addCartBeforePlacementValidator(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(cp.getValueAsString());
		b.addCartBeforePlacementValidator.add(value);
	}

	/**
	 * Adds the before key eviction action.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void addBeforeKeyEvictionAction(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(cp.getValueAsString());
		b.addBeforeKeyEvictionAction.add(value);
	}

	/**
	 * Default cart consumer.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void defaultCartConsumer(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.defaultCartConsumer = (LabeledValueConsumer) ConfigUtils.stringToLabeledValueConsumerSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Readiness evaluator.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void readinessEvaluator(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Object obj = ConfigUtils.stringToReadinessEvaluatorSupplier.apply(cp.getValueAsString());
		if (obj instanceof BiPredicate) {
			b.readinessEvaluatorBiP = (BiPredicate) obj;
			b.readinessEvaluatorP = null;
		} else if (obj instanceof Predicate re) {
			b.readinessEvaluatorBiP = null;
			b.readinessEvaluatorP = re;
		} else {
			throw new ConveyorConfigurationException("Unexpected readinessEvaluator type " + obj.getClass());
		}
	}

	/**
	 * Ready when.
	 *
	 * @param b the b
	 * @param cp the s
	 */
	public static void readyWhen(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		String[] parts = cp.getValueAsString().trim().split("\\s+");
		if (parts.length == 0) {
			return;
		}
		if (b.readinessTester == null) {
			b.readinessTester = new ReadinessTester<>();
		}
		int count = 1;
		try {
			count = Integer.parseInt(parts[0]);
		} catch (Exception e) {
			Object[] labels = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(parts[0]);
			for (Object label : labels) {
				b.readyLabels.put(label, count);
			}
			return;
		}

		if (parts.length == 1) {
			b.readyLabels.put(null, count);
		} else {
			Object[] labels = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(parts[1]);
			for (Object label : labels) {
				b.readyLabels.put(label, count);
			}
		}
	}

	/**
	 * Builder supplier.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void builderSupplier(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.builderSupplier = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Adds the before key rescheduling action.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void addBeforeKeyReschedulingAction(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		BiConsumer value = (BiConsumer) ConfigUtils.stringToBiConsumerSupplier.apply(cp.getValueAsString());
		b.addBeforeKeyReschedulingAction.add(value);
	}

	/**
	 * Accept labels.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void acceptLabels(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Object[] value = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(cp.getValueAsString());
		b.acceptedLabels.addAll(Arrays.asList(value));
	}

	/**
	 * Enable postpone expiration.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void enablePostponeExpiration(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.enablePostponeExpiration = cp.getValueAsBoolean();
	}

	/**
	 * Enable postpone expiration on timeout.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void enablePostponeExpirationOnTimeout(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.enablePostponeExpirationOnTimeout = cp.getValueAsBoolean();
	}

	/**
	 * Auto acknowledge.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void autoAcknowledge(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.autoAcknowledge = cp.getValueAsBoolean();
	}

	/**
	 * Auto acknowledge on status.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void autoAcknowledgeOnStatus(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.autoAcknowledgeOnStatus = (Status[]) ConfigUtils.stringToStatusConverter.apply(cp.getValueAsString());
	}

	/**
	 * Cart payload accessor.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void cartPayloadAccessor(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.cartPayloadAccessor = (Function) ConfigUtils.stringToFunctionSupplier.apply(cp.getValueAsString());
	}

	/**
	 * Forward.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void forward(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		Trio value = (Trio) ConfigUtils.stringToForwardTrioSupplier.apply(cp.getValueAsString());
		b.forward.add(value);
	}

	/**
	 * Supplier.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void supplier(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		//If conveyor specified by external source it must provide its own metainfo, when needed
		b.constructor = mi->ConfigUtils.stringToConveyorSupplier.apply(cp.getValueAsString()).get();
		b.maxQueueSize = 0;
		b.enablePriorityQueue = false;
	}

	/**
	 * Persitence.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void persitence(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.persistence = cp.getValueAsString();
	}

	/**
	 * All files read successfully.
	 *
	 * @param b
	 *            the b
	 * @param readOk
	 *            the read ok
	 */
	// Readiness management
	public static void allFilesReadSuccessfully(ConveyorBuilder b, Boolean readOk) {
		LOG.debug("Applying allFilesReadSuccessfully={}", readOk);
		if (readOk) {
			b.allFilesRead = true;
		} else {
			throw new ConveyorConfigurationException(
					"Conveyor initialization terminated because of file reading issue");
		}
	}

	/**
	 * Dependency.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void dependency(ConveyorBuilder b, String s) {
		LOG.debug("Setting dependency {}",s);
		String[] parts = s.split(",");
		for (String p : parts) {
			String clean = p.trim();
			if (!"".equals(clean)) {
				b.dependencies.add(clean);
			}
		}
	}

	/**
	 * Completed.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void completed(ConveyorBuilder b, String s) {
		LOG.debug("Completed dependency {}",s);
		if (b.dependencies.remove(s)) {
			b.completed.add(s);
		}
	}

	/**
	 * Parallel.
	 *
	 * @param b
	 *            the b
	 * @param cp
	 *            the s
	 */
	public static void parallel(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		String s = cp.getValueAsString();
		try {
			b.parallelFactor = Integer.parseInt(s.split("\\s+")[0]);
			b.lParallel.clear();
		} catch (Exception e) {
			String[] parts = s.split(",");
			if (parts.length > 0) {
				b.parallelFactor = 1;
				for (String part : parts) {
					String trimmed = part.trim();
					b.dependencies.add(trimmed);
					b.lParallel.add(trimmed);
				}
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * Max queue size.
	 *
	 * @param b the b
	 * @param cp the s
	 */
	public static void maxQueueSize(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		int maxSize = Integer.parseInt(cp.getValueAsString().split("\\s+")[0]);
		b.maxQueueSize = maxSize;
		if(b.maxQueueSize > 0) {
			b.enablePriorityQueue = false;
			b.constructor = mi-> {
				if(mi==null) {
					return new AssemblingConveyor(() -> new ArrayBlockingQueue(maxSize));
				} else {
					return new AssemblingConveyorMI(() -> new ArrayBlockingQueue(maxSize),mi);
				}
			};
		}
	}

	public static void priority(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		try {
			final Supplier<PriorityBlockingQueue<Cart>> queueSupplier=Priority.valueOf(cp.getValueAsString());
			b.enablePriorityQueue = true;
			b.maxQueueSize = 0;
			b.constructor = mi-> {
				if(mi==null) {
					return new AssemblingConveyor(queueSupplier);
				} else {
					return new AssemblingConveyorMI(queueSupplier,mi);
				}
			};

		} catch (Exception e) {
			b.enablePriorityQueue = false;
			LOG.error("Failed Applying priority {}", cp, e);
		}
	}

	private static void logRegister(ConveyorProperty cp) {
		LOG.debug("Applying {}",cp);
	}

	public static void registerPath(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		ConveyorConfiguration.registerPath(cp.getValueAsString());
	}
	/**
	 * Persistence property.
	 *
	 * @param b
	 *            the b
	 * @param pp
	 *            the pp
	 */
	public static void persistenceProperty(ConveyorBuilder b, PersistenceProperty pp) {

		String key = pp.buildKey();
		LOG.debug("Adding{}persistenceProperty {}:{}={}", pp.isDefaultProperty() ? " default " : " ", key,
				pp.getProperty(), pp.getValue());
		PersistenceProperties pm;
		if (pp.isDefaultProperty()) {
			pm = b.defaultPersistenceProperties.computeIfAbsent(key,k->new PersistenceProperties(pp.getType(), pp.getSchema(), pp.getName()));
		} else {
			pm = b.persistenceProperties.computeIfAbsent(key,k->new PersistenceProperties(pp.getType(), pp.getSchema(), pp.getName()));
		}
		pm.addProperty(pp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ConveyorBuilder [" + "dependencies=" + dependencies + ", "
				+ "allFilesRead=" + allFilesRead + ", " + (lParallel != null ? "lParallel=" + lParallel + ", " : "")
				+ (completed != null ? "completed=" + completed + ", " : "")
				+ (constructor != null ? "constructor=" + constructor + ", " : "")
				+ (maxQueueSize > 0 ? "maxQueueSize=" + maxQueueSize + ", " : "")
				+ (enablePriorityQueue != null && enablePriorityQueue ? "enablePriorityQueue=" + enablePriorityQueue + ", " : "")
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
				+ (acceptedLabels != null ? "acceptLabels=" + acceptedLabels + ", " : "")
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Testing#test()
	 */
	@Override
	public boolean test() {
		return allFilesRead && dependencies.size() == 0;
	}

	public static void keyType(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.keyType = cp.getValueAsClass();
	}

	public static void labelType(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.labelType = cp.getValueAsClass();
	}

	public static void productType(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		b.productType = cp.getValueAsClass();
	}

	public static void supportedValueTypes(ConveyorBuilder b, ConveyorProperty cp) {
		logRegister(cp);
		String[] parts = cp.getValueAsString().trim().split("\\s+|\\,");
		if (parts.length == 0) {
			return;
		}
		Object label = ((Object[]) ConfigUtils.stringToLabelArraySupplier.apply(parts[0]))[0];
		List<Class> supportedTypes = b.supportedValueTypes.computeIfAbsent(label, l -> new ArrayList<>());
		for(int i = 1; i< parts.length; i++) {
			supportedTypes.add(ConveyorProperty.getValueAsClass(parts[i]));
		}
	}
}
