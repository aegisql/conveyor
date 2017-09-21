package com.aegisql.conveyor.persistence.core;

import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.BuilderLoader;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.ResultConsumerLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;

// TODO: Auto-generated Javadoc
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

	/** The static acknowledge builder. */
	private final AcknowledgeBuilder<K> staticAcknowledgeBuilder;

	/** The result consumer. */
	private ResultConsumer<K, OUT> resultConsumer = bin -> {
	};

	/** The ack persistence. */
	private final Persistence<K> ackPersistence;

	/** The forward persistence. */
	private final Persistence<K> forwardPersistence;

	/** The clean persistence. */
	private final Persistence<K> cleanPersistence;

	/** The static persistence. */
	private final Persistence<K> staticPersistence;

	/** The on status. */
	private final EnumMap<Status, Consumer<AcknowledgeStatus<K>>> onStatus = new EnumMap<>(Status.class);

	/** The initialization mode. */
	private final AtomicBoolean initializationMode = new AtomicBoolean(true);

	/**
	 * Instantiates a new persistent conveyor.
	 *
	 * @param persistence
	 *            the persistence
	 * @param forward
	 *            the forward
	 */
	public PersistentConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward) {

		ackPersistence = persistence.copy();
		forwardPersistence = persistence.copy();
		cleanPersistence = persistence.copy();
		staticPersistence = persistence.copy();

		this.forward = forward;
		this.cleaner = new PersistenceCleanupBatchConveyor<>(cleanPersistence);
		this.ackConveyor = new AcknowledgeBuildingConveyor<>(ackPersistence, forward, cleaner);
		onStatus.put(Status.READY, this::complete);
		onStatus.put(Status.CANCELED, this::complete);
		onStatus.put(Status.INVALID, this::complete);
		onStatus.put(Status.TIMED_OUT, this::complete);
		onStatus.put(Status.WAITING_DATA, k -> {
			throw new RuntimeException("Unexpected WAITING_DATA status for key=" + k);
		});
		forward.setAcknowledgeAction(status -> {
			onStatus.get(status.getStatus()).accept(status);
		});

		this.ackConveyor.staticPart().label(ackConveyor.MODE).value(true).place().join();
		if (forward != null && forward.getResultConsumer() != null) {
			this.resultConsumer = forward.getResultConsumer();
		} else {
			this.resultConsumer = bin -> {
			};
		}
		this.staticAcknowledgeBuilder = new AcknowledgeBuilder<>(staticPersistence, forward, ackConveyor);
		// not empty only if previous conveyor could not complete.
		// Pers must be initialized with the previous state
		Collection<Cart<K, ?, L>> staticParts = persistence.<L> getAllStaticParts();
		LOG.debug("Static parts: {}", staticParts);
		Collection<Cart<K, ?, L>> allParts = persistence.<L> getAllParts();
		LOG.debug("All parts: {}", allParts);
		staticParts.forEach(cart -> this.place(cart));
		allParts.forEach(cart -> {
			long cartExpTime = cart.getExpirationTime();
			if (cartExpTime == 0 || cartExpTime > System.currentTimeMillis()) {
				this.place(cart);
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
			throw new RuntimeException(e.getMessage(), e);
		}
		this.initializationMode.set(false);
		this.ackConveyor.setInitializationMode(false);
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
	public PersistentConveyor(Persistence<K> persistence) {
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
	public PersistentConveyor(Persistence<K> persistence, Supplier<Conveyor<K, L, OUT>> forwardSupplier) {
		this(persistence, forwardSupplier.get());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		return new PartLoader<K, L, X, OUT, Boolean>(cl -> {
			Cart<K, Object, L> cart;
			if (cl.filter != null) {
				cart = new MultiKeyCart<K, Object, L>(cl.filter, cl.partValue, cl.label, cl.creationTime,
						cl.expirationTime);
			} else {
				cart = new ShoppingCart<K, Object, L>(cl.key, cl.partValue, cl.label, cl.creationTime,
						cl.expirationTime);
			}
			cl.getAllProperties().forEach((k, v) -> cart.addProperty(k, v));

			return place(cart);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#staticPart()
	 */
	@Override
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart() {
		return new StaticPartLoader<L, X, OUT, Boolean>(cl -> {
			Map<String, Object> properties = new HashMap<>();
			properties.put("CREATE", cl.create);
			Cart<K, ?, L> staticPart = new ShoppingCart<>(null, cl.staticPartValue, cl.label,
					System.currentTimeMillis(), 0, properties, STATIC_PART);
			return place(staticPart);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		return new BuilderLoader<K, OUT, Boolean>(cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			if (bs == null) {
				// bs = builderSupplier;
			}
			final CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime,
					cl.expirationTime);
			cl.getAllProperties().forEach((k, v) -> {
				cart.addProperty(k, v);
			});
			return place(cart);
		}, cl -> {
			throw new RuntimeException("Futures not supported in persistent builde suppliers");
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
		if (cart.getKey() == null || cart.getLoadType() == LoadType.RESULT_CONSUMER) {
			CompletableFuture<Boolean> f = cart.getFuture();
			try {
				AcknowledgeBuilder.processCart(staticAcknowledgeBuilder, cart);
				f.complete(true);
			} catch (Exception e) {
				f.completeExceptionally(e);
			}
			return f;
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
				cart = new ResultConsumerCart<K, OUT, L>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime);
			} else {
				//BUG - do not copy properties
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, k -> {
					return new ResultConsumerCart<K, OUT, L>(k, rcl.consumer, rcl.creationTime, rcl.expirationTime);
				});
			}
			rcl.getAllProperties().forEach((k, v) -> {
				cart.addProperty(k, v);
			});
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
		return forward.scrapConsumer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer(com.aegisql.conveyor.
	 * consumers.scrap.ScrapConsumer)
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {
		return forward.scrapConsumer(scrapConsumer);
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
		forward.setName(string);
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
		return "PersistentConveyor<" + forward.getName() + ">";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#forwardResultTo(com.aegisql.conveyor.
	 * Conveyor, java.lang.Object)
	 */
	@Override
	public <L2, OUT2> void forwardResultTo(Conveyor<K, L2, OUT2> destination, L2 label) {
		forward.forwardResultTo(destination, label);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#forwardResultTo(com.aegisql.conveyor.
	 * Conveyor, java.util.function.Function, java.lang.Object)
	 */
	@Override
	public <K2, L2, OUT2> void forwardResultTo(Conveyor<K2, L2, OUT2> destination,
			Function<ProductBin<K, OUT>, K2> keyConverter, L2 label) {
		forward.forwardResultTo(destination, keyConverter, label);
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

}
