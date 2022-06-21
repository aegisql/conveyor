package com.aegisql.conveyor.config;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
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
	private Supplier<Conveyor> constructor = AssemblingConveyor::new;

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
	private final Map<String, PersistenceProperties> defaultProperties = new TreeMap<>();

	private Boolean enablePriorityQueue = false;

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
			Conveyor instance = null;

			for (String key : persistenceProperties.keySet()) {
				PersistenceProperties pp = persistenceProperties.get(key);
				Map<String, LinkedList<PersistenceProperty>> ppMapList = pp.getProperties();

				PersistenceProperties pp0 = defaultProperties.get(pp.getLevel0Key());
				PersistenceProperties pp1 = defaultProperties.get(pp.getLevel1Key());
				PersistenceProperties pp2 = defaultProperties.get(pp.getLevel2Key());

				if (pp2 != null) {
					pp2.getProperties().forEach((k, p) -> {
						if (!ppMapList.containsKey(k)) {
							ppMapList.put(k, p);
						}
					});
				}
				if (pp1 != null) {
					pp1.getProperties().forEach((k, p) -> {
						if (!ppMapList.containsKey(k)) {
							ppMapList.put(k, p);
						}
					});
				}
				if (pp0 != null) {
					pp0.getProperties().forEach((k, p) -> {
						if (!ppMapList.containsKey(k)) {
							ppMapList.put(k, p);
						}
					});
				}

					PersistenceProperty keyProperty = ppMapList.get("keyClass").getLast();
					LOG.debug("Persistence found {} {}", key, pp);

					if (keyProperty == null) {
						throw new ConveyorConfigurationException(
								"Missing mandatory Persistence property 'keyClass' in " + key);
					}

					Class keyClass = Class.forName(keyProperty.getValueAsString());

					JdbcPersistenceBuilder dp = JdbcPersistenceBuilder.presetInitializer(pp.getType().toLowerCase(),keyClass);
					dp = dp.autoInit(true);
					dp = dp.schema(pp.getSchema());
					dp = dp.partTable(pp.getName());

					BinaryLogConfigurationBuilder bLogConf = BinaryLogConfiguration.builder();
					bLogConf.partTableName(pp.getName());

					// optional parts
					for (LinkedList<PersistenceProperty> ppList : ppMapList.values())
						for (PersistenceProperty p : ppList) {
							switch (p.getProperty()) {
							case "keyClass": //already set
								break;
							case "database":
								dp = dp.database(p.getValueAsString());
								break;
							case "autoInit":
								dp = dp.autoInit(Boolean.parseBoolean(p.getValueAsString()));
								break;
							case "restoreOrder":
								dp = dp.restoreOrder(RestoreOrder.valueOf(p.getValueAsString()));
								break;
							case "username":
								dp = dp.user(p.getValueAsString());
								break;
							case "password":
								dp = dp.password(p.getValueAsString());
								break;
							case "completedLogTable":
								dp = dp.completedLogTable(p.getValueAsString());
								break;
							case "host":
								dp = dp.host(p.getValueAsString());
								break;
							case "port":
								dp = dp.port(Integer.parseInt(p.getValueAsString()));
								break;
							case "encryptionAlgorithm":
								dp = dp.encryptionAlgorithm(p.getValueAsString());
								break;
							case "encryptionTransformation":
								dp = dp.encryptionTransformation(p.getValueAsString());
								break;
							case "encryptionKeyLength":
								dp = dp.encryptionKeyLength(Integer.parseInt(p.getValueAsString()));
								break;
							case "encryptionSecret":
								dp = dp.encryptionSecret(p.getValueAsString());
								break;
							case "maxBatchSize":
								dp = dp.maxBatchSize(Integer.parseInt(p.getValueAsString()));
								break;
							case "doNotSaveProperties":
								String[] parts = p.getValueAsString().split(",");
								for (String part : parts) {
									dp = dp.doNotSaveCartProperties(part.trim());
								}
								break;
							case "maxBatchTime":
								Long value = (Long) ConfigUtils.timeToMillsConverter.apply(p.getValueAsString());
								dp = dp.maxBatchTime(Duration.ofMillis(value));
								break;
							case "archiveStrategy.path":
								bLogConf.path(p.getValueAsString());
								dp = dp.archiver(bLogConf.build());
								break;
							case "archiveStrategy.moveTo":
								bLogConf.moveToPath(p.getValueAsString());
								dp = dp.archiver(bLogConf.build());
								break;
							case "archiveStrategy.maxFileSize":
								bLogConf.maxFileSize(p.getValueAsString());
								dp = dp.archiver(bLogConf.build());
								break;
							case "archiveStrategy.bucketSize":
								bLogConf.bucketSize(Integer.parseInt(p.getValueAsString()));
								dp = dp.archiver(bLogConf.build());
								break;
							case "archiveStrategy.zip":
								bLogConf.zipFile(Boolean.parseBoolean(p.getValueAsString()));
								dp = dp.archiver(bLogConf.build());
								break;
							case "archiveStrategy":
								ArchiveStrategy as = ArchiveStrategy.valueOf(p.getValueAsString());
								switch (as) {
								case NO_ACTION:
									dp = dp.noArchiving();
									break;
								case DELETE:
									dp = dp.deleteArchiving();
									break;
								case SET_ARCHIVED:
									dp = dp.setArchived();
									break;
								case MOVE_TO_FILE:
									dp = dp.archiver(bLogConf.build());
									break;
									default:
									break;
								}
								break;
							case "archiveStrategy.archiver":
								Archiver ar = ConfigUtils.stringToArchiverConverter.apply(p.getValueAsString());
								dp = dp.archiver(ar);
								LOG.warn("Unimplemented PersistentProperty {}", p);
								break;
							case "archiveStrategy.persistence":
								Persistence per = Persistence.byName(p.getValueAsString());
								dp = dp.archiver(per);
								break;
							case "labelConverter":
								try {
									Class clas = Class.forName(p.getValueAsString());
									dp = dp.labelConverter(clas);
									LOG.debug("Label converter {}", clas.getName());
								} catch (Exception e) {
									ObjectConverter oc = ConfigUtils.stringToObjectConverter
											.apply(p.getValueAsString());
									dp = dp.labelConverter(oc);
									LOG.debug("Label converter {}", oc.conversionHint());
								}
								break;
							case "idSupplier":
								dp = dp.idSupplier(ConfigUtils.stringToIdSupplier.apply(p.getValueAsString()));
								LOG.warn("Unimplemented PersistentProperty {}", p);
								break;
							case "addBinaryConverter":
								String[] s = p.getValueAsString().split(",");
								ObjectConverter oc = ConfigUtils.stringToObjectConverter
								.apply(s[1].trim());
								try {
									Class clas = Class.forName(s[0].trim());
									dp = dp.addBinaryConverter(clas, oc);
								} catch (Exception e) {
									Object label = ConfigUtils.stringToRefConverter.apply(s[0]);
									dp = dp.addBinaryConverter(label, oc);
								}
								break;
							case "addField":
								String[] classFieldName = p.getValueAsString().split(",",3);
								Class fieldClass = Class.forName(classFieldName[0].trim());
								String name = classFieldName[1].trim();
								if(classFieldName.length == 2) {
									dp = dp.addField(fieldClass, name);
								} else {
									Function accessor = (Function)ConfigUtils.stringToFunctionSupplier.apply(classFieldName[2].trim());
									dp = dp.addField(fieldClass, name, accessor);
								}
								break;
							case "addUniqueFields":
								String[] fields = p.getValueAsString().split(",");
								List<String> fl = new ArrayList<>();
								if(fields != null) {
									for(String f:fields) {
										fl.add(f.trim());
									}
								}
								dp = dp.addUniqueFields(fl);
								break;
							case "minCompactSize":
								dp = dp.minCompactSize(Integer.parseInt(p.getValueAsString()));
								LOG.warn("minCompactSize PersistentProperty {}", p);
								break;
							default:
								LOG.warn("Unsupported PersistentProperty {}", p);
								break;
							}
						}
					dp.build();
					persistence = "com.aegisql.conveyor.persistence."+pp.getType().toLowerCase()+"."+pp.getSchema()+":type="+pp.getName();
					LOG.debug("Created Persistence {}",persistence);
				
			}

			if (parallelFactor > 1) {
				instance = new KBalancedParallelConveyor(constructor, parallelFactor);
				LOG.info("Instantiate K-Balanced conveyor with parallelizm={}", parallelFactor);
			} else if (lParallel.size() > 1) {
				LOG.info("Instantiate L-Balanced conveyor with parallelizm={}", lParallel);
				String[] lConveyors = new String[lParallel.size()];
				lConveyors = lParallel.toArray(lConveyors);
				instance = new LBalancedParallelConveyor<>(lConveyors);
			} else {
				instance = constructor.get();
				LOG.info("Instantiate {}", instance.getClass().getName());
			}

			if (persistence != null) {
				Persistence p = Persistence.byName(persistence);
				instance = new PersistentConveyor(p.copy(), instance);
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
	 * @param s
	 *            the s
	 */
	public static void idleHeartBeat(ConveyorBuilder b, String s) {
		LOG.debug("Applying idleHeartBeat={}", s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.idleHeartBeat = Duration.ofMillis(value);
	}

	/**
	 * Default builder timeout.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void defaultBuilderTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultBuilderTimeout={}", s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.defaultBuilderTimeout = Duration.ofMillis(value);
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void rejectUnexpireableCartsOlderThan(ConveyorBuilder b, String s) {
		LOG.debug("Applying rejectUnexpireableCartsOlderThan={}", s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.rejectUnexpireableCartsOlderThan = Duration.ofMillis(value);
	}

	/**
	 * Expiration postpone time.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void expirationPostponeTime(ConveyorBuilder b, String s) {
		LOG.debug("Applying expirationPostponeTime={}", s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.expirationPostponeTime = Duration.ofMillis(value);
	}

	/**
	 * Static part.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void staticPart(ConveyorBuilder b, String s) {
		LOG.debug("Applying staticPart={}", s);
		Pair value = (Pair) ConfigUtils.stringToLabelValuePairSupplier.apply(s);
		b.staticParts.add(value);
	}

	/**
	 * First result consumer.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void firstResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstResultConsumer={}", s);
		b.firstResultConsumer = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
	}

	/**
	 * Next result consumer.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void nextResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextResultConsumer={}", s);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
		b.nextResultConsumers.add(value);
	}

	/**
	 * First scrap consumer.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void firstScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstScrapConsumer={}", s);
		b.firstScrapConsumer = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
	}

	/**
	 * Next scrap consumer.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void nextScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextScrapConsumer={}", s);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
		b.nextScrapConsumers.add(value);
	}

	/**
	 * Timeout action.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void timeoutAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying timeoutAction={}", s);
		b.timeoutAction = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
	}

	/**
	 * Acknowledge action.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void acknowledgeAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying acknowledgeAction={}", s);
		b.acknowledgeAction = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
	}

	/**
	 * Adds the cart before placement validator.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void addCartBeforePlacementValidator(ConveyorBuilder b, String s) {
		LOG.debug("Applying addCartBeforePlacementValidator={}", s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addCartBeforePlacementValidator.add(value);
	}

	/**
	 * Adds the before key eviction action.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void addBeforeKeyEvictionAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyEvictionAction={}", s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addBeforeKeyEvictionAction.add(value);
	}

	/**
	 * Default cart consumer.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void defaultCartConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultCartConsumer={}", s);
		b.defaultCartConsumer = (LabeledValueConsumer) ConfigUtils.stringToLabeledValueConsumerSupplier.apply(s);
	}

	/**
	 * Readiness evaluator.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void readinessEvaluator(ConveyorBuilder b, String s) {
		LOG.debug("Applying readinessEvaluator={}", s);
		Object obj = ConfigUtils.stringToReadinessEvaluatorSupplier.apply(s);
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
	 * @param s the s
	 */
	public static void readyWhen(ConveyorBuilder b, String s) {
		LOG.debug("Applying readyWhen={}", s);
		String[] parts = s.trim().split("\\s+");
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
	 * @param s
	 *            the s
	 */
	public static void builderSupplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying builderSupplier={}", s);
		b.builderSupplier = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply(s);
	}

	/**
	 * Adds the before key rescheduling action.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void addBeforeKeyReschedulingAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyReschedulingAction={}", s);
		BiConsumer value = (BiConsumer) ConfigUtils.stringToBiConsumerSupplier.apply(s);
		b.addBeforeKeyReschedulingAction.add(value);
	}

	/**
	 * Accept labels.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void acceptLabels(ConveyorBuilder b, String s) {
		LOG.debug("Applying acceptLabels={}", s);
		Object[] value = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(s);
		b.acceptedLabels.addAll(Arrays.asList(value));
	}

	/**
	 * Enable postpone expiration.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void enablePostponeExpiration(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpiration={}", s);
		b.enablePostponeExpiration = Boolean.valueOf(s);
	}

	/**
	 * Enable postpone expiration on timeout.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void enablePostponeExpirationOnTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpirationOnTimeout={}", s);
		b.enablePostponeExpirationOnTimeout = Boolean.valueOf(s);
	}

	/**
	 * Auto acknowledge.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void autoAcknowledge(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledge={}", s);
		b.autoAcknowledge = Boolean.valueOf(s);
	}

	/**
	 * Auto acknowledge on status.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void autoAcknowledgeOnStatus(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledgeOnStatus={}", s);
		b.autoAcknowledgeOnStatus = (Status[]) ConfigUtils.stringToStatusConverter.apply(s);
	}

	/**
	 * Cart payload accessor.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void cartPayloadAccessor(ConveyorBuilder b, String s) {
		LOG.debug("Applying cartPayloadAccessor={}", s);
		b.cartPayloadAccessor = (Function) ConfigUtils.stringToFunctionSupplier.apply(s);
	}

	/**
	 * Forward.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void forward(ConveyorBuilder b, String s) {
		LOG.debug("Applying forward={}", s);
		Trio value = (Trio) ConfigUtils.stringToForwardTrioSupplier.apply(s);
		b.forward.add(value);
	}

	/**
	 * Supplier.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void supplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor supplier={}", s);
		b.constructor = ConfigUtils.stringToConveyorSupplier.apply(s);
		b.maxQueueSize = 0;
		b.enablePriorityQueue = false;
	}

	/**
	 * Persitence.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void persitence(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor persitence={}", s);
		b.persistence = s;
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
			b.allFilesRead = readOk;
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
		LOG.debug("Applying dependency={}", s);
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
		LOG.debug("Applying completed={}", s);
		if (b.dependencies.remove(s)) {
			b.completed.add(s);
		}
	}

	/**
	 * Parallel.
	 *
	 * @param b
	 *            the b
	 * @param s
	 *            the s
	 */
	public static void parallel(ConveyorBuilder b, String s) {
		LOG.debug("Applying parallel={}", s);
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
	 * @param s the s
	 */
	public static void maxQueueSize(ConveyorBuilder b, String s) {
		LOG.debug("Applying maxQueueSize={}", s);
		int maxSize = Integer.parseInt(s.split("\\s+")[0]);
		b.maxQueueSize = maxSize;
		if(b.maxQueueSize > 0) {
			b.enablePriorityQueue = false;
			b.constructor = ()->new AssemblingConveyor( ()->new ArrayBlockingQueue(maxSize) );
		}
	}

	public static void priority(ConveyorBuilder b, String s) {
		LOG.debug("Applying priority={}", s);
		Supplier<PriorityBlockingQueue<Cart>> queueSupplier = null;
		switch (s.toUpperCase()) {
			case "FIFO" -> queueSupplier = Priority.FIFO;
			case "FILO" -> queueSupplier = Priority.FILO;
			case "NEWEST_FIRST" -> queueSupplier = Priority.NEWEST_FIRST;
			case "OLDEST_FIRST" -> queueSupplier = Priority.OLDEST_FIRST;
			case "EXPIRE_SOONER_FIRST" -> queueSupplier = Priority.EXPIRE_SOONER_FIRST;
			case "PRIORITIZED" -> queueSupplier = Priority.PRIORITIZED;
			default -> {
				LOG.warn("Undefined priority {}. Ignored. Supported priorities:  FIFO,FILO,NEWEST_FIRST,OLDEST_FIRST,EXPIRE_SOONER_FIRST,PRIORITIZED", s);
				b.enablePriorityQueue = false;
				return;
			}
		}
		b.enablePriorityQueue  = true;
		b.maxQueueSize = 0;
		Supplier<PriorityBlockingQueue<Cart>> finalQueueSupplier = queueSupplier;
		b.constructor = ()->new AssemblingConveyor(finalQueueSupplier);
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
		LOG.debug("Applying{}persistenceProperty={}:{}={}", pp.isDefaultProperty() ? " default " : " ", key,
				pp.getProperty(), pp.getValue());
		PersistenceProperties pm = null;
		if (pp.isDefaultProperty()) {
			pm = b.defaultProperties.get(key);
			if (pm == null) {
				pm = new PersistenceProperties(pp.getType(), pp.getSchema(), pp.getName());
				b.defaultProperties.put(key, pm);
			}
		} else {
			pm = b.persistenceProperties.get(key);
			if (pm == null) {
				pm = new PersistenceProperties(pp.getType(), pp.getSchema(), pp.getName());
				b.persistenceProperties.put(key, pm);
			}
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
		return "ConveyorBuilder [" + (dependencies != null ? "dependencies=" + dependencies + ", " : "")
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

}
