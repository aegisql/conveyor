/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.StaticCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.delay.DelayProvider;
import com.aegisql.conveyor.loaders.BuilderLoader;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.loaders.ResultConsumerLoader;
import com.aegisql.conveyor.loaders.ScrapConsumerLoader;
import com.aegisql.conveyor.loaders.StaticPartLoader;

// TODO: Auto-generated Javadoc
/**
 * The Class AssemblingConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 * @param <K>
 *            the key type
 * @param <L>
 *            the generic type
 * @param <OUT>
 *            the generic type
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

	/** Keeps static values*/
	protected final Map<L,Cart<K,?,L>> staticValues = new HashMap<>();
	
	/** The cart counter. */
	protected long cartCounter = 0;

	/** The command counter. */
	protected long commandCounter = 0;

	/** The builder timeout. */
	protected long builderTimeout = 0;

	/** The start time reject. */
	protected long startTimeReject = System.currentTimeMillis();

	/** The timeout action. */
	protected Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The result consumer. */
	protected ResultConsumer <K,OUT> resultConsumer = null;

	/** The scrap logger. */
	protected ScrapConsumer<K,?> scrapLogger = scrapBin->{
		LOG.error("{}",scrapBin);
	};

	/** The scrap consumer. */
	protected ScrapConsumer<K,?> scrapConsumer = scrapLogger;
	
	/** The cart consumer. */
	protected LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer = (l, v, b) -> {
		throw new IllegalStateException("Cart Consumer is not set");
	};

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

	/** The key before eviction. */
	private Consumer<K> keyBeforeEviction = key -> {
		LOG.trace("Key is ready to be evicted {}", key);
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> bs = collector.remove(key);
	};

	/** The key before reschedule. */
	private BiConsumer<K, Long> keyBeforeReschedule = (key, newExpirationTime) -> {
		Objects.requireNonNull(key, "NULL key cannot be rescheduld");
		Objects.requireNonNull(newExpirationTime, "NULL newExpirationTime cannot be applied to the schedile");
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> buildingSite = collector.get(key);
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

	/** The synchronize builder. */
	protected boolean synchronizeBuilder = false;

	/** The accepted labels. */
	protected final Set<L> acceptedLabels = new HashSet<>();

	protected CompletableFuture<Boolean> conveyorFuture = null;
	
	private final Object conveyorFutureLock = new Object();
	
	/** The inner thread. */
	private final Thread innerThread;

	/**
	 * The Class Lock.
	 */
	private static final class Lock {

		/** The r lock. */
		private final ReentrantLock rLock = new ReentrantLock();

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
		public void waitData(Queue<?> q) throws InterruptedException {
			rLock.lock();
			try {
				if (q.isEmpty()) {
					hasCarts.await(expirationCollectionInterval, expirationCollectionUnit);
				}
			} finally {
				rLock.unlock();
			}
		}

	}

	/** The lock. */
	private final Lock lock = new Lock();

	/** The save carts. */
	private boolean saveCarts;

	/** The name. */
	private String name;

	/** The Constant mBeanServer. */
	private final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	/** The l balanced. */
	private boolean lBalanced = false;

	/** The postpone expiration enabled. */
	private boolean postponeExpirationEnabled = false;

	/** The postpone expiration mills. */
	private long postponeExpirationMills = 0;

	/** The forwarding results. */
	private boolean forwardingResults = false;

	/** The object name. */
	protected ObjectName objectName;

	/** The forwarding to. */
	private String forwardingTo = "not forwarding";

	/** The postpone expiration on timeout enabled. */
	private boolean postponeExpirationOnTimeoutEnabled;

	/**
	 * Wait data.
	 *
	 * @return true, if successful
	 */
	private boolean waitData() {
		try {
			lock.waitData(inQueue);
		} catch (InterruptedException e) {
			LOG.error("Interrupted ", e);
			stop();
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
		boolean returnNull = false;
		K key = cart.getKey();
		if (key == null) {
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
					buildingSite = new BuildingSite<K, L, Cart<K, ?, L>, OUT>(cart, bs, cartConsumer, readiness,
							timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
							postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer);
					if (cart.getValue() instanceof FutureSupplier) {
						FutureSupplier fs = (FutureSupplier) cart.getValue();
						buildingSite.addFuture(fs.getFuture());
					}
				} else {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), cart,
							"Ignore cart. Neither creating cart nor default builder supplier available",
							null, 
							FailureType.BUILD_INITIALIZATION_FAILED,cart.getAllProperties()));
				}
				returnNull = true;
			} else if (builderSupplier != null) {
				buildingSite = new BuildingSite<K, L, Cart<K, ?, L>, OUT>(cart, builderSupplier, cartConsumer,
						readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
						postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), cart,
						"Ignore cart. Neither builder nor builder supplier available",
						null, 
						FailureType.BUILD_INITIALIZATION_FAILED,cart.getAllProperties()));
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
	 * Instantiates a new assembling conveyor.
	 */
	public AssemblingConveyor() {
		this(ConcurrentLinkedQueue<Cart<K, ?, L>>::new); // this class does not
															// permit null
															// elements.
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
		this.addCartBeforePlacementValidator(cart -> {
			if (!running) {
				throw new IllegalStateException("Conveyor is not running");
			}
		});
		this.addCartBeforePlacementValidator(cart -> {
			if (cart.expired()) {
				throw new IllegalStateException("Cart has already expired " + cart);
			}
		});
		this.addCartBeforePlacementValidator(cart -> {
			if (cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject)) {
				throw new IllegalStateException("Cart is too old " + cart);
			}
		});

		commandBeforePlacementValidator = commandBeforePlacementValidator.andThen(cmd -> {
			if (!running) {
				throw new IllegalStateException("Conveyor is not running");
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
				while (running || (inQueue.peek() != null) || (mQueue.peek() != null)) {
					if (!waitData())
						break; //When interrupted, which is exceptional behavior, should return right away
					processManagementCommands();
					Cart<K, ?, L> cart = inQueue.poll();
					if (cart != null) {
						cartCounter++;
						processSite(cart, true);
					}
					removeExpired();
					if(this.conveyorFuture != null && (inQueue.peek() == null) && (mQueue.peek() == null) && (collector.size() == 0)) {
						running = false;
						this.conveyorFuture.complete(true);
						LOG.info("No pending messages or commands. Ready to leave {}", Thread.currentThread().getName());
					}
				}
				LOG.info("Leaving {}", Thread.currentThread().getName());
				drainQueues();
			} catch (Throwable e) { // Let it crash, but don't pretend its
									// running
				stop();
				throw e;
			}
		});
		innerThread.setDaemon(false);
		this.name = "AssemblingConveyor " + innerThread.getId();
		innerThread.setName(this.name);
		this.setMbean(this.name);
		innerThread.start();
	}

	/**
	 * Sets the mbean.
	 *
	 * @param name
	 *            the new mbean
	 */
	protected void setMbean(String name) {
		try {
			final AssemblingConveyor<K, L, OUT> thisConv = this;
			Object mbean = new StandardMBean(new AssemblingConveyorMBean() {
				@Override
				public String getName() {
					return thisConv.name;
				}

				@Override
				public long getThreadId() {
					return thisConv.innerThread.getId();
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
				public String getExpirationPosponeTimeMsec() {
					if (thisConv.postponeExpirationEnabled) {
						return "" + thisConv.postponeExpirationMills;
					} else {
						return "not enabled";
					}
				}

				@Override
				public String getForwardingResultsTo() {
					return thisConv.forwardingTo;
				}

				@Override
				public boolean isLBalanced() {
					return thisConv.lBalanced;
				}

				@Override
				public String getAcceptedLabels() {
					if (acceptedLabels.size() == 0 || acceptedLabels.contains(null)) {
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
			}, AssemblingConveyorMBean.class, false);
			ObjectName newObjectName = new ObjectName("com.aegisql.conveyor:type=" + name);
			synchronized (mBeanServer) {
				if (this.objectName == null) {
					this.objectName = newObjectName;
					this.setMbean(name);
				}
				if (mBeanServer.isRegistered(this.objectName)) {
					mBeanServer.unregisterMBean(objectName);
					this.objectName = newObjectName;
					this.setMbean(name);
				} else {
					mBeanServer.registerMBean(mbean, newObjectName);
					this.objectName = newObjectName;
				}
			}
		} catch (Exception e) {
			LOG.error("MBEAN error " + name, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Process management commands.
	 */
	private void processManagementCommands() {
		GeneralCommand<K, ?> cmdCart = null;
		while ((cmdCart = mQueue.poll()) != null) {
			K key = cmdCart.getKey();
			if(key != null) {
				processManagementCommand(cmdCart);
			} else {
				final CommandLabel label = cmdCart.getLabel();
				final Object value = cmdCart.getValue();
				final long expTime = cmdCart.getExpirationTime();
				collector.keySet().stream().filter(cmdCart.getFilter()).forEach(k->{
					GeneralCommand<K,?> nextCommandCart = new GeneralCommand(k,value, label, expTime);
					mQueue.add(nextCommandCart);
				});
				cmdCart.getFuture().complete(true);
			}
		}
	}

	/**
	 * Process management command.
	 *
	 * @param cmdCart the cmd cart
	 */
	private void processManagementCommand(GeneralCommand<K, ?> cmdCart) {
		commandCounter++;
		if (LOG.isDebugEnabled()) {
			LOG.debug("processing command " + cmdCart);
		}
		CommandLabel l = cmdCart.getLabel();
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
		Cart<K, ?, L> cart = null;
		while ((cart = inQueue.poll()) != null) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), cart, "Draining inQueue",
					null, 
					FailureType.CONVEYOR_STOPPED,cart.getAllProperties()));
		}
		delayProvider.clear();
		collector.forEach((k, v) -> {
			scrapConsumer.accept(new ScrapBin(k, v, "Draining collector", null, FailureType.CONVEYOR_STOPPED,v.getProperties()));
			v.cancelFutures();
		});
		collector.clear();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		return new PartLoader<K, L, X, OUT, Boolean>(cl -> {
			
			if(cl.filter != null) {
				return place(new MultiKeyCart<K, Object, L>(cl.filter, cl.partValue, cl.label, cl.creationTime, cl.expirationTime));
			} else {
				return place(new ShoppingCart<K, Object, L>(cl.key, cl.partValue, cl.label,cl.creationTime ,cl.expirationTime));
			}
		});
	}

	@Override
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart() {
		return  new StaticPartLoader<L, X, OUT, Boolean>(cl -> {
			return place(new StaticCart<K, Object, L>(cl.staticPartValue, cl.label, cl.create));
		});
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		return new BuilderLoader<K, OUT, Boolean>(cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			if(bs == null) {
				bs = builderSupplier;
			}
			CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime);
			return place(cart);
		},
		cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			if(bs == null) {
				bs = builderSupplier;
			}
			CompletableFuture<OUT> future = new CompletableFuture<OUT>();
			if (bs == null) {
				bs = builderSupplier.withFuture(future);
			} else {
				bs = bs.withFuture(future);
			}
			CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime);
			FutureSupplier supplier = (FutureSupplier<OUT>) cart.getValue();
			CompletableFuture<Boolean> cartFuture = place(cart);
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
		return new FutureLoader<K, OUT>(cl -> {
			CompletableFuture<OUT> future = new CompletableFuture<OUT>();
			FutureCart<K, OUT, L> cart = new FutureCart<K, OUT, L>(cl.key, future, cl.creationTime, cl.expirationTime);
			CompletableFuture<Boolean> cartFuture = this.place(cart);
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
			CompletableFuture<Boolean> future = cart.getFuture();
			commandBeforePlacementValidator.accept(cart);
			boolean r = mQueue.add(cart);
			if (!r) {
				future.cancel(true);
			}
			return future;
		} catch (RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.COMMAND_REJECTED,cart.getAllProperties()));
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

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return new ResultConsumerLoader<>(rcl->{
			Cart<K,?,L> cart = null;
			if(rcl.key != null) {
				cart = new ResultConsumerCart<K, OUT, L>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, k->{
					return new ResultConsumerCart<K, OUT, L>(k, rcl.consumer, rcl.creationTime, rcl.expirationTime);
				});
			}
			return this.place(cart);
		}, 
		rc->{
			this.resultConsumer = rc;
		}, 
		resultConsumer);
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K,OUT> consumer) {
		return this.resultConsumer().first(consumer);
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {
		return new ScrapConsumerLoader<>(sc -> this.scrapConsumer = sc, this.scrapConsumer);
	}

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
		CompletableFuture<Boolean> future = cart.getFuture();
		try {
			cartBeforePlacementValidator.accept(cart);
			boolean r = inQueue.add(cart);
			if (!r) {
				future.cancel(true);
			}
		} catch (RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.CART_REJECTED,cart.getAllProperties()));
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
	}
	
	/**
	 * Complete tasks and stop.
	 */
	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		if(this.conveyorFuture == null) {
			synchronized (this.conveyorFutureLock) {
				if(this.conveyorFuture == null) {
					this.addCartBeforePlacementValidator(c->{
						K key = c.getKey();
						if(key != null && ! this.collector.containsKey(key)) {
							throw new IllegalStateException("Conveyor preparing to shut down. No new messages can be accepted");
						}
					});
					this.commandBeforePlacementValidator = commandBeforePlacementValidator.andThen(cmd -> {
						K key = cmd.getKey();
						if(key != null && ! this.collector.containsKey(key)) {
							throw new IllegalStateException("Conveyor preparing to shut down. No new commands can be accepted");
						}
					});
					this.conveyorFuture = new CompletableFuture<Boolean>();
				}
			}
		}
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
	private void processSite(Cart<K, ?, L> cart, boolean accept) {
		K key = cart.getKey();
		if (key == null) {
			if (cart instanceof MultiKeyCart) {
				MultiKeyCart<K, ?, L> mCart = (MultiKeyCart<K, ?, L>) cart;
				try {
					
					Function<K,Cart<K, ?, L>> cartBuilder = mCart.cartBuilder();
					
					collector.entrySet().stream().map(entry -> entry.getKey()).filter(mCart::test)
							.collect(Collectors.toList()).forEach(k -> {
								processSite(cartBuilder.apply(k), accept);
							});
					cart.getFuture().complete(true);
				} catch (Exception e) {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getLabel(), cart, "MultiKey cart failure", e, FailureType.GENERAL_FAILURE,cart.getAllProperties()));
					throw e;
				}
			} else if(cart instanceof StaticCart) {
				if(((StaticCart)cart).isCreate()) {
					staticValues.put(cart.getLabel(), cart);
				} else {
					staticValues.remove(cart.getLabel());
				}
				cart.getFuture().complete(true);
			}
			return;
		}
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> buildingSite = null;
		CompletableFuture resultFuture = null;
		ResultConsumer newConsumer     = null;
		if (cart instanceof FutureCart) {
			FutureCart<K, ? extends OUT, L> fc = (FutureCart<K, ? extends OUT, L>) cart;
			resultFuture = fc.getValue();
		}
		if (cart instanceof ResultConsumerCart) {
			ResultConsumerCart<K,OUT, L> rc = (ResultConsumerCart<K,OUT, L>) cart;
			newConsumer = rc.getValue();
		}
		FailureType failureType = FailureType.GENERAL_FAILURE;
		try {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Read " + cart);
			}
			buildingSite = getBuildingSite(cart);
			if (buildingSite == null) {
				if (cart instanceof CreatingCart) {
					cart.getFuture().complete(Boolean.TRUE);
				} else {
					cart.getFuture().complete(Boolean.FALSE);
				}
				if (resultFuture != null) {
					resultFuture.completeExceptionally(new Exception("No active building site found"));
				}
				return;
			}
			buildingSite.addProperties(cart.getAllProperties());
			if (resultFuture != null) {
				buildingSite.addFuture(resultFuture);
				cart.getFuture().complete(true);
				return;
			}
			if (newConsumer != null) {
				buildingSite.setResultConsumer(newConsumer);
				cart.getFuture().complete(true);
				return;
			}
			if (Status.TIMED_OUT.equals(cart.getValue())) {
				failureType = FailureType.ON_TIMEOUT_FAILED;
				buildingSite.timeout((Cart<K, ?, L>) cart);
			} else if (accept) {
				failureType = FailureType.DATA_REJECTED;
				buildingSite.accept((Cart<K, ?, L>) cart);
			}
			failureType = FailureType.READY_FAILED;
			if (buildingSite.ready()) {
				failureType = FailureType.BEFORE_EVICTION_FAILED;
				keyBeforeEviction.accept(key);
				failureType = FailureType.BUILD_FAILED;
				OUT res = buildingSite.unsafeBuild();
				failureType = FailureType.RESULT_CONSUMER_FAILED;
				completeSuccessfully((BuildingSite<K, L, ?, OUT>) buildingSite,res,Status.READY);
			}
			cart.getFuture().complete(Boolean.TRUE);
		} catch (Exception e) {
			if (buildingSite != null) {
				buildingSite.setStatus(Status.INVALID);
				buildingSite.setLastError(e);
				buildingSite.setLastCart(cart);
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), buildingSite,
						"Site Processor failed", e, failureType,cart.getAllProperties()));
				buildingSite.completeFuturesExceptionaly(e);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
						new ScrapBin(cart.getKey(), cart, "Cart Processor Failed", e, failureType,cart.getAllProperties()));
			}
			if (!failureType.equals(FailureType.BEFORE_EVICTION_FAILED)) {
				try {
					keyBeforeEviction.accept(key);
				} catch (Exception e2) {
					LOG.error("BeforeEviction failed after processing failure: {} {} {}", failureType, e.getMessage(),
							e2.getMessage());
					collector.remove(key);
				}
			}
//			cart.getFuture().completeExceptionally(e);
		}
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
				if (LOG.isTraceEnabled()) {
					LOG.trace("Expiration will bin postponed for key={}", bs.getKey());
				}
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
		for (K key : delayProvider.getAllExpiredKeys()) {
			BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> buildingSite = collector.get(key);
			if (buildingSite == null) {
				continue;
			}

			if (timeoutAction != null || buildingSite.getTimeoutAction() != null) {
				try {
					ShoppingCart<K, Object, L> to = new ShoppingCart<K, Object, L>(buildingSite.getKey(),
							Status.TIMED_OUT, null);
					buildingSite.timeout((Cart<K, ?, L>) to);

					if (buildingSite.ready()) {
						if (LOG.isTraceEnabled()) {
							LOG.trace("Expired and finished " + key);
						}
						OUT res = buildingSite.build();
						completeSuccessfully((BuildingSite<K, L, ?, OUT>) buildingSite,res,Status.TIMED_OUT);
					} else {
						if (postponeTimeout(buildingSite)) {
							continue;
						}
						if (LOG.isTraceEnabled()) {
							LOG.trace("Expired and not finished " + key);
						}
						scrapConsumer.accept(new ScrapBin(key,
								buildingSite, "Site expired", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties()));
						buildingSite.cancelFutures();
					}
				} catch (Exception e) {
					buildingSite.setStatus(Status.INVALID);
					buildingSite.setLastError(e);
					scrapConsumer.accept(new ScrapBin(key,
							buildingSite, "Timeout processor failed ", e, FailureType.BUILD_EXPIRED,buildingSite.getProperties()));
					buildingSite.completeFuturesExceptionaly(e);
				}
			} else {
				if (postponeTimeout(buildingSite)) {
					continue;
				}
				if (LOG.isTraceEnabled()) {
					LOG.trace("Expired and removed " + key);
				}
				scrapConsumer.accept(new ScrapBin(key,
						buildingSite, "Site expired. No timeout action", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties()));
				buildingSite.cancelFutures();
			}
			keyBeforeEviction.accept(key);
			cnt++;
		}
		if (cnt > 0) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Timeout collected: " + cnt);
			}
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
	 * @param cartConsumer
	 *            the cart consumer
	 */
	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		this.cartConsumer = (LabeledValueConsumer<L, ?, Supplier<? extends OUT>>) cartConsumer;
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
		this.name = name;
		this.innerThread.setName(name);
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
		K key = cart.getKey();
		BuildingSite bs = (BuildingSite) conveyor.collector.get(key);
		conveyor.keyBeforeEviction.accept(key);
		if(bs != null) {
			bs.cancelFutures();
		}
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
	static <K> void rescheduleNow(AssemblingConveyor conveyor, Cart<K, ?, ?> cart) {
		K key = cart.getKey();
		long newExpirationTime = cart.getExpirationTime();
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
		K key = cart.getKey();
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
		K key = cart.getKey();
		if (conveyor.collector.containsKey(key)) {
			conveyor.processSite(cart, false);
			cart.getFuture().complete(true);
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring check command.", key);
			cart.getFuture().complete(false);
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
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
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
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> bs = collector.get(key);
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
			for (L l : labels) {
				acceptedLabels.add(l);
			}
			this.addCartBeforePlacementValidator(cart -> {
				if (!acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException(
							"Conveyor '" + this.name + "' cannot process label '" + cart.getLabel() + "'");
				}
			});

			lBalanced = true;

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.aegisql.conveyor.Conveyor#forwardPartialResultTo(java.lang.Object,
	 * com.aegisql.conveyor.Conveyor)
	 */
	public <L2,OUT2> void forwardResultTo(Conveyor<K, L2, OUT2> destination, L2 label) {
		forwardResultTo(destination,b->b.key,label);
	}
	
	@Override
	public <K2, L2, OUT2> void forwardResultTo(Conveyor<K2, L2, OUT2> destination, Function<ProductBin<K,OUT>,K2> keyConverter, L2 label) {
		this.forwardingResults = true;
		this.forwardingTo = destination.toString();
		this.resultConsumer().andThen(bin -> {
			LOG.debug("Forward {} from {} to {} {}", label, this.name, destination.getName(), bin);
			PartLoader<K2, L2, OUT, OUT2, Boolean> part = destination.part()
			.id(keyConverter.apply(bin))
			.label(label)
			.value(bin.product)
			.ttl( bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			part.place();
		}).set();
	}

	/**
	 * Creates a close copy of current conveyor set readiness evaluator and
	 * result consumer to throw an exception acceptedLabels are not copied.
	 *
	 * @return the assembling conveyor
	 */
	public AssemblingConveyor<K, L, OUT> detach() {
		AssemblingConveyor<K, L, OUT> c = new AssemblingConveyor<>();
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
		return "AssemblingConveyor [name=" + name + ", thread=" + innerThread.getId() + "]";
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

	@Override
	public void setIdleHeartBeat(Duration duration) {
		this.setIdleHeartBeat(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		this.rejectUnexpireableCartsOlderThan(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	@Override
	public void setExpirationPostponeTime(Duration duration) {
		this.setExpirationPostponeTime(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	private void completeSuccessfully(BuildingSite<K, L, ?, OUT> buildingSite, OUT res, Status status) {
		buildingSite.completeWithValue(res,status);
	}
	
}
