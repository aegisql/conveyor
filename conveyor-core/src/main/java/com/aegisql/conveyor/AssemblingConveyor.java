/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.cart.*;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ForwardResult.ForwardingConsumer;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.delay.DelayProvider;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

import static com.aegisql.conveyor.cart.LoadType.*;
import static com.aegisql.conveyor.validation.CommonValidators.*;

/**
 * The Class AssemblingConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 * @param <K>
 *            the key type
 * @param <L>
 *            the label type
 * @param <OUT>
 *            the product type
 */
public class AssemblingConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	protected final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	/** The in queue. */
	protected final Queue<Cart<K, ?, L>> inQueue;

	/** The m queue. */
	protected final Queue<GeneralCommand<K, ?>> mQueue;

	/** The delay provider. */
	private final DelayProvider<K> delayProvider = new DelayProvider<>();

	/** The collector. */
	protected final Map<K, BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT>> collector = new HashMap<>();

	/**  Keeps static values. */
	protected final Map<L,Cart<K,?,L>> staticValues = new HashMap<>();
	
	/** The cart counter. */
	protected long cartCounter = 0;

	/** The command counter. */
	protected long commandCounter = 0;

	/** The builder timeout. */
	protected long builderTimeout = 0;

	/** The start time reject. */
	protected long startTimeReject = System.currentTimeMillis();

	protected long lastProcessingTime = System.currentTimeMillis();

	protected long longInactivityTime = Long.MAX_VALUE;

	/** The timeout action. */
	protected Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The result consumer. */
	protected ResultConsumer <K,OUT> resultConsumer = null;

	/** The scrap logger. */
	protected ScrapConsumer<K,?> scrapLogger = scrapBin-> LOG.error("{}",scrapBin);

	/** The scrap consumer. */
	protected ScrapConsumer<K,?> scrapConsumer = scrapLogger;
	
	/** The cart consumer. */
	protected LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>> cartConsumer = (l, v, b) -> {
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The payload function. */
	protected Function<Cart<K,?,L>,Object> payloadFunction = Cart::getValue;

	/** The ready. */
	protected BiPredicate<State<K, L>, Supplier<? extends OUT>> readiness = null;

	/** The builder supplier. */
	protected BuilderSupplier<OUT> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};

	/** The cart before placement validator. */
	protected Consumer<Cart<K, ?, L>> cartBeforePlacementValidator = cart -> {
		if (cart == null)
			throw new NullPointerException("Cart is null");
	};

	/** The command before placement validator. */
	private Consumer<GeneralCommand<K, ?>> commandBeforePlacementValidator = cart -> {
		if (cart == null)
			throw new NullPointerException("Command is null");
	};

	/** The auto ack. */
	protected boolean autoAck = true;

	protected boolean existingBuildsFirst;
	
	/** The ack action. */
	protected Consumer<AcknowledgeStatus<K>> ackAction = status->{};

	/** The ack status set. */
	private EnumSet<Status> ackStatusSet = EnumSet.allOf(Status.class);

	/** The key before eviction. */
	private Consumer<AcknowledgeStatus<K>> keyBeforeEviction = status -> {
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> removed = collector.remove(status.getKey());
		if(removed != null) {
			LOG.trace("Key {} has been removed. status:{}", status.getKey(), status.getStatus());
			if (autoAck) {
				if (ackStatusSet.contains(status.getStatus())) {
					ackAction.accept(status);
				} else {
					LOG.debug("Auto Acknowledge for key {} not applicable for status {} of {}", status.getKey(), status.getStatus(), ackStatusSet);
				}
			}
		} else {
			LOG.trace("Key {} was not found. Eviction action ignored", status.getKey());
		}
	};

	/** The key before reschedule. */
	private BiConsumer<K, Long> keyBeforeReschedule = (key, newExpirationTime) -> {
		Objects.requireNonNull(key, "NULL key cannot be rescheduled");
		Objects.requireNonNull(newExpirationTime, "NULL newExpirationTime cannot be applied to the schedule");
		var buildingSite = collector.get(key);
		if (buildingSite != null) {
			long oldExpirationTime = buildingSite.expireableSource.getExpirationTime();
			delayProvider.getBox(oldExpirationTime).delete(key);
			buildingSite.updateExpirationTime(newExpirationTime);
			LOG.trace("Rescheduled {}. added expiration {} msec", key, newExpirationTime - oldExpirationTime);
			if (newExpirationTime > 0) {
				delayProvider.getBox(newExpirationTime).add(key);
			}
		} else {
			LOG.trace("Build is not found for the key {}", key);
		}
	};

	/** The running. */

	protected volatile boolean running = true;

	protected volatile boolean suspended = false;

	/** The synchronize builder. */
	protected boolean synchronizeBuilder = false;

	/** The accepted labels. */
	protected final Set<L> acceptedLabels = new HashSet<>();

	/** The conveyor future. */
	protected volatile CompletableFuture<Boolean> conveyorFuture = null;
	
	/** The conveyor future lock. */
	protected final Object conveyorFutureLock = new Object();
	
	/** The inner thread. */
	protected final Thread innerThread;

	private Runnable longInactivityAction = ()->{};

	/**
	 * The Class Lock.
	 */
	private static final class Lock {

		/** The r lock. */
		private final ReentrantLock rLock = new ReentrantLock(true);

		/** The has carts. */
		private final Condition hasCarts = rLock.newCondition();

		/** The expiration collection interval. */
		private long expirationCollectionInterval = 1000; //Long.MAX_VALUE;

		/**
		 * Sets the expiration collection interval.
		 *
		 * @param expirationCollectionInterval
		 *            the new expiration collection interval
		 */
		public void setExpirationCollectionInterval(long expirationCollectionInterval) {
			this.expirationCollectionInterval = expirationCollectionInterval;
		}

		/**
		 * Sets the expiration collection unit.
		 *
		 * @param expirationCollectionUnit
		 *            the new expiration collection unit
		 */
		public void setExpirationCollectionUnit(TimeUnit expirationCollectionUnit) {
			this.expirationCollectionUnit = expirationCollectionUnit;
		}

		/** The expiration collection unit. */
		private TimeUnit expirationCollectionUnit = TimeUnit.MILLISECONDS;

		/**
		 * Tell.
		 */
		public void tell() {
			rLock.lock();
			try {
				hasCarts.signal();
			} finally {
				rLock.unlock();
			}
		}

		/**
		 * Wait data.
		 *
		 * @param q
		 *            the q
		 * @throws InterruptedException
		 *             the interrupted exception
		 */
		public void waitData(boolean suspended, Queue<?> q) throws InterruptedException {
				if (suspended || q.isEmpty()) {
					//noinspection ResultOfMethodCallIgnored
					rLock.lockInterruptibly();
					try {
						hasCarts.await(expirationCollectionInterval, expirationCollectionUnit);
					} finally {
						rLock.unlock();
					}
				}
		}

	}

	/** The lock. */
	private final Lock lock = new Lock();

	/** The save carts. */
	private boolean saveCarts;
	
	/** The name. */
	private String name;

	/** The l balanced. */
	private boolean lBalanced = false;

	/** The postpone expiration enabled. */
	private boolean postponeExpirationEnabled = false;

	/** The postpone expiration mills. */
	private long postponeExpirationMills = 0;

	/** The forwarding results. */
	private boolean forwardingResults = false;

	/** The forwarding to. */
	private final List<String> forwardingTo = new ArrayList<>();

	/** The postpone expiration on timeout enabled. */
	private boolean postponeExpirationOnTimeoutEnabled;

	/** The current site. */
	private volatile BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT>  currentSite;

	public final Supplier<K> current_id = () -> currentSite.getKey();

	public final Supplier<L> current_label = () -> currentSite.getLastCart().getLabel();

	public final Supplier<Map<String,Object>> current_properties = () -> currentSite.getProperties();

	public final Function<String,Object> current_property = name->currentSite.getProperties().get(name);

	public final Supplier<Long> current_creation_time = () -> currentSite.getCreatingCart().getCreationTime();

	public final Supplier<Long> current_expiration_time = () -> currentSite.getExpirationTime();

	public final Supplier<LoadType> current_load_type = () -> currentSite.getLastCart().getLoadType();

	public final Supplier<Status> current_status = () -> currentSite.getStatus();

	/** The status line. */
	protected String statusLine = "Accepting Parts";

	/**
	 * Instantiates a new assembling conveyor.
	 */
	public AssemblingConveyor() {
		this( Priority.DEFAULT);
	}

	/**
	 * Instantiates a new assembling conveyor.
	 *
	 * @param cartQueueSupplier
	 *            the cart queue supplier
	 */
	public AssemblingConveyor(Supplier<Queue<? extends Cart<K, ?, ?>>> cartQueueSupplier) {
		this.inQueue = (Queue<Cart<K, ?, L>>) cartQueueSupplier.get();
		this.mQueue = (Queue<GeneralCommand<K, ?>>) cartQueueSupplier.get();
		existingBuildsFirst = Priority.EXISTING_BUILDS_FIRST.equals(cartQueueSupplier);
		this.addCartBeforePlacementValidator(CART_NOT_NULL());
		this.addCartBeforePlacementValidator(NOT_RUNNING(()->running,()->name));
		this.addCartBeforePlacementValidator(CART_EXPIRED());
		this.addCartBeforePlacementValidator(CART_TOO_OLD(()->startTimeReject));

		commandBeforePlacementValidator = commandBeforePlacementValidator.andThen(cmd -> {
			if (!running) {
				throw new IllegalStateException("Conveyor "+getName()+" is not running");
			}
		}).andThen(cmd -> {
			if (cmd.expired()) {
				throw new IllegalStateException("Command has already expired " + cmd);
			}
		}).andThen(cmd -> {
			if (cmd.getCreationTime() < (System.currentTimeMillis() - startTimeReject)) {
				throw new IllegalStateException("Command is too old " + cmd);
			}
		});
		acceptedLabels.add(null);
		this.innerThread = new Thread(() -> {
			try {
				while (running || ! inQueue.isEmpty() || ! mQueue.isEmpty()) {
					if (!waitData()) {
						break; //When interrupted, which is exceptional behavior, should return right away
					}
					processManagementCommands();
					if(suspended) {
						continue;
					}
					var cart = inQueue.poll();
					if (cart != null) {
						cartCounter++;
						processSite(cart, true);
					}
					removeExpired();
					if(this.conveyorFuture != null && (inQueue.peek() == null) && (mQueue.peek() == null) && (collector.size() == 0)) {
						running = false;
						this.conveyorFuture.complete(true);
						statusLine = "Completed all tasks and stopped";
						LOG.info("No pending messages or commands. Ready to leave {}", Thread.currentThread().getName());
					}
					currentSite = null;
					if((System.currentTimeMillis() - this.lastProcessingTime) > this.longInactivityTime && this.collector.isEmpty()) {
						LOG.info("Long inactivity time reached");
						this.longInactivityAction.run();
						this.lastProcessingTime = System.currentTimeMillis();
					}
				}
				LOG.info("Leaving {}", Thread.currentThread().getName());
				drainQueues();
			} catch (Throwable e) { // Let it crash, but don't pretend it is running
				stop();
				statusLine = "Unrecoverable error: "+e.getMessage();
				throw e;
			}
		});
		innerThread.setDaemon(false);
		this.name = "AssemblingConveyor " + innerThread.threadId();
		innerThread.setName(this.name);
		this.setMbean(this.name);
		innerThread.start();
	}

	/**
	 * Wait data.
	 *
	 * @return true, if successful
	 */
	private boolean waitData() {
		try {
			lock.waitData(suspended, inQueue);
		} catch (InterruptedException e) {
			LOG.info("Interrupted {}",name,e);
			stop();
			Thread.currentThread().interrupt();
		}
		return running;
	}

	/**
	 * Gets the building site.
	 *
	 * @param cart
	 *            the cart
	 * @return the building site
	 */
	private BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> getBuildingSite(Cart<K, ?, L> cart) {
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> buildingSite = null;
		var returnNull = false;
		var key = cart.getKey();
		if (key == null && (cart.getValue() != null && !(cart.getValue() instanceof Cart))) {
			returnNull = true;
		} else if (Status.TIMED_OUT.equals(cart.getValue())) {
			returnNull = true;
		} else if ((buildingSite = collector.get(key)) == null) {
			BuilderSupplier<OUT> bs;
			if (cart.getValue() != null && cart.getValue() instanceof BuilderSupplier) {
				bs = ((Supplier<BuilderSupplier<OUT>>) cart).get();
				if (bs == null) {
					bs = builderSupplier;
				}

				if (bs != null) {
					buildingSite = new BuildingSite<>(cart, bs, cartConsumer, readiness,
							timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
							postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer,
							ackAction,this);
					if (cart.getValue() instanceof FutureSupplier futureSupplier) {
						buildingSite.addFuture(futureSupplier.getFuture());
					}
					buildingSite.addProperties(cart.getAllProperties());
				} else {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(this, cart.getKey(), cart,
							"Ignore cart. Neither creating cart nor default builder supplier available",
							null,
							FailureType.BUILD_INITIALIZATION_FAILED,cart.getAllProperties(), null));
				}
				returnNull = true;
			} else if (builderSupplier != null) {
				buildingSite = new BuildingSite<>(cart, builderSupplier, cartConsumer,
						readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
						postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer,
						ackAction,this);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(this, cart.getKey(), cart,
						"Ignore cart. Neither builder nor builder supplier available",
						null,
						FailureType.BUILD_INITIALIZATION_FAILED,cart.getAllProperties(), null));
				returnNull = true;
			}
			if (buildingSite != null) {
				collector.put(key, buildingSite);
				if (buildingSite.isExpireable()) {
					delayProvider.getBox(buildingSite.getExpirationTime()).add(key);
				}
			}
		}
		if (returnNull) {
			return null;
		} else {
			return buildingSite;
		}
	}


	/**
	 * Sets the mbean.
	 *
	 * @param name
	 *            the new mbean
	 */
	protected void setMbean(String name) {
			final var thisConv = this;
			Conveyor.register(this, new AssemblingConveyorMBean() {
				@Override
				public String getGenericName() {
					return thisConv.getGenericName();
				}

				@Override
				public String getName() {
					return thisConv.name;
				}

				@Override
				public long getThreadId() {
					return thisConv.innerThread.threadId();
				}

				@Override
				public int getInputQueueSize() {
					return thisConv.inQueue.size();
				}

				@Override
				public int getCollectorSize() {
					return thisConv.collector.size();
				}

				@Override
				public int getCommandQueueSize() {
					return thisConv.mQueue.size();
				}

				@Override
				public String getType() {
					return thisConv.getClass().getSimpleName();
				}

				@Override
				public boolean isRunning() {
					return thisConv.running;
				}

				@Override
				public long getDefaultBuilderTimeoutMsec() {
					return thisConv.builderTimeout;
				}

				@Override
				public long getIdleHeartBeatMsec() {
					return thisConv.lock.expirationCollectionUnit.toMillis(thisConv.lock.expirationCollectionInterval);
				}

				@Override
				public long getExpirationPostponeTimeMsec() {
					if (thisConv.postponeExpirationEnabled) {
						return thisConv.postponeExpirationMills;
					} else {
						return 0;
					}
				}

				@Override
				public String getForwardingResultsTo() {
					return thisConv.forwardingTo.toString();
				}

				@Override
				public boolean isLBalanced() {
					return thisConv.lBalanced;
				}

				@Override
				public String getAcceptedLabels() {
					if (acceptedLabels.isEmpty() || acceptedLabels.contains(null)) {
						return "accepts all labels";
					} else {
						return acceptedLabels.toString();
					}
				}

				@Override
				public long getCartCounter() {
					return thisConv.cartCounter;
				}

				@Override
				public long getCommandCounter() {
					return thisConv.commandCounter;
				}

				@Override
				public Conveyor<K, L, OUT> conveyor() {
					return thisConv;
				}

				@Override
				public void stop() {
					LOG.info("Conveyor {} received JMX stop",name);
					thisConv.stop();
					
				}

				@Override
				public void completeAndStop() {
					LOG.info("Conveyor {} received JMX completeAndStop",name);
					thisConv.completeAndStop();
				}

				@Override
				public void interrupt() {
					LOG.info("Conveyor {} changed received JMX interrupt command",name);
					thisConv.interrupt(name);
				}

				@Override
				public void setIdleHeartBeatMsec(long msec) {
					if(msec > 0) {
						thisConv.setIdleHeartBeat(msec, TimeUnit.MILLISECONDS);
						LOG.info("Conveyor {} changed IdleHeartBeat to {}msec",name,msec);
					}					
				}

				@Override
				public void setDefaultBuilderTimeoutMsec(long msec) {
					if(msec > 0) {
						thisConv.setDefaultBuilderTimeout(msec, TimeUnit.MILLISECONDS);
						LOG.info("Conveyor {} changed DefaultBuilderTimeout to {}msec",name,msec);
					}
				}

				@Override
				public void rejectUnexpireableCartsOlderThanMsec(long msec) {
					if(msec > 0) {
						thisConv.rejectUnexpireableCartsOlderThan(msec, TimeUnit.MILLISECONDS);
						LOG.info("Conveyor {} changed rejectUnexpireableCartsOlderThan to {}msec",name,msec);
					}
				}

				@Override
				public void setExpirationPostponeTimeMsec(long msec) {
					if(msec > 0) {
						thisConv.setExpirationPostponeTime(msec, TimeUnit.MILLISECONDS);
						LOG.info("Conveyor {} changed ExpirationPostponeTime to {}msec",name,msec);
					}					
				}

				@Override
				public String getStatus() {
					return thisConv.statusLine ;
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

	/**
	 * Process management commands.
	 */
	private void processManagementCommands() {
		GeneralCommand<K, ?> cmdCart;
		while ((cmdCart = mQueue.poll()) != null) {
			var key = cmdCart.getKey();
			if(key != null) {
				processManagementCommand(cmdCart);
			} else {
				final var label = cmdCart.getLabel();
				final var value = cmdCart.getValue();
				final var expTime = cmdCart.getExpirationTime();
				final var cmdFuture = cmdCart.getFuture();
				if(label == CommandLabel.SUSPEND) {
					suspend();
					cmdFuture.complete(true);
					return;
				}
				List<GeneralCommand> commands = collector
						.keySet()
						.stream()
						.filter(cmdCart.getFilter())
						.map(k -> new GeneralCommand(k, value, label, expTime)).toList();
				
				commands.forEach(nextCommandCart->{
					try {
						processManagementCommand(nextCommandCart);
					} catch(Exception e) {
						RuntimeException ex = new ConveyorRuntimeException("Failed milti-key command "+label+"("+nextCommandCart.getKey()+")",e);
						cmdFuture.completeExceptionally(ex);
						throw ex;
					}
				});
				cmdFuture.complete(true);
			}
			this.lastProcessingTime = System.currentTimeMillis();
		}
	}

	/**
	 * Process management command.
	 *
	 * @param cmdCart the cmd cart
	 */
	private void processManagementCommand(GeneralCommand<K, ?> cmdCart) {
		commandCounter++;
		LOG.debug("processing command {}", cmdCart);
		var l = cmdCart.getLabel();
		try {
			l.get().accept(this, cmdCart);
		} catch (Exception e) {
			cmdCart.getFuture().completeExceptionally(e);
			throw e;
		}
	}

	/**
	 * Drain queues.
	 */
	protected void drainQueues() {
		Cart<K, ?, L> cart;
		while ((cart = inQueue.poll()) != null) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(this, cart.getKey(), cart, "Draining inQueue",
					null, 
					FailureType.CONVEYOR_STOPPED,cart.getAllProperties(), null));
		}
		delayProvider.clear();
		collector.forEach((k, bs) -> {
			bs.setStatus(Status.CANCELED);
			scrapConsumer.accept(new ScrapBin(this, k, bs, "Draining collector", null, FailureType.CONVEYOR_STOPPED,bs.getProperties(), bs.getAcknowledge()));
			bs.cancelFutures();
		});
		collector.clear();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public PartLoader<K,L> part() {
		return new PartLoader<>(pl -> {
			PartLoader<K, L> partLoader;
			if (existingBuildsFirst && collector.containsKey(pl.key)) {
				partLoader = pl.increasePriority();
			} else {
				partLoader = pl;
			}
			Cart<K, ?, L> cart;
			if (partLoader.filter != null) {
				cart = new MultiKeyCart<>(partLoader.filter, partLoader.partValue, partLoader.label, partLoader.creationTime, partLoader.expirationTime, partLoader.priority);
			} else {
				cart = new ShoppingCart<>(partLoader.key, partLoader.partValue, partLoader.label, partLoader.creationTime, partLoader.expirationTime, partLoader.priority);
			}
			partLoader.getAllProperties().forEach(cart::addProperty);
			return place(cart);
		});
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#staticPart()
	 */
	@Override
	public StaticPartLoader<L> staticPart() {
		return new StaticPartLoader<>(cl -> {
			var properties = new HashMap<String, Object>();
			properties.put("CREATE", cl.create);
			var staticPart = new ShoppingCart<K, Object, L>(null, cl.staticPartValue, cl.label, System.currentTimeMillis(), 0, properties, STATIC_PART, cl.priority);
			return place(staticPart);
		});
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT> build() {
		return new BuilderLoader<>(cl -> {
			var bs = cl.value;
			if (bs == null) {
				bs = builderSupplier;
			}
			final var cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime, cl.priority);
			cl.getAllProperties().forEach(cart::addProperty);
			return place(cart);
		},
				cl -> {
					var bs = cl.value;
					if (bs == null) {
						bs = builderSupplier;
					}
					var future = new CompletableFuture<OUT>();
					if (bs == null) {
						bs = builderSupplier.withFuture(future);
					} else {
						bs = bs.withFuture(future);
					}
					var cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime, cl.priority);
					var supplier = (FutureSupplier) cart.getValue();
					var cartFuture = place(cart);
					if (cartFuture.isCancelled()) {
						supplier.getFuture().cancel(true);
					}
					return supplier.getFuture();
				}
		);
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#future()
	 */
	@Override
	public FutureLoader<K, OUT> future() {
		return new FutureLoader<>(cl -> {
			var future = new CompletableFuture<OUT>();
			final var cart = new FutureCart<K, OUT, L>(cl.key, future, cl.creationTime, cl.expirationTime, cl.priority);
			cl.getAllProperties().forEach(cart::addProperty);
			var cartFuture = this.place(cart);
			if (cartFuture.isCancelled()) {
				future.cancel(true);
			}
			return future;
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> cart) {
		try {
			var future = cart.getFuture();
			commandBeforePlacementValidator.accept(cart);
			var r = mQueue.add(cart);
			if (!r) {
				future.cancel(true);
			}
			return future;
		} catch (RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(this, cart.getKey(), cart, e.getMessage(), e, FailureType.COMMAND_REJECTED,cart.getAllProperties(), null));
			throw e;
		} finally {
			lock.tell();
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#command()
	 */
	@Override
	public CommandLoader<K, OUT> command() {
		return new CommandLoader<>(this::command);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer()
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return new ResultConsumerLoader<>(rcl->{
			final Cart<K,?,L> cart;
			if(rcl.key != null) {
				cart = new ResultConsumerCart<>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime, rcl.priority);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, LoadType.RESULT_CONSUMER,rcl.priority);
			}
			rcl.getAllProperties().forEach(cart::addProperty);
			return this.place(cart);
		}, 
		rc->{
			this.resultConsumer = rc;
			if(rc instanceof ForwardingConsumer) {
				this.forwardingResults = true;
				this.forwardingTo.add(((ForwardingConsumer)rc).getToConvName());
			}
		}, 
		resultConsumer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer(com.aegisql.conveyor.consumers.result.ResultConsumer)
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K,OUT> consumer) {
		return this.resultConsumer().first(consumer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer()
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {
		return new ScrapConsumerLoader<>(sc -> this.scrapConsumer = sc, this.scrapConsumer);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer(com.aegisql.conveyor.consumers.scrap.ScrapConsumer)
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K,?> scrapConsumer) {
		return scrapConsumer().first(scrapConsumer);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		var future = cart.getFuture();
		try {
			cartBeforePlacementValidator.accept(cart);
			if ( ! inQueue.add(cart) ) {
				future.cancel(true);
			}
		} catch (RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(this, cart.getKey(), cart, e.getMessage(), e, FailureType.CART_REJECTED,cart.getAllProperties(), null));
		} finally {
			lock.tell();
		}
		return future;
	}

	/**
	 * Gets the collector size.
	 *
	 * @return the collector size
	 */
	public int getCollectorSize() {
		return collector.size();
	}

	/**
	 * Gets the input queue size.
	 *
	 * @return the input queue size
	 */
	public int getInputQueueSize() {
		return inQueue.size();
	}

	/**
	 * Gets the delayed queue size.
	 *
	 * @return the delayed queue size
	 */
	public int getDelayedQueueSize() {
		return delayProvider.delayedSize();
	}

	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer
	 *            the scrap consumer
	 */
	protected void setInnerScrapConsumer(ScrapConsumer<K,?> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	/**
	 * Stop.
	 */
	public void stop() {
		running = false;
		if(this.conveyorFuture != null) {
			this.conveyorFuture.complete(false);
		}
		lock.tell();
		statusLine = "Stopped";
		LOG.info("Conveyor {} has stopped!",name);
	}

	protected void shutDownOnIdleTimeout() {
		stop();
	}

	/**
	 * Complete tasks and stop.
	 *
	 * @return the completable future
	 */
	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		LOG.info("Conveyor {} is about to stop",name);
		if(this.conveyorFuture == null) {
			synchronized (this.conveyorFutureLock) {
				if(this.conveyorFuture == null) {
					this.addCartBeforePlacementValidator(c->{
						var key = c.getKey();
						if(key != null && ! this.collector.containsKey(key)) {
							throw new IllegalStateException("Conveyor preparing to shut down. No new messages can be accepted");
						}
					});
					this.commandBeforePlacementValidator = commandBeforePlacementValidator.andThen(cmd -> {
						var key = cmd.getKey();
						CommandLabel cmdLabel = cmd.getLabel();
						switch(cmdLabel) {
							case CANCEL_BUILD:
							case COMPLETE_BUILD:
							case COMPLETE_BUILD_EXEPTIONALLY:
							case TIMEOUT_BUILD:
							case PEEK_BUILD:
							case PEEK_KEY:
								break;
							default:
								if(key != null && ! this.collector.containsKey(key)) {
									throw new IllegalStateException("Conveyor preparing to shut down. No new commands can be accepted. Rejected command "+cmd.getLabel()+" for key "+cmd.getKey());
								}
						}
					});
					this.conveyorFuture = new CompletableFuture<>();
				}
			}
		}
		statusLine = "Stopping...";
		lock.tell();
		return this.conveyorFuture;
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionIdleInterval() {
		return lock.expirationCollectionInterval;
	}

	/**
	 * Gets the expiration collection idle time unit.
	 *
	 * @return the expiration collection idle time unit
	 */
	public TimeUnit getExpirationCollectionIdleTimeUnit() {
		return lock.expirationCollectionUnit;
	}

	/**
	 * Process site.
	 *
	 * @param cart
	 *            the cart
	 * @param accept
	 *            the accept
	 */
	@SuppressWarnings("unchecked")
	private void processSite(Cart<K, ?, L> cart, boolean accept) {
		var key = cart.getKey();
		if (key == null) {
			if (cart.getLoadType() == MULTI_KEY_PART) {
				try {
					MultiKeyCart<K,?,L> mc = ((MultiKeyCart<K,?,L>)cart);
					final var label = cart.getLabel();
					final var load = mc.getValue();
					final var filter = load.getFilter();
					final Function<K,Cart<K, ?, L>> cartBuilder;
					if (load.getLoadType() == RESULT_CONSUMER) {
						cartBuilder = k -> {
							ResultConsumerCart rcc = new ResultConsumerCart(k, (ResultConsumer) load.getValue(), cart.getCreationTime(), cart.getExpirationTime(), cart.getPriority());
							rcc.putAllProperties(cart.getAllProperties());
							return rcc;
						};
					} else {
						cartBuilder = k -> {
							ShoppingCart<K, ?, L> c = new ShoppingCart<>(k, load.getValue(), label, cart.getCreationTime(), cart.getExpirationTime());
							c.putAllProperties(mc.getAllProperties());
							return c;
						};
					}
					LOG.trace("READY TO APPLY MULTI");
					collector.keySet().stream().filter(filter).toList()
					.forEach(k -> {
								LOG.trace("MULTI FILTER MATCH {}",k);
								processSite(cartBuilder.apply(k), accept);
							});
					cart.getFuture().complete(true);
				} catch (Exception e) {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(this, cart.getLabel(), cart, "MultiKey cart failure", e, FailureType.GENERAL_FAILURE,cart.getAllProperties(), null));
					throw e;
				}
			} else if(cart.getLoadType() == STATIC_PART) {
				if( cart.getProperty("CREATE", Boolean.class) ) {
					staticValues.put(cart.getLabel(), cart);
				} else {
					staticValues.remove(cart.getLabel());
				}
				cart.getFuture().complete(true);
			}
			if( ! (cart.getLoadType() == PART && cart.getValue() instanceof Cart)) {
				return;
			}
		}
		currentSite = null;
		CompletableFuture resultFuture = null;
		ResultConsumer newConsumer     = null;
		if (cart.getLoadType() == FUTURE) {
			var fc = (FutureCart<K, ? extends OUT, L>) cart;
			resultFuture = fc.getValue();
		}
		if (cart.getLoadType() == RESULT_CONSUMER) {
			var rc = (ResultConsumerCart<K,OUT, L>) cart;
			newConsumer = rc.getValue();
		}
		var failureType = FailureType.GENERAL_FAILURE;
		try {
			LOG.trace("Read {}",cart);
			currentSite = getBuildingSite(cart);
			if (currentSite == null) {
				if (cart.getLoadType() == BUILDER) {
					cart.getFuture().complete(Boolean.TRUE);
				} else {
					cart.getFuture().complete(Boolean.FALSE);
				}
				if (resultFuture != null) {
					resultFuture.completeExceptionally(new Exception("No active building site found"));
				}
				return;
			}
			currentSite.siteRunning(true);
			currentSite.addProperties(cart.getAllProperties());
			if (resultFuture != null) {
				currentSite.addFuture(resultFuture);
				cart.getFuture().complete(true);
				return;
			}
			if (newConsumer != null) {
				currentSite.setResultConsumer(newConsumer);
				cart.getFuture().complete(true);
				return;
			}
			if (Status.TIMED_OUT.equals(cart.getValue())) {
				failureType = FailureType.ON_TIMEOUT_FAILED;
				currentSite.timeout(cart);
			} else if (accept) {
				failureType = FailureType.DATA_REJECTED;
				currentSite.accept(cart);
			}
			failureType = FailureType.READY_FAILED;
			if (currentSite.ready()) {
				failureType = FailureType.BUILD_FAILED;
				OUT res = currentSite.unsafeBuild();
				failureType = FailureType.RESULT_CONSUMER_FAILED;
				completeSuccessfully((BuildingSite<K, L, ?, OUT>) currentSite,res,Status.READY);
				failureType = FailureType.BEFORE_EVICTION_FAILED;
				keyBeforeEviction.accept(new AcknowledgeStatus<>(key, Status.READY, currentSite.getProperties()));
			}
			cart.getFuture().complete(Boolean.TRUE);
		} catch (KeepRunningConveyorException e) {
			if (currentSite != null) {
				handleErrorAndContinue(currentSite, cart, e);
			}
		} catch (Exception e) {
			if (currentSite != null) {
				handleError(currentSite, cart, e, failureType);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
						new ScrapBin(this, cart.getKey(), cart, "Cart Processor Failed", e, failureType,cart.getAllProperties(), null));
			}
			if (!failureType.equals(FailureType.BEFORE_EVICTION_FAILED)) {
				try {
					keyBeforeEviction.accept(new AcknowledgeStatus<>(key, Status.INVALID, currentSite==null?null:currentSite.getProperties()));
				} catch (Exception e2) {
					LOG.error("BeforeEviction failed after processing failure: {} {} {}", failureType, e.getMessage(),
							e2.getMessage());
					collector.remove(key);
				}
			}
		} finally {
			if(currentSite != null) {
				currentSite.siteRunning(false);
			}
			this.lastProcessingTime = System.currentTimeMillis();
		}
	}

	private void handleError( BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> currentSite, Cart<K, ?, L> cart, Exception e, FailureType failureType) {
		Map<String,Object> properties = cart.getAllProperties();
		properties.putAll(currentSite.getProperties());
		currentSite.setStatus(Status.INVALID);
		currentSite.setLastError(e);
		currentSite.setLastCart(cart);
		cart.getScrapConsumer().accept(new ScrapBin(this, cart.getKey(), cart,
				"Cart Processor failed", e, failureType, properties, null));
		scrapConsumer.accept(new ScrapBin(this, cart.getKey(), currentSite,
				"Site Processor failed", e, failureType, properties, currentSite.getAcknowledge()));
		currentSite.completeFuturesExceptionaly(e);
	}

	private void handleErrorAndContinue(BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> currentSite, Cart<K, ?, L> cart, KeepRunningConveyorException e) {
		Map<String,Object> properties = cart.getAllProperties();
		properties.putAll(currentSite.getProperties());
		currentSite.setLastError(e);
		currentSite.setLastCart(cart);
		cart.getScrapConsumer().accept(new ScrapBin(this, cart.getKey(), cart,
				"Cart Processor failed. Keep running", e, FailureType.KEEP_RUNNING_EXCEPTION, properties, null));
		scrapConsumer.accept(new ScrapBin(this, cart.getKey(), cart,
				"Site Processor failed. Keep running", e, FailureType.KEEP_RUNNING_EXCEPTION, properties, currentSite.getAcknowledge()));
		currentSite.completeFuturesExceptionaly(e);
	}

	/**
	 * Postpone timeout.
	 *
	 * @param bs
	 *            the bs
	 * @return true, if successful
	 */
	private boolean postponeTimeout(BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> bs) {
		if (postponeExpirationEnabled) {
			if (!bs.expired()) {
				LOG.trace("Expiration will bin postponed for key={}", bs.getKey());
				delayProvider.getBox(bs.getExpirationTime()).add(bs.getKey());
				return true;
			}
		}
		bs.setStatus(Status.TIMED_OUT);
		return false;
	}

	/**
	 * Removes the expired.
	 */
	private void removeExpired() {
		int cnt = 0;
		var statusForEviction = Status.TIMED_OUT;
		for (K key : delayProvider.getAllExpiredKeys()) {
			var buildingSite = collector.get(key);
			if (buildingSite == null) {
				continue;
			}

			if (timeoutAction != null || buildingSite.getTimeoutAction() != null) {
				try {
					var to = new ShoppingCart<K, Object, L>(buildingSite.getKey(),
							Status.TIMED_OUT, null);
					buildingSite.timeout(to);

					if (buildingSite.ready()) {
						LOG.trace("Expired and finished {}",key);
						var res = buildingSite.build();
						completeSuccessfully((BuildingSite<K, L, ?, OUT>) buildingSite,res,Status.TIMED_OUT);
						statusForEviction = Status.READY;
					} else {
						if (postponeTimeout(buildingSite)) {
							continue;
						}
						LOG.trace("Expired and not finished {}",key);
						scrapConsumer.accept(new ScrapBin(this, key,
								buildingSite, "Site expired", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
						buildingSite.cancelFutures();
					}
				} catch (Exception e) {
					buildingSite.setStatus(Status.INVALID);
					buildingSite.setLastError(e);
					statusForEviction = Status.INVALID;
					scrapConsumer.accept(new ScrapBin(this, key,
							buildingSite, "Timeout processor failed ", e, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
					buildingSite.completeFuturesExceptionaly(e);
				}
			} else {
				if (postponeTimeout(buildingSite)) {
					continue;
				}
				LOG.trace("Expired and removed {}",key);
				scrapConsumer.accept(new ScrapBin(this, key,
						buildingSite, "Site expired. No timeout action", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
				buildingSite.cancelFutures();
			}
			keyBeforeEviction.accept(new AcknowledgeStatus<>(key, statusForEviction, buildingSite.getProperties()));
			cnt++;
			this.lastProcessingTime = System.currentTimeMillis();
		}
		if (cnt > 0) {
			LOG.trace("Timeout collected: {}",cnt);
		}
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval
	 *            the expiration collection interval
	 * @param unit
	 *            the unit
	 */
	public void setIdleHeartBeat(long expirationCollectionInterval, TimeUnit unit) {
		lock.setExpirationCollectionInterval(expirationCollectionInterval);
		lock.setExpirationCollectionUnit(unit);
		lock.tell();
	}

	/**
	 * Gets the builder timeout.
	 *
	 * @return the builder timeout
	 */
	public long getDefaultBuilderTimeout() {
		return builderTimeout;
	}

	/**
	 * Sets the builder timeout.
	 *
	 * @param builderTimeout
	 *            the builder timeout
	 * @param unit
	 *            the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setDefaultBuilderTimeout(java.time.Duration)
	 */
	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		this.setDefaultBuilderTimeout(duration.toMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout
	 *            the timeout
	 * @param unit
	 *            the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.startTimeReject = unit.toMillis(timeout);
	}

	/**
	 * Checks if is on timeout action.
	 *
	 * @return true, if is on timeout action
	 */
	public boolean isOnTimeoutAction() {
		return timeoutAction != null;
	}

	/**
	 * Sets the on timeout action.
	 *
	 * @param timeoutAction
	 *            the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		this.timeoutAction = timeoutAction;
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param <B> the generic type
	 * @param cartConsumer            the cart consumer
	 */
	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		this.cartConsumer = (l,c,b)-> ((LabeledValueConsumer)cartConsumer).accept(l, getPayload(c), b);
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness
	 *            the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> readiness) {
		this.readiness = readiness;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness
	 *            the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		this.readiness = (status, builder) -> readiness.test(builder);
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier
	 *            the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		this.builderSupplier = builderSupplier;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		this.innerThread.setName(name);
		try {
			//unregister old name
			Conveyor.unRegister(oldName);
		} catch (Exception e) {
			//Ignore. Might be already unregistered
		}
		this.setMbean(this.name);
	}

	/*
	 * STATIC METHODS TO SUPPORT MANAGEMENT COMMANDS
	 * 
	 */

	/**
	 * Creates the now.
	 *
	 * @param conveyor
	 *            the conveyor
	 * @param cart
	 *            the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void createNow(AssemblingConveyor conveyor, Cart cart) {
		conveyor.getBuildingSite(cart);
		cart.getFuture().complete(true);
	}

	/**
	 * Cancel now.
	 *
	 * @param <K> the key type
	 * @param conveyor            the conveyor
	 * @param cart            the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static <K> void cancelNow(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		var key = cart.getKey();
		var bs = (BuildingSite) conveyor.collector.get(key);
		var properties = bs == null ? new HashMap<String,Object>():bs.getProperties();
		conveyor.keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.CANCELED, properties));
		if(bs != null) {
			bs.cancelFutures();
			bs.setStatus(Status.CANCELED);
		}
		cart.getFuture().complete(true);
	}

	static <K,OUT> void complete(AssemblingConveyor conveyor, Cart<K, OUT, ?> cart) {
		var key = cart.getKey();
		var result = cart.getValue();
		var bs = (BuildingSite) conveyor.collector.get(key);
		var properties = bs == null ? new HashMap<String,Object>():bs.getProperties();
		if(bs != null) {
			conveyor.keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.READY, properties));
			conveyor.completeSuccessfully(bs,result,Status.READY);
			cart.getFuture().complete(true);
		} else {
			cart.getFuture().complete(false);
		}
	}

	static <K> void completeExceptionally(AssemblingConveyor conveyor, Cart<K, Throwable, ?> cart) {
		var key = cart.getKey();
		Throwable error = cart.getValue();
		if(error != null) {
			var bs = (BuildingSite) conveyor.collector.get(key);
			var properties = bs == null ? new HashMap<String, Object>() : bs.getProperties();
			if (bs != null) {
				if (error instanceof KeepRunningConveyorException) {
					conveyor.handleErrorAndContinue(bs, cart, (KeepRunningConveyorException) error);
				} else {
					conveyor.keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.CANCELED, properties));
					conveyor.handleError(bs, cart, (Exception) error, FailureType.EXTERNAL_FAILURE);
				}
			}
			cart.getFuture().complete(true);
		} else {
			LOG.debug("Expected exception. Command ignored for key {}.",key);
			cart.getFuture().complete(false);
		}
	}

	static <K> void addProperties(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		var key = cart.getKey();
		var bs = (BuildingSite) conveyor.collector.get(key);
		if(bs != null) {
			bs.addProperties(cart.getAllProperties());
			cart.getFuture().complete(true);
		} else {
			cart.getFuture().complete(false);
		}
	}


	/**
	 * Cancel now.
	 *
	 * @param <K> the key type
	 * @param conveyor            the conveyor
	 * @param cart            the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static <K> void rescheduleNow(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		var key = cart.getKey();
		var newExpirationTime = cart.getExpirationTime();
		conveyor.keyBeforeReschedule.accept(key, newExpirationTime);
		cart.getFuture().complete(true);
	}

	/**
	 * Timeout now.
	 *
	 * @param <K> the key type
	 * @param conveyor            the conveyor
	 * @param cart            the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static <K> void timeoutNow(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		var key = cart.getKey();
		conveyor.collector.get(key);
		conveyor.keyBeforeReschedule.accept(key, System.currentTimeMillis());
		cart.getFuture().complete(true);
	}

	/**
	 * Check build.
	 *
	 * @param <K> the key type
	 * @param conveyor            the conveyor
	 * @param cart            the cart
	 */
	static <K> void checkBuild(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		var key = cart.getKey();
		if (conveyor.collector.containsKey(key)) {
			conveyor.processSite(cart, false);
			cart.getFuture().complete(true);
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring check command.", key);
			cart.getFuture().complete(false);
		}
	}

	/**
	 * Peek build.
	 *
	 * @param <K> the key type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static <K,OUT> void peekBuild(AssemblingConveyor<K,?,OUT> conveyor, Cart<K, Consumer<ProductBin<K, OUT>>, ?> cart) {
		var key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			var bs = conveyor.collector.get(key);
			try {
				var prod = bs.unsafeBuild();
				var bin = new ProductBin(conveyor, key, prod, bs.getExpirationTime(), bs.getStatus(), bs.getProperties(), null);
				cart.getValue().accept(bin);
				cart.getFuture().complete(true);
			} catch (Exception e) {
				var prop = bs.getProperties();
				prop.put("ERROR", e);
				var bin = new ProductBin(conveyor, key, null, bs.getExpirationTime(), Status.INVALID, bs.getProperties(), null);
				cart.getValue().accept(bin);
				cart.getFuture().complete(false);
			}
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring peek command.", key);
			var bin = new ProductBin(conveyor, key, null, 0, Status.NOT_FOUND, null, null);
			cart.getValue().accept(bin);
			cart.getFuture().complete(false);
		}
	}

	static <K,OUT> void peekKey(AssemblingConveyor<K,?,OUT> conveyor, Cart<K, Consumer<K>, ?> cart) {
		var key = cart.getKey();
		if (conveyor.collector.containsKey(key)) {
			try {
				cart.getValue().accept(key);
				cart.getFuture().complete(true);
			} catch (Exception e) {
				cart.getFuture().completeExceptionally(e);
			}
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring peek key command.", key);
			cart.getFuture().complete(false);
		}
	}

	/**
	 * Memento build.
	 *
	 * @param <K> the key type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static <K,OUT> void mementoBuild(AssemblingConveyor<K,?,OUT> conveyor, Cart<K, Consumer<Memento>, ?> cart) {
		var key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			var bs = conveyor.collector.get(key);
			var memento = bs.getMemento();
			cart.getValue().accept(memento);
			cart.getFuture().complete(true);
		} else {
			LOG.debug("Key '{}' does not exist. Empty memento", key);
			cart.getValue().accept(null);
			cart.getFuture().complete(false);
		}
	}

	/**
	 * Restore build.
	 *
	 * @param <K> the key type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static <K,OUT> void restoreBuild(AssemblingConveyor<K,?,OUT> conveyor, Cart<K, Memento, ?> cart) {
		var key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			var bs = conveyor.collector.get(key);
			var memento = cart.getValue();
			bs.restore(memento);
			cart.getFuture().complete(true);
		} else {
			LOG.debug("Key '{}' does not exist. Creating new from Memento", key);
			createNow(conveyor, cart);
		}
	}

	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Checks if is synchronize builder.
	 *
	 * @return true, if is synchronize builder
	 */
	public boolean isSynchronizeBuilder() {
		return synchronizeBuilder;
	}

	/**
	 * Checks if is keep carts on site.
	 *
	 * @return true, if is keep carts on site
	 */
	public boolean isKeepCartsOnSite() {
		return saveCarts;
	}

	/**
	 * Sets the keep carts on site.
	 *
	 * @param saveCarts
	 *            the new keep carts on site
	 */
	public void setKeepCartsOnSite(boolean saveCarts) {
		this.saveCarts = saveCarts;
	}

	/**
	 * Sets the synchronize builder.
	 *
	 * @param synchronizeBuilder
	 *            the new synchronize builder
	 */
	public void setSynchronizeBuilder(boolean synchronizeBuilder) {
		this.synchronizeBuilder = synchronizeBuilder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#addCartBeforePlacementValidator(java.util.
	 * function.Consumer)
	 */
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if (cartBeforePlacementValidator != null) {
			this.cartBeforePlacementValidator = this.cartBeforePlacementValidator.andThen(cartBeforePlacementValidator);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyEvictionAction(java.util.
	 * function.Consumer)
	 */
	public void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction) {
		if (keyBeforeEviction != null) {
			this.keyBeforeEviction = keyBeforeEviction.andThen(this.keyBeforeEviction);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#addBeforeKeyReschedulingAction(java.util.
	 * function.BiConsumer)
	 */
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		if (keyBeforeRescheduling != null) {
			this.keyBeforeReschedule = keyBeforeRescheduling.andThen(this.keyBeforeReschedule);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getExpirationTime(java.lang.Object)
	 */
	public long getExpirationTime(K key) {
		var bs = collector.get(key);
		if (bs == null) {
			return -1;
		} else {
			return bs.expireableSource.getExpirationTime();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#acceptLabels(java.lang.Object[])
	 */
	public void acceptLabels(L... labels) {
		if (labels != null && labels.length > 0) {
			acceptedLabels.addAll(Arrays.asList(labels));
			this.addCartBeforePlacementValidator(cart -> {
				if (!acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException(
							"Conveyor '" + this.name + "' cannot process label '" + cart.getLabel() + "'");
				}
			});

			lBalanced = true;

		}
	}

	/**
	 * Creates a close copy of current conveyor set readiness evaluator and
	 * result consumer to throw an exception acceptedLabels are not copied.
	 *
	 * @return the assembling conveyor
	 */
	public AssemblingConveyor<K, L, OUT> detach() {
		var c = new AssemblingConveyor<K,L,OUT>();
		c.setBuilderSupplier(builderSupplier);
		c.setDefaultBuilderTimeout(builderTimeout, TimeUnit.MILLISECONDS);
		c.setIdleHeartBeat(getExpirationCollectionIdleInterval(), getExpirationCollectionIdleTimeUnit());
		c.setName("copy of " + name);
		c.setInnerScrapConsumer(scrapConsumer);
		c.setReadinessEvaluator(b -> {
			throw new IllegalStateException("Readiness evaluator is not set for copy of conveyor '" + name + "'");
		});
		c.setDefaultCartConsumer(cartConsumer);
		c.setKeepCartsOnSite(saveCarts);
		c.setOnTimeoutAction(timeoutAction);
		c.setSynchronizeBuilder(synchronizeBuilder);
		c.startTimeReject = this.startTimeReject;
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#isLBalanced()
	 */
	public boolean isLBalanced() {
		return lBalanced;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#getAcceptedLabels()
	 */
	public Set<L> getAcceptedLabels() {
		return acceptedLabels;
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getGenericName()+" [name=" + name + ", thread=" + innerThread.threadId() + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpiration(boolean)
	 */
	@Override
	public void enablePostponeExpiration(boolean flag) {
		this.postponeExpirationEnabled = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#enablePostponeExpirationOnTimeout(boolean)
	 */
	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		this.postponeExpirationOnTimeoutEnabled = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		this.postponeExpirationMills = unit.toMillis(time);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aegisql.conveyor.Conveyor#isForwardingResults()
	 */
	@Override
	public boolean isForwardingResults() {
		return forwardingResults;
	}

	/**
	 * Gets the cart counter.
	 *
	 * @return the cart counter
	 */
	@Override
	public long getCartCounter() {
		return cartCounter;
	}

	/**
	 * Gets the command counter.
	 *
	 * @return the command counter
	 */
	public long getCommandCounter() {
		return commandCounter;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setIdleHeartBeat(java.time.Duration)
	 */
	@Override
	public void setIdleHeartBeat(Duration duration) {
		this.setIdleHeartBeat(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#rejectUnexpireableCartsOlderThan(java.time.Duration)
	 */
	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		this.rejectUnexpireableCartsOlderThan(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(java.time.Duration)
	 */
	@Override
	public void setExpirationPostponeTime(Duration duration) {
		this.setExpirationPostponeTime(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	/**
	 * Complete successfully.
	 *
	 * @param buildingSite the building site
	 * @param res the res
	 * @param status the status
	 */
	private void completeSuccessfully(BuildingSite<K, L, ?, OUT> buildingSite, OUT res, Status status) {
		buildingSite.completeWithValue(res,status);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setAutoAcknowledge(boolean)
	 */
	@Override
	public void setAutoAcknowledge(boolean auto) {
		this.autoAck = auto;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setAcknowledgeAction(java.util.function.Consumer)
	 */
	@Override
	public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
		this.ackAction = ackAction;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#autoAcknowledgeOnStatus(com.aegisql.conveyor.Status, com.aegisql.conveyor.Status[])
	 */
	@Override
	public void autoAcknowledgeOnStatus(Status first, Status... other) {
		ackStatusSet = EnumSet.of(first, other);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getResultConsumer()
	 */
	@Override
	public ResultConsumer<K, OUT> getResultConsumer() {
		return resultConsumer;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#interrupt(java.lang.String)
	 */
	@Override
	public void interrupt(String conveyorName) {
		if(name.equals(conveyorName)) {
			BuildingSite bs = currentSite;
			if(bs != null) {
				bs.interrupt(innerThread);
                LOG.info("interrupted {}", conveyorName);
			} else {
                LOG.warn("No active build found for {}", conveyorName);
			}
		} else {
            LOG.warn("{} ignored interruption for {}", name, conveyorName);
		}
	}

	@Override
	public void interrupt(String conveyorName, K key) {
		if(name.equals(conveyorName)) {
			BuildingSite bs = currentSite; // not thread safe
			if(bs != null) {
				bs.interrupt(innerThread,key);
                LOG.info("interrupted {} for {}", conveyorName, key);
			} else {
                LOG.warn("No active build found for {}", conveyorName);
			}
		} else {
            LOG.warn("{} ignored interruption for {}", name, conveyorName);
		}
	}
	
	/**
	 * Gets the payload.
	 *
	 * @param cart the cart
	 * @return the payload
	 */
	protected Object getPayload(Cart<K,?,L> cart) {
		return payloadFunction.apply(cart);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setCartPayloadAccessor(java.util.function.Function)
	 */
	@Override
	public void setCartPayloadAccessor(Function<Cart<K, ?, L>, Object> payloadFunction) {
		this.payloadFunction = payloadFunction;
	}

	@Override
	public void suspend() {
		this.statusLine = "Suspended";
		this.suspended = true;
	}

	@Override
	public void resume() {
		this.statusLine = "Accepting Parts";
		this.suspended = false;
		lock.tell();
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public Class<?> mBeanInterface() {
		return AssemblingConveyorMBean.class;
	}

	@Override
	public ConveyorMetaInfo<K, L, OUT> getMetaInfo() {
		throw new ConveyorRuntimeException("Meta Info is not available for '"+getName()+"'. getMetaInfo() method must be overridden in a child conveyor class.");
	}

	public void setLongInactivityAction(Runnable action, long inactivityTime, TimeUnit unit) {
		setLongInactivityAction(action,Duration.ofMillis(unit.toMillis(inactivityTime)));
	}

	public void setLongInactivityAction(Runnable action, Duration inactivityTime) {
		this.longInactivityAction = action;
		this.longInactivityTime = inactivityTime.toMillis();
	}

}
