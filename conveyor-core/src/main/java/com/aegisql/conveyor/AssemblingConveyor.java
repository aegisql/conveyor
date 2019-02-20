/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static com.aegisql.conveyor.cart.LoadType.BUILDER;
import static com.aegisql.conveyor.cart.LoadType.FUTURE;
import static com.aegisql.conveyor.cart.LoadType.MULTI_KEY_PART;
import static com.aegisql.conveyor.cart.LoadType.PART;
import static com.aegisql.conveyor.cart.LoadType.RESULT_CONSUMER;
import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.Load;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ForwardResult.ForwardingConsumer;
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
	protected LabeledValueConsumer<L, Cart<K,?,L>, Supplier<? extends OUT>> cartConsumer = (l, v, b) -> {
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The payload function. */
	protected Function<Cart<K,?,L>,Object> payloadFunction = cart->cart.getValue();

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
	
	/** The ack action. */
	protected Consumer<AcknowledgeStatus<K>> ackAction = status->{};

	/** The ack status set. */
	private EnumSet<Status> ackStatusSet = EnumSet.allOf(Status.class);

	/** The key before eviction. */
	private Consumer<AcknowledgeStatus<K>> keyBeforeEviction = status -> {
		LOG.trace("Key is ready to be evicted {} status:{}", status.getKey(), status.getStatus());
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> bs = collector.remove(status.getKey());
		if(autoAck) {
			if(ackStatusSet.contains(status.getStatus())) {
				ackAction.accept(status);
			} else {
				LOG.debug("Auto Acknowledge for key {} not applicable for status {} of {}",status.getKey(),status.getStatus(),ackStatusSet);
			}
	}
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

	protected volatile boolean suspended = false;

	/** The synchronize builder. */
	protected boolean synchronizeBuilder = false;

	/** The accepted labels. */
	protected final Set<L> acceptedLabels = new HashSet<>();

	/** The conveyor future. */
	protected CompletableFuture<Boolean> conveyorFuture = null;
	
	/** The conveyor future lock. */
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
		public void waitData(boolean suspended, Queue<?> q) throws InterruptedException {
			rLock.lock();
			try {
				if (suspended || q.isEmpty()) {
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
	private List<String> forwardingTo = new ArrayList<>();

	/** The postpone expiration on timeout enabled. */
	private boolean postponeExpirationOnTimeoutEnabled;

	/** The current site. */
	private volatile BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT>  currentSite;

	/** The status line. */
	protected String statusLine = "Accepting Parts";

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
					buildingSite = new BuildingSite<K, L, Cart<K, ?, L>, OUT>(cart, bs, cartConsumer, readiness,
							timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
							postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer,
							ackAction);
					if (cart.getValue() instanceof FutureSupplier) {
						FutureSupplier fs = (FutureSupplier) cart.getValue();
						buildingSite.addFuture(fs.getFuture());
					}
				} else {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), cart,
							"Ignore cart. Neither creating cart nor default builder supplier available",
							null, 
							FailureType.BUILD_INITIALIZATION_FAILED,cart.getAllProperties(), null));
				}
				returnNull = true;
			} else if (builderSupplier != null) {
				buildingSite = new BuildingSite<K, L, Cart<K, ?, L>, OUT>(cart, builderSupplier, cartConsumer,
						readiness, timeoutAction, builderTimeout, TimeUnit.MILLISECONDS, synchronizeBuilder, saveCarts,
						postponeExpirationEnabled, postponeExpirationMills, postponeExpirationOnTimeoutEnabled,staticValues,resultConsumer,
						ackAction);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getKey(), cart,
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
				throw new IllegalStateException("Conveyor "+getName()+" is not running");
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
				while (running || (inQueue.peek() != null) || (mQueue.peek() != null)) {
					if (!waitData())
						break; //When interrupted, which is exceptional behavior, should return right away
					processManagementCommands();
					if(suspended) {
						continue;
					}
					Cart<K, ?, L> cart = inQueue.poll();
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

				@Override
				public <K, L, OUT> Conveyor<K, L, OUT> conveyor() {
					return (Conveyor<K, L, OUT>) thisConv;
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
					if (mBeanServer.isRegistered(newObjectName)) {
						LOG.warn("Replacing existing mbean with name {}",newObjectName);
						mBeanServer.unregisterMBean(newObjectName);
					}
					mBeanServer.registerMBean(mbean, newObjectName);
					this.objectName = newObjectName;
				}
			}
		} catch (Exception e) {
			LOG.error("MBEAN error " + name, e);
			throw new ConveyorRuntimeException(e);
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
				final CompletableFuture<Boolean> cmdFuture = cmdCart.getFuture();
				if(label == CommandLabel.SUSPEND) {
					suspend();
					cmdFuture.complete(true);
					return;
				}
				List<GeneralCommand> commands = collector
					.keySet()
					.stream()
					.filter(cmdCart.getFilter())
					.map(k->new GeneralCommand(k,value, label, expTime))
					.collect(Collectors.toList());
				
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
			LOG.debug("processing command {}", cmdCart);
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
					FailureType.CONVEYOR_STOPPED,cart.getAllProperties(), null));
		}
		delayProvider.clear();
		collector.forEach((k, bs) -> {
			bs.setStatus(Status.CANCELED);
			scrapConsumer.accept(new ScrapBin(k, bs, "Draining collector", null, FailureType.CONVEYOR_STOPPED,bs.getProperties(), bs.getAcknowledge()));
			bs.cancelFutures();
		});
		collector.clear();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public PartLoader<K,L> part() {
		return new PartLoader<K, L>(cl -> {
			Cart <K, ?, L> cart;
			if(cl.filter != null) {
				cart = new MultiKeyCart<K, Object, L>(cl.filter, cl.partValue, cl.label, cl.creationTime, cl.expirationTime,cl.priority);
			} else {
				cart = new ShoppingCart<K, Object, L>(cl.key, cl.partValue, cl.label,cl.creationTime ,cl.expirationTime,cl.priority);
			}
			cl.getAllProperties().forEach((k,v)->cart.addProperty(k, v));
			return place(cart);
		});
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#staticPart()
	 */
	@Override
	public StaticPartLoader<L> staticPart() {
		return  new StaticPartLoader<L>(cl -> {
			Map<String,Object> properties = new HashMap<>();
			properties.put("CREATE", cl.create);
			Cart<K,?,L> staticPart = new ShoppingCart<>(null, cl.staticPartValue, cl.label, System.currentTimeMillis(), 0, properties, STATIC_PART,cl.priority);
			return place(staticPart);
		});
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT> build() {
		return new BuilderLoader<K, OUT>(cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			if(bs == null) {
				bs = builderSupplier;
			}
			final CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime,cl.priority);
			cl.getAllProperties().forEach((k,v)->{cart.addProperty(k, v);});
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
			CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime,cl.priority);
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
			final FutureCart<K, OUT, L> cart = new FutureCart<K, OUT, L>(cl.key, future, cl.creationTime, cl.expirationTime,cl.priority);
			cl.getAllProperties().forEach((k,v)->{cart.addProperty(k, v);});
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
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.COMMAND_REJECTED,cart.getAllProperties(), null));
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
				cart = new ResultConsumerCart<K, OUT, L>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime, rcl.priority);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, LoadType.RESULT_CONSUMER,rcl.priority);
			}
			rcl.getAllProperties().forEach((k,v)->{ cart.addProperty(k, v);});
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
		CompletableFuture<Boolean> future = cart.getFuture();
		try {
			cartBeforePlacementValidator.accept(cart);
			if ( ! inQueue.add(cart) ) {
				future.cancel(true);
			}
		} catch (RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.CART_REJECTED,cart.getAllProperties(), null));
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
		K key = cart.getKey();
		if (key == null) {
			if (cart.getLoadType() == MULTI_KEY_PART) {
				try {
					MultiKeyCart<K,?,L> mc = ((MultiKeyCart<K,?,L>)cart);
					final L label = cart.getLabel();
					final Load<K,?> load = mc.getValue();
					final Predicate<K> filter = load.getFilter();
					final Function<K,Cart<K, ?, L>> cartBuilder;
					switch(load.getLoadType()) {
						case RESULT_CONSUMER:
							cartBuilder = k->{
								ResultConsumerCart rcc = new ResultConsumerCart(k, (ResultConsumer)load.getValue(), cart.getCreationTime(), cart.getExpirationTime(),cart.getPriority());
								rcc.putAllProperties(cart.getAllProperties());
								return rcc;
							};
						break;
						default:
							cartBuilder = k->{
								ShoppingCart<K,?,L> c = new ShoppingCart<>(k, load.getValue(), label,cart.getCreationTime(), cart.getExpirationTime());
								c.putAllProperties(mc.getAllProperties());
								return c;
							};
					}
					LOG.trace("READY TO APPLY MULTY");
					collector.entrySet().stream().map(entry -> entry.getKey()).filter(filter)
							.collect(Collectors.toList())
					.forEach(k -> {
						LOG.trace("MULTI FILTER MATCH {}",k);
								processSite(cartBuilder.apply(k), accept);
							});
					cart.getFuture().complete(true);
				} catch (Exception e) {
					cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(new ScrapBin(cart.getLabel(), cart, "MultiKey cart failure", e, FailureType.GENERAL_FAILURE,cart.getAllProperties(), null));
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
			FutureCart<K, ? extends OUT, L> fc = (FutureCart<K, ? extends OUT, L>) cart;
			resultFuture = fc.getValue();
		}
		if (cart.getLoadType() == RESULT_CONSUMER) {
			ResultConsumerCart<K,OUT, L> rc = (ResultConsumerCart<K,OUT, L>) cart;
			newConsumer = rc.getValue();
		}
		FailureType failureType = FailureType.GENERAL_FAILURE;
		try {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Read " + cart);
			}
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
				currentSite.timeout((Cart<K, ?, L>) cart);
			} else if (accept) {
				failureType = FailureType.DATA_REJECTED;
				currentSite.accept((Cart<K, ?, L>) cart);
			}
			failureType = FailureType.READY_FAILED;
			if (currentSite.ready()) {
				failureType = FailureType.BUILD_FAILED;
				OUT res = currentSite.unsafeBuild();
				failureType = FailureType.RESULT_CONSUMER_FAILED;
				completeSuccessfully((BuildingSite<K, L, ?, OUT>) currentSite,res,Status.READY);
				failureType = FailureType.BEFORE_EVICTION_FAILED;
				keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.READY, currentSite.getProperties()));
			}
			cart.getFuture().complete(Boolean.TRUE);
		} catch (KeepRunningConveyorException e) {
			if (currentSite != null) {
				currentSite.setLastError(e);
				currentSite.setLastCart(cart);
				cart.getScrapConsumer().accept(new ScrapBin(cart.getKey(), cart,
						"Cart Processor failed. Keep running", e, FailureType.KEEP_RUNNING_EXCEPTION,cart.getAllProperties(), null));
				scrapConsumer.accept(new ScrapBin(cart.getKey(), currentSite,
						"Site Processor failed. Keep running", e, FailureType.KEEP_RUNNING_EXCEPTION,cart.getAllProperties(), currentSite.getAcknowledge()));
				currentSite.completeFuturesExceptionaly(e);
			}
		} catch (Exception e) {
			if (currentSite != null) {
				currentSite.setStatus(Status.INVALID);
				currentSite.setLastError(e);
				currentSite.setLastCart(cart);
				cart.getScrapConsumer().accept(new ScrapBin(cart.getKey(), cart,
						"Cart Processor failed", e, failureType,cart.getAllProperties(), null));
				scrapConsumer.accept(new ScrapBin(cart.getKey(), currentSite,
						"Site Processor failed", e, failureType,cart.getAllProperties(), currentSite.getAcknowledge()));
				currentSite.completeFuturesExceptionaly(e);
			} else {
				cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
						new ScrapBin(cart.getKey(), cart, "Cart Processor Failed", e, failureType,cart.getAllProperties(), null));
			}
			if (!failureType.equals(FailureType.BEFORE_EVICTION_FAILED)) {
				try {
					keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.INVALID, currentSite==null?null:currentSite.getProperties()));
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
		Status statusForEviction = Status.TIMED_OUT;
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
						statusForEviction = Status.READY;
					} else {
						if (postponeTimeout(buildingSite)) {
							continue;
						}
						if (LOG.isTraceEnabled()) {
							LOG.trace("Expired and not finished " + key);
						}
						scrapConsumer.accept(new ScrapBin(key,
								buildingSite, "Site expired", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
						buildingSite.cancelFutures();
					}
				} catch (Exception e) {
					buildingSite.setStatus(Status.INVALID);
					buildingSite.setLastError(e);
					statusForEviction = Status.INVALID;
					scrapConsumer.accept(new ScrapBin(key,
							buildingSite, "Timeout processor failed ", e, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
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
						buildingSite, "Site expired. No timeout action", null, FailureType.BUILD_EXPIRED,buildingSite.getProperties(), buildingSite.getAcknowledge()));
				buildingSite.cancelFutures();
			}
			keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, statusForEviction, buildingSite.getProperties()));
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
		this.cartConsumer = (l,c,b)->{
			((LabeledValueConsumer)cartConsumer).accept(l, getPayload(c), b);
		};
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
		Map<String,Object> properties = bs == null ? new HashMap<>():bs.getProperties();
		conveyor.keyBeforeEviction.accept(new AcknowledgeStatus<K>(key, Status.CANCELED, properties));
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
	 * Peek build.
	 *
	 * @param <K> the key type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static <K,OUT> void peekBuild(AssemblingConveyor<K,?,OUT> conveyor, Cart<K, Consumer<ProductBin<K, OUT>>, ?> cart) {
		K key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			BuildingSite<K, ?, ?, ? extends OUT> bs = conveyor.collector.get(key);
			try {
				OUT prod = bs.unsafeBuild();
				ProductBin<K, OUT> bin = new ProductBin<K, OUT>(key, prod, bs.getDelayMsec(), bs.getStatus(), bs.getProperties(), null);
				cart.getValue().accept(bin);
				cart.getFuture().complete(true);
			} catch (Exception e) {
				Map<String,Object> prop = bs.getProperties();
				prop.put("ERROR", e);
				ProductBin<K, OUT> bin = new ProductBin<K, OUT>(key, null, bs.getDelayMsec(), Status.INVALID, bs.getProperties(), null);
				cart.getValue().accept(bin);
				cart.getFuture().complete(false);
			}
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring peek command.", key);
			ProductBin<K, OUT> bin = new ProductBin<K, OUT>(key, null, 0, Status.NOT_FOUND, null, null);
			cart.getValue().accept(bin);
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
		K key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			BuildingSite<K, ?, ?, ? extends OUT> bs = conveyor.collector.get(key);
			Memento memento = bs.getMemento();
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
		K key = cart.getKey();
		
		if (conveyor.collector.containsKey(key)) {
			BuildingSite<K, ?, ?, ? extends OUT> bs = conveyor.collector.get(key);
			Memento memento = cart.getValue();
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
				LOG.info("interrupted "+conveyorName);
			} else {
				LOG.warn("No active build found for "+conveyorName);				
			}
		} else {
			LOG.warn(name + " ignored interruption for "+conveyorName);
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
		this.suspended = true;
	}

	@Override
	public void resume() {
		this.suspended = false;
		lock.tell();
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}
	
}
