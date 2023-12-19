package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.*;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import com.aegisql.conveyor.serial.SerializablePredicate;

import javax.management.ObjectName;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

/**
 * The Class PersistentConveyor.
 *
 * @param <K>
 *            the key type
 * @param <L>
 *            the generic type
 * @param <OUT>
 *            the generic type
 */
public class PersistentConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The forward. */
	private final Conveyor<K, L, OUT> forward;

	/** The ack conveyor. */
	private final AcknowledgeBuildingConveyor<K> ackConveyor;

	/** The cleaner. */
	private final PersistenceCleanupBatchConveyor<K> cleaner;

	/** The result consumer. */
	private ResultConsumer<K, OUT> resultConsumer;

	/** The forward persistence. */
	private final Persistence<K> forwardPersistence;

	/** The on status. */
	private final EnumMap<Status, Consumer<AcknowledgeStatus<K>>> onStatus = new EnumMap<>(Status.class);

	private String doNotPersist = "~";
	
	private ObjectName objectName;

	private String name;

	private volatile boolean suspended = false;
	
	public void setSkipPersistencePropertyKey(String doNotPersist) {
		this.doNotPersist = doNotPersist;
	}

	/**
	 * Instantiates a new persistent conveyor.
	 *
	 * @param persistence
	 *            the persistence
	 * @param forward
	 *            the forward
	 */
	public PersistentConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward) {

		/** The ack persistence. */
		Persistence<K> ackPersistence = persistence.copy();
		forwardPersistence = persistence.copy();
		/** The clean persistence. */
		Persistence<K> cleanPersistence = persistence.copy();

		this.forward = forward;
		this.cleaner = new PersistenceCleanupBatchConveyor<>(cleanPersistence);
		this.ackConveyor = new AcknowledgeBuildingConveyor<>(ackPersistence, forward);
		this.ackConveyor.staticPart().value(persistence.getMinCompactSize()).label(ackConveyor.MIN_COMPACT).place();

		String name = forward.getName();
		setName(name);
		onStatus.put(Status.READY, this::complete);
		onStatus.put(Status.CANCELED, this::complete);
		onStatus.put(Status.INVALID, this::complete);
		onStatus.put(Status.TIMED_OUT, this::complete);
		onStatus.put(Status.WAITING_DATA, k -> {
			throw new PersistenceException("Unexpected WAITING_DATA status for key=" + k);
		});
		forward.setAcknowledgeAction(status -> onStatus.get(status.getStatus()).accept(status));

		this.ackConveyor.staticPart().label(ackConveyor.MODE).value(true).place().join();
		if (forward != null && forward.getResultConsumer() != null) {
			this.resultConsumer = forward.getResultConsumer();
		} else {
			this.resultConsumer = bin -> {
			};
		}
		// not empty only if previous conveyor could not complete.
		// Pers must be initialized with the previous state
		Collection<Cart<K, ?, L>> staticParts = persistence.getAllStaticParts();
		LOG.debug("Static parts: {}", staticParts);
		Collection<Cart<K, ?, L>> allParts = persistence.getAllParts();
		LOG.debug("All parts: {}", allParts);
		staticParts.forEach(this::place);
		allParts.forEach(cart -> {
			long cartExpTime = cart.getExpirationTime();
			if (cartExpTime == 0 || cartExpTime > System.currentTimeMillis()) {
				cart.addProperty("#TIMESTAMP", System.nanoTime());
				if(cart.getLoadType() == LoadType.COMMAND) {
					forward.command((GeneralCommand) cart);
				} else {
					forward.place(cart);
				}
			} else {
				if (cleaner != null) {
					cleaner.part().label(cleaner.KEY).value(cart.getKey()).place();
					cleaner.part().label(cleaner.CART_ID).value(cart.getProperty("#CART_ID", Long.class)).place();
				}
			}
		});
		try {
			persistence.close();
		} catch (IOException e) {
			throw new PersistenceException(e.getMessage(), e);
		}
		this.ackConveyor.staticPart().label(ackConveyor.MODE).value(false).place().join();
	}

	/**
	 * Complete.
	 *
	 * @param status
	 *            the status
	 */
	private void complete(AcknowledgeStatus<K> status) {
		forwardPersistence.saveCompletedBuildKey(status.getKey());
		Set<Long> siteIds = new HashSet<>();
		for (Map.Entry<String, Object> en : status.getProperties().entrySet()) {
			if ("#CART_ID".equals(en.getValue())) {
				Long id = Long.parseLong(en.getKey());
				siteIds.add(id);
			}
		}
		cleaner.part().label(cleaner.KEY).value(status.getKey()).place();
		cleaner.part().label(cleaner.CART_IDS).value(siteIds).place();
		ackConveyor.part().id(status.getKey()).label(ackConveyor.COMPLETE).value(status).place();
	}

	/**
	 * Unload.
	 *
	 * @param status
	 *            the status
	 */
	private void unload(AcknowledgeStatus<K> status) {
		ackConveyor.part().id(status.getKey()).label(ackConveyor.UNLOAD).value(status).place();
	}

	/**
	 * Instantiates a new persistent conveyor.
	 *
	 * @param persistence
	 *            the persistence
	 */
	PersistentConveyor(Persistence<K> persistence) {
		this(persistence, new AssemblingConveyor<>());
	}

	/**
	 * Instantiates a new persistent conveyor.
	 *
	 * @param persistence
	 *            the persistence
	 * @param forwardSupplier
	 *            the forward supplier
	 */
	PersistentConveyor(Persistence<K> persistence, Supplier<Conveyor<K, L, OUT>> forwardSupplier) {
		this(persistence, forwardSupplier.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public PartLoader<K, L> part() {
		return new PartLoader<>(cl -> {
			Cart<K, ?, L> cart;
			if (cl.filter != null) {
				cart = new MultiKeyCart<>(cl.filter, cl.partValue, cl.label, cl.creationTime,
						cl.expirationTime, LoadType.PART, cl.priority);
			} else {
				cart = new ShoppingCart<>(cl.key, cl.partValue, cl.label, cl.creationTime,
						cl.expirationTime, cl.priority);
			}
			cl.getAllProperties().forEach(cart::addProperty);

			return place(cart);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#staticPart()
	 */
	@Override
	public StaticPartLoader<L> staticPart() {
		return new StaticPartLoader<>(cl -> {
			Map<String, Object> properties = new HashMap<>();
			properties.put("CREATE", cl.create);
			Cart<K, ?, L> staticPart = new ShoppingCart<>(null, cl.staticPartValue, cl.label,
					System.currentTimeMillis(), 0, properties, STATIC_PART, cl.priority);
			return place(staticPart);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT> build() {
		return new BuilderLoader<>(cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			// bs = builderSupplier;
			final CreatingCart<K, OUT, L> cart = new CreatingCart<>(cl.key, bs, cl.creationTime,
					cl.expirationTime, cl.priority);
			cl.getAllProperties().forEach(cart::addProperty);
			return place(cart);
		}, cl -> {
			throw new PersistenceException("Futures not supported in persistent builde suppliers");
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#future()
	 */
	@Override
	public FutureLoader<K, OUT> future() {
		// do not save future
		return forward.future();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#command()
	 */
	@Override
	public CommandLoader<K, OUT> command() {
		// do not save command
		return forward.command();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#place(com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		
		if(cart.getAllProperties() != null && cart.getAllProperties().containsKey(doNotPersist )) {
			return forward.place(cart);
		}
		
		Cart<K, Cart<K, ?, ?>, SmartLabel<AcknowledgeBuilder<K>>> ackCart = PersistenceCart.of(cart, ackConveyor.CART);
		LOG.debug("PLACING " + ackCart);
		CompletableFuture<Boolean> forwardFuture = cart.getFuture();
		CompletableFuture<Boolean> ackFuture = ackCart.getFuture();
		CompletableFuture<Boolean> bothFutures = ackFuture.thenCombine(forwardFuture, (a, b) -> a && b);
		ackConveyor.place(ackCart);
		return bothFutures;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#command(com.aegisql.conveyor.cart.command.
	 * GeneralCommand)
	 */
	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		return forward.command(command);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer()
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return new ResultConsumerLoader<>(rcl -> {
			final Cart<K, ?, L> cart;
			if (rcl.key != null) {
				cart = new ResultConsumerCart<>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime, rcl.priority);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, LoadType.RESULT_CONSUMER,rcl.priority);
			}
			rcl.getAllProperties().forEach(cart::addProperty);
			return this.place(cart);
		}, rc -> {
			this.resultConsumer = rc;
			this.forward.resultConsumer(rc).set();
		}, resultConsumer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer(com.aegisql.conveyor.
	 * consumers.result.ResultConsumer)
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer) {
		return this.resultConsumer().first(consumer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getCollectorSize()
	 */
	@Override
	public int getCollectorSize() {
		return ackConveyor.getCollectorSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getInputQueueSize()
	 */
	@Override
	public int getInputQueueSize() {
		return ackConveyor.getInputQueueSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getDelayedQueueSize()
	 */
	@Override
	public int getDelayedQueueSize() {
		return ackConveyor.getDelayedQueueSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer()
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {

		return new ScrapConsumerLoader<>(sc->{
			ackConveyor.scrapConsumer(sc).set();
			forward.scrapConsumer(sc).set();
		}, LogScrap.error(this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer(com.aegisql.conveyor.
	 * consumers.scrap.ScrapConsumer)
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {

		return new ScrapConsumerLoader<>(sc->{
			ackConveyor.scrapConsumer(sc).set();
			forward.scrapConsumer(sc).set();
		}, scrapConsumer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#stop()
	 */
	@Override
	public void stop() {
		forward.stop();
		ackConveyor.stop();
		cleaner.stop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#completeAndStop()
	 */
	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		CompletableFuture<Boolean> fFuture = forward.completeAndStop();
		CompletableFuture<Boolean> aFuture = ackConveyor.completeAndStop();
		CompletableFuture<Boolean> cFuture = cleaner.completeAndStop();
		return fFuture.thenCombine(aFuture, (a, b) -> a && b).thenCombine(cFuture, (a, b) -> a && b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setIdleHeartBeat(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit) {
		forward.setIdleHeartBeat(heartbeat, unit);
		ackConveyor.setIdleHeartBeat(heartbeat, unit);
		cleaner.setIdleHeartBeat(heartbeat, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setIdleHeartBeat(java.time.Duration)
	 */
	@Override
	public void setIdleHeartBeat(Duration duration) {
		forward.setIdleHeartBeat(duration);
		ackConveyor.setIdleHeartBeat(duration);
		cleaner.setIdleHeartBeat(duration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setDefaultBuilderTimeout(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		forward.setDefaultBuilderTimeout(builderTimeout, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setDefaultBuilderTimeout(java.time.
	 * Duration)
	 */
	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		forward.setDefaultBuilderTimeout(duration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#rejectUnexpireableCartsOlderThan(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		forward.rejectUnexpireableCartsOlderThan(timeout, unit);
		ackConveyor.rejectUnexpireableCartsOlderThan(timeout, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#rejectUnexpireableCartsOlderThan(java.time.
	 * Duration)
	 */
	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		forward.rejectUnexpireableCartsOlderThan(duration);
		ackConveyor.rejectUnexpireableCartsOlderThan(duration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setOnTimeoutAction(java.util.function.
	 * Consumer)
	 */
	@Override
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		forward.setOnTimeoutAction(timeoutAction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#setDefaultCartConsumer(com.aegisql.conveyor
	 * .LabeledValueConsumer)
	 */
	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		forward.setDefaultCartConsumer(cartConsumer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#setReadinessEvaluator(java.util.function.
	 * BiPredicate)
	 */
	@Override
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready) {
		forward.setReadinessEvaluator(ready);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#setReadinessEvaluator(java.util.function.
	 * Predicate)
	 */
	@Override
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		forward.setReadinessEvaluator(readiness);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#setBuilderSupplier(com.aegisql.conveyor.
	 * BuilderSupplier)
	 */
	@Override
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		forward.setBuilderSupplier(builderSupplier);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setName(java.lang.String)
	 */
	@Override
	public void setName(String string) {
		this.name = string;
		forward.setName("Persistent<"+string+">");
		this.setMbean(string);
		ackConveyor.setName("AcknowledgeBuildingConveyor<" + string + ">");
		cleaner.setName("PersistenceCleanupBatchConveyor<" + string + ">");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return forward.isRunning() && ackConveyor.isRunning() && cleaner.isRunning();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#addCartBeforePlacementValidator(java.util.
	 * function.Consumer)
	 */
	@Override
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		forward.addCartBeforePlacementValidator(cartBeforePlacementValidator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyEvictionAction(java.util.
	 * function.Consumer)
	 */
	@Override
	public void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction) {
		forward.addBeforeKeyEvictionAction(keyBeforeEviction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#addBeforeKeyReschedulingAction(java.util.
	 * function.BiConsumer)
	 */
	@Override
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		forward.addBeforeKeyReschedulingAction(keyBeforeRescheduling);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getExpirationTime(java.lang.Object)
	 */
	@Override
	public long getExpirationTime(K key) {
		return forward.getExpirationTime(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#isLBalanced()
	 */
	@Override
	public boolean isLBalanced() {
		return forward.isLBalanced();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getAcceptedLabels()
	 */
	@Override
	public Set<L> getAcceptedLabels() {
		return forward.getAcceptedLabels();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#acceptLabels(java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void acceptLabels(L... labels) {
		forward.acceptLabels(labels);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpiration(boolean)
	 */
	@Override
	public void enablePostponeExpiration(boolean flag) {
		forward.enablePostponeExpiration(flag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#enablePostponeExpirationOnTimeout(boolean)
	 */
	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		forward.enablePostponeExpirationOnTimeout(flag);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		forward.setExpirationPostponeTime(time, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(java.time.
	 * Duration)
	 */
	@Override
	public void setExpirationPostponeTime(Duration duration) {
		forward.setExpirationPostponeTime(duration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#isForwardingResults()
	 */
	@Override
	public boolean isForwardingResults() {
		return forward.isForwardingResults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getCartCounter()
	 */
	@Override
	public long getCartCounter() {
		return forward.getCartCounter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setAutoAcknowledge(boolean)
	 */
	@Override
	public void setAutoAcknowledge(boolean auto) {
		forward.setAutoAcknowledge(auto);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#setAcknowledgeAction(java.util.function.
	 * Consumer)
	 */
	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		Consumer<AcknowledgeStatus<K>> first = status -> onStatus.get(status.getStatus()).accept(status);
		forward.setAcknowledgeAction(first.andThen(ackAction));
	}

	/**
	 * Gets the working conveyor.
	 *
	 * @return the working conveyor
	 */
	public Conveyor<K, L, OUT> getWorkingConveyor() {
		return forward;
	}

	/**
	 * Gets the acknowledge building conveyor.
	 *
	 * @return the acknowledge building conveyor
	 */
	public AcknowledgeBuildingConveyor<K> getAcknowledgeBuildingConveyor() {
		return ackConveyor;
	}

	/**
	 * Gets the cleaning conveyor.
	 *
	 * @return the cleaning conveyor
	 */
	public PersistenceCleanupBatchConveyor<K> getCleaningConveyor() {
		return cleaner;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#autoAcknowledgeOnStatus(com.aegisql.
	 * conveyor.Status, com.aegisql.conveyor.Status[])
	 */
	@Override
	public void autoAcknowledgeOnStatus(Status first, Status... other) {
		forward.autoAcknowledgeOnStatus(first, other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getResultConsumer()
	 */
	@Override
	public ResultConsumer<K, OUT> getResultConsumer() {
		return resultConsumer;
	}

	/**
	 * Unload on builder timeout.
	 *
	 * @param unload
	 *            the unload
	 */
	public void unloadOnBuilderTimeout(boolean unload) {
		if (unload) {
			onStatus.put(Status.TIMED_OUT, this::unload);
		} else {
			onStatus.put(Status.TIMED_OUT, this::complete);
		}
		ackConveyor.staticPart().label(ackConveyor.UNLOAD_ENABLED).value(unload).place();
	}

	@Override
	public void interrupt(String conveyorName) {
		forward.interrupt(conveyorName);
	}

	@Override
	public void setCartPayloadAccessor(Function<Cart<K, ?, L>, Object> payloadFunction) {
		forward.setCartPayloadAccessor(payloadFunction);
	}
	
	public CompletableFuture<Boolean> compact(K key) {
		return ackConveyor.part().id(key).value(key).label(ackConveyor.COMPACT).place();
	}

	public CompletableFuture<Boolean> compact() {
		return ackConveyor.part().foreach().value(null).label(ackConveyor.COMPACT).place();
	}

	public CompletableFuture<Boolean> compact(SerializablePredicate<K> p) {
		return ackConveyor.part().foreach(p).value(null).label(ackConveyor.COMPACT).place();
	}

	/**
	 * Sets the mbean.
	 *
	 * @param name the new mbean
	 */
	protected void setMbean(String name) {
			final PersistentConveyor<K,L,OUT> thisConv = this;

			Conveyor.register(this, new PersistentConveyorMBean() {
				@Override
				public String getName() {
					return name;
				}
				@Override
				public String getType() {
					return thisConv.getClass().getSimpleName();
				}
				@Override
				public boolean isRunning() {
					return thisConv.forward.isRunning();
				}
				@Override
				public <K, L, OUT> Conveyor<K, L, OUT> conveyor() {
					return (Conveyor<K, L, OUT>) thisConv;
				}
				@Override
				public void stop() {
					thisConv.stop();
				}
				@Override
				public void completeAndStop() {
					thisConv.completeAndStop();
				}
				@Override
				public void idleHeartBeatMsec(long msec) {
					if(msec > 0) {
						thisConv.setIdleHeartBeat(msec, TimeUnit.MILLISECONDS);
					}					
				}

				@Override
				public void defaultBuilderTimeoutMsec(long msec) {
					if(msec > 0) {
						thisConv.setDefaultBuilderTimeout(msec, TimeUnit.MILLISECONDS);
					}
				}

				@Override
				public void rejectUnexpireableCartsOlderThanMsec(long msec) {
					if(msec > 0) {
						thisConv.rejectUnexpireableCartsOlderThan(msec, TimeUnit.MILLISECONDS);
					}
				}

				@Override
				public void expirationPostponeTimeMsec(long msec) {
					if(msec > 0) {
						thisConv.setExpirationPostponeTime(msec, TimeUnit.MILLISECONDS);
					}					
				}
				@Override
				public String getPersistenceDescription() {
					return ""+forwardPersistence;
				}
				@Override
				public boolean isSuspended() {
					return thisConv.suspended;
				}
				@Override
				public void suspend() {
					thisConv.suspend();					
				}
				@Override
				public void resume() {
					thisConv.resume();					
				}
			});
	}

	@Override
	public void suspend() {
		this.suspended = true;
		forward.suspend();
	}

	@Override
	public void resume() {
		this.suspended = false;
		forward.resume();
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public Class<?> mBeanInterface() {
		return PersistentConveyorMBean.class;
	}

	@Override
	public ConveyorMetaInfo<K, L, OUT> getMetaInfo() {
		return forward.getMetaInfo();
	}


}
