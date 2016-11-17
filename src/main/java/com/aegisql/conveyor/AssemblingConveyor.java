/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.delay.DelayProvider;

// TODO: Auto-generated Javadoc
/**
 * The Class AssemblingConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class AssemblingConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	protected final static Logger LOG = LoggerFactory.getLogger(AssemblingConveyor.class);

	/** The in queue. */
	protected final Deque<Cart<K,?,L>> inQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The m queue. */
	protected final Queue<GeneralCommand<K, ?>> mQueue = new ConcurrentLinkedDeque<>(); // this class does not permit the use of null elements.

	/** The delay provider. */
	private final DelayProvider<K> delayProvider = new DelayProvider<>();
	
	/** The collector. */
	protected final Map<K, BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>> collector = new HashMap<>();

	protected long cartCounter = 0;
	
	protected long commandCounter = 0;
	
	/** The builder timeout. */
	protected long builderTimeout = 0;
	
	/** The start time reject. */
	protected long startTimeReject = System.currentTimeMillis();

	/** The timeout action. */
	protected Consumer<Supplier<? extends OUT>> timeoutAction;

	/** The result consumer. */
	protected Consumer<ProductBin<K,OUT>> resultConsumer = out -> { LOG.error("LOST RESULT {} {}",out.key,out.product); };

	/** The scrap consumer. */
	protected Consumer<ScrapBin<?,?>> scrapConsumer = (scrapBin) -> { LOG.error("{}",scrapBin); };
	
	/** The cart consumer. */
	protected LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer = (l,v,b) -> { 
		scrapConsumer.accept( new ScrapBin<L, Object>(l,v, "Cart Consumer is not set. label",FailureType.GENERAL_FAILURE));
		throw new IllegalStateException("Cart Consumer is not set");
	};
	
	/** The ready. */
	protected BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness = null;
	
	/** The builder supplier. */
	protected BuilderSupplier<OUT> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};
	
	/** The cart before placement validator. */
	protected Consumer<Cart<K,?,L>> cartBeforePlacementValidator = cart -> {if(cart==null) throw new NullPointerException("Cart is null");};

	/** The command before placement validator. */
	private Consumer<GeneralCommand<K, ?>> commandBeforePlacementValidator = cart -> {if(cart==null) throw new NullPointerException("Command is null");};

	/** The key before eviction. */
	private Consumer<K> keyBeforeEviction = key -> {
		LOG.trace("Key is ready to be evicted {}",key);
		collector.remove(key);
	};

	/** The key before reschedule. */
	private BiConsumer<K,Long> keyBeforeReschedule = (key,newExpirationTime) -> {
		Objects.requireNonNull(key, "NULL key cannot be rescheduld");
		Objects.requireNonNull(newExpirationTime, "NULL newExpirationTime cannot be applied to the schedile");
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = collector.get(key);
		if( buildingSite != null ) {
			long oldExpirationTime = buildingSite.builderExpiration;
			delayProvider.getBox(oldExpirationTime).delete(key);
			buildingSite.updateExpirationTime( newExpirationTime );
			LOG.trace("Rescheduled {}. added expiration {} msec",key,newExpirationTime - oldExpirationTime);
			if(newExpirationTime > 0) {
				delayProvider.getBox(newExpirationTime).add(key);
			}
		} else {
			LOG.trace("Build is not found for the key {}",key);
		}
	};

	/** The running. */
	
	protected volatile boolean running = true;
	
	/** The synchronize builder. */
	protected boolean synchronizeBuilder = false;

	/** The accepted labels. */
	protected final Set<L> acceptedLabels = new HashSet<>();

	
	/** The inner thread. */
	private final Thread innerThread;
	
	/**
	 * The Class Lock.
	 */
	private static final class Lock {
		
		/** The r lock. */
		private final ReentrantLock rLock = new ReentrantLock();
		
		/** The has carts. */
		private final Condition hasCarts  = rLock.newCondition();
		
		/** The expiration collection interval. */
		private long expirationCollectionInterval = Long.MAX_VALUE;
		
		/**
		 * Sets the expiration collection interval.
		 *
		 * @param expirationCollectionInterval the new expiration collection interval
		 */
		public void setExpirationCollectionInterval(long expirationCollectionInterval) {
			this.expirationCollectionInterval = expirationCollectionInterval;
		}

		/**
		 * Sets the expiration collection unit.
		 *
		 * @param expirationCollectionUnit the new expiration collection unit
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
		 * @param q the q
		 * @throws InterruptedException the interrupted exception
		 */
		public void waitData(Queue<?> q) throws InterruptedException {
			rLock.lock();
			try {
				if( q.isEmpty() ) {
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
	
	private final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer(); 

	/** The l balanced. */
	private boolean lBalanced = false;

	/** The postpone expiration enabled. */
	private boolean postponeExpirationEnabled = false;

	/** The postpone expiration mills. */
	private long postponeExpirationMills = 0;

	/** The forwarding results. */
	private boolean forwardingResults = false;

	protected ObjectName objectName;

	private String forwardingTo = "not forwarding";

	/**
	 * Wait data.
	 *
	 * @return true, if successful
	 */
	private boolean waitData() {
		try {
			lock.waitData( inQueue );
		} catch (InterruptedException e) {
			LOG.error("Interrupted ", e);
			stop();
		}
		return running;
	}
	
	/**
	 * For each key and builder.
	 *
	 * @param keyBuilderPairConsumer the key builder pair consumer
	 */
	public void forEachKeyAndBuilder( BiConsumer<K, Supplier<? extends OUT>> keyBuilderPairConsumer ) {
		lock.rLock.lock();
		try {
			collector.forEach((key,site)->{
				try {
					keyBuilderPairConsumer.accept( key, site.getProductSupplier() );
				} catch (RuntimeException e) {
					LOG.error("Error processing key="+key,e);
				}
				
			});
		} finally {
			lock.rLock.unlock();
		}
	}
	
	/**
	 * Gets the building site.
	 *
	 * @param cart the cart
	 * @return the building site
	 */
	private BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> getBuildingSite(Cart<K,?,L> cart) {
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = null;
		boolean returnNull = false;
		K key = cart.getKey();
		if(key == null) {
			returnNull = true;
		} else if( Status.TIMED_OUT.equals( cart.getValue() )) {
			returnNull = true;
		} else if ( (buildingSite = collector.get(key)) == null) {
			BuilderSupplier<OUT> bs;
			if(cart.getValue() != null && cart.getValue() instanceof BuilderSupplier ) {
				bs = ((Supplier<BuilderSupplier<OUT>>)cart).get();
				if( bs == null ) {
					bs = builderSupplier;
				}
				
				if(bs != null) {
					buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(
							cart, 
							bs, 
							cartConsumer, 
							readiness, 
							timeoutAction, 
							builderTimeout, 
							TimeUnit.MILLISECONDS,
							synchronizeBuilder,
							saveCarts,
							postponeExpirationEnabled,
							postponeExpirationMills
						);
					if(cart.getValue() instanceof FutureSupplier) {
						FutureSupplier fs = (FutureSupplier) cart.getValue();
						buildingSite.addFuture(fs.getFuture());
					}
				} else {
					scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Ignore cart. Neither creating cart nor default builder supplier available",FailureType.BUILD_INITIALIZATION_FAILED) );
					cart.getFuture().complete(Boolean.FALSE);
				}
				returnNull = true;
			} else if(builderSupplier != null) {
				buildingSite = new BuildingSite<K, L, Cart<K,?,L>, OUT>(
						cart, 
						builderSupplier, 
						cartConsumer, 
						readiness, 
						timeoutAction, 
						builderTimeout, 
						TimeUnit.MILLISECONDS, 
						synchronizeBuilder,
						saveCarts,
						postponeExpirationEnabled,
						postponeExpirationMills
					);
			} else {
				scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Ignore cart. Neither builder nor builder supplier available",FailureType.BUILD_INITIALIZATION_FAILED) );
				cart.getFuture().complete(Boolean.FALSE);
				returnNull = true;
			}
			if(buildingSite != null) {
				collector.put(key, buildingSite);
				if(buildingSite.getExpirationTime() > 0) {
					delayProvider.getBox(buildingSite.getExpirationTime()).add(key);
				}
			}
		}
		if( returnNull ) {
			return null;
		} else {
			return buildingSite;
		}
	}
		
	/**
	 * Instantiates a new assembling conveyor.
	 */
	public AssemblingConveyor() {
		
		this.addCartBeforePlacementValidator(cart->{
			if( ! running ) {
				throw new IllegalStateException("Conveyor is not running");
			}
		});
		this.addCartBeforePlacementValidator(cart->{
			if( cart.expired() ) {
				throw new IllegalStateException("Cart has already expired " + cart);
			}
		});
		this.addCartBeforePlacementValidator(cart->{
			if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject) ) {
				throw new IllegalStateException("Cart is too old " + cart);
			}
		});
		
		commandBeforePlacementValidator = commandBeforePlacementValidator.andThen(cmd->{if( ! running ) {
			throw new IllegalStateException("Conveyor is not running");
		}}).andThen(cmd->{if( cmd.expired() ) {
				throw new IllegalStateException("Command has already expired " + cmd);
		}}).andThen(cmd->{if( cmd.getCreationTime() < (System.currentTimeMillis() - startTimeReject) ) {
				throw new IllegalStateException("Command is too old " + cmd);
		}});
		acceptedLabels.add(null);
		this.innerThread = new Thread(() -> {
			try {
				while (running) {
					if (! waitData() ) 
						break;
					processManagementCommands();
					Cart<K,?,L> cart = inQueue.poll();
					if(cart != null) {
						cartCounter++;
						processSite(cart,true);
					}
					removeExpired();
				}
				LOG.info("Leaving {}", Thread.currentThread().getName());
				drainQueues();
			} catch (Throwable e) { // Let it crash, but don't pretend its running
				stop();
				throw e;
			}
		});
		innerThread.setDaemon(false);
		this.name = "AssemblingConveyor "+innerThread.getId();
		innerThread.setName( this.name );
		this.setMbean(this.name);
		innerThread.start();
	}

	protected void setMbean(String name) {
		try {
			final AssemblingConveyor<K,L,OUT> thisConv = this;
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
					if(thisConv.postponeExpirationEnabled) {
						return ""+thisConv.postponeExpirationMills;
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
					if(acceptedLabels.size() == 0 || acceptedLabels.contains(null) ) {
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
			ObjectName newObjectName = new ObjectName("com.aegisql.conveyor:type="+name);
			synchronized(mBeanServer) {
				if(this.objectName == null) {
					this.objectName = newObjectName;
					this.setMbean(name);
				}
				if(mBeanServer.isRegistered(this.objectName)) {
					mBeanServer.unregisterMBean(objectName);
					this.objectName = newObjectName;
					this.setMbean(name);
				} else {
					mBeanServer.registerMBean(mbean, newObjectName);
					this.objectName = newObjectName;
				}
			}
		} catch (Exception e) {
			LOG.error("MBEAN error "+name,e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Process management commands.
	 */
	private void processManagementCommands() {
		Cart<K,?,CommandLabel> cmd = null;
		while((cmd = mQueue.poll()) != null) {
			commandCounter++;
			if(LOG.isDebugEnabled()) {
				LOG.debug("processing command "+cmd);
			}
			CommandLabel l = cmd.getLabel();
			try{
				l.get().accept(this, cmd);
			} catch(Exception e) {
				cmd.getFuture().completeExceptionally(e);
				throw e;
			}
		}
	}

	/**
	 * Drain queues.
	 */
	protected void drainQueues() {
		Cart<K,?,L> cart = null;
		while((cart = inQueue.poll()) != null) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Draining inQueue",FailureType.CONVEYOR_STOPPED) );
			cart.getFuture().cancel(true);
			if( cart instanceof FutureCart ) {
				FutureCart<K,OUT,L> fc = (FutureCart<K,OUT,L>)cart;
				fc.getValue().cancel(true);
			}
		}
		delayProvider.clear();
		collector.forEach((k,v)->{
			scrapConsumer.accept( new ScrapBin<K,Object>(k,v,"Draining collector",FailureType.CONVEYOR_STOPPED) );
			v.cancelFutures();
		});
		collector.clear();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(GeneralCommand<K, V> cart) {
		try {
			CompletableFuture<Boolean> future = cart.getFuture();
			commandBeforePlacementValidator.accept(cart);
			boolean r = mQueue.add(cart);
			if( ! r ) {
				future.cancel(true);
			}
			lock.tell();
			return future;
		} catch (RuntimeException e ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,e.getMessage(), FailureType.COMMAND_REJECTED) );
			cart.getFuture().cancel(true);
			lock.tell();
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, long)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long expirationTime) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, expirationTime ){ private static final long serialVersionUID = 1L;} );
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, ttl, unit){ private static final long serialVersionUID = 1L;} );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, java.time.Duration)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Duration duration) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, duration){ private static final long serialVersionUID = 1L;} );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, java.time.Instant)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Instant instant) {		
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, instant){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(Cart<K,V,L> cart) {
		try {
			CompletableFuture<Boolean> future = cart.getFuture();
			cartBeforePlacementValidator.accept(cart);
			boolean r = inQueue.add(cart);
			if( ! r ) {
				future.cancel(true);
			}
			lock.tell();
			return future;
		} catch (RuntimeException e ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,e.getMessage(),FailureType.CART_REJECTED) );
			lock.tell();
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(java.lang.Object, java.lang.Object, java.lang.Object, long)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, long expirationTime) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(java.lang.Object, java.lang.Object, java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(java.lang.Object, java.lang.Object, java.lang.Object, java.time.Duration)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, Duration duration) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(java.lang.Object, java.lang.Object, java.lang.Object, java.time.Instant)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, Instant instant) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,instant));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(Cart<K,V,L> cart) {
		CompletableFuture<Boolean> future = cart.getFuture();
		try {
			cartBeforePlacementValidator.accept(cart);
			boolean r = inQueue.add(cart);
			if( ! r ) {
				future.cancel(true);
			}
			lock.tell();
			return future;
		} catch (RuntimeException e ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,e.getMessage(),FailureType.CART_REJECTED) );
			future.completeExceptionally(e);
			lock.tell();
			return future;
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label) {
		return this.offer( new ShoppingCart<K,V,L>(key,value,label));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(java.lang.Object, java.lang.Object, java.lang.Object, long)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, long expirationTime) {
		return this.offer( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(java.lang.Object, java.lang.Object, java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.offer( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(java.lang.Object, java.lang.Object, java.lang.Object, java.time.Duration)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, Duration duration) {
		return this.offer( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(java.lang.Object, java.lang.Object, java.lang.Object, java.time.Instant)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, Instant instant) {
		return this.offer( new ShoppingCart<K,V,L>(key,value,label,instant));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(com.aegisql.conveyor.cart.CreatingCart)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(CreatingCart<K, OUT, L> cart) {
		return this.add( cart );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key) );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, long)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, long expirationTime) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,expirationTime) );		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, long ttl, TimeUnit unit) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,ttl,unit) );		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, java.time.Duration)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, Duration duration) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,duration) );		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, java.time.Instant)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, Instant instant) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,instant) );				
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, com.aegisql.conveyor.BuilderSupplier)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,value) );		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, long)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, long expirationTime) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,value,expirationTime) );				
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, long ttl, TimeUnit unit) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,value,ttl,unit) );				
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, java.time.Duration)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, Duration duration) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,value,duration) );				
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuild(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, java.time.Instant)
	 */
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, Instant instant) {
		return this.createBuild( new CreatingCart<K, OUT, L>(key,value,instant) );						
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(com.aegisql.conveyor.cart.CreatingCart)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(CreatingCart<K, OUT, L> cart) {
		BuilderAndFutureSupplier<OUT> supplier = (BuilderAndFutureSupplier<OUT>) cart.getValue();
		CompletableFuture<Boolean> cartFuture = this.add(  cart );		
		if(cartFuture.isCancelled()) {
			supplier.getFuture().cancel(true);
		}
		return supplier.getFuture();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(this.builderSupplier, new CompletableFuture<OUT>())) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, long)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, long expirationTime) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(this.builderSupplier, new CompletableFuture<OUT>()),expirationTime) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, long ttl, TimeUnit unit) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(this.builderSupplier, new CompletableFuture<OUT>()),ttl,unit) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, java.time.Duration)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, Duration duration) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(this.builderSupplier, new CompletableFuture<OUT>()),duration) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, java.time.Instant)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, Instant instant) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(this.builderSupplier, new CompletableFuture<OUT>()),instant) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, com.aegisql.conveyor.BuilderSupplier)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, BuilderSupplier<OUT> value) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(value, new CompletableFuture<OUT>())) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, long)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, BuilderSupplier<OUT> value, long expirationTime) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(value, new CompletableFuture<OUT>()),expirationTime) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, BuilderSupplier<OUT> value, long ttl, TimeUnit unit) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(value, new CompletableFuture<OUT>()),ttl,unit) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, java.time.Duration)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, BuilderSupplier<OUT> value, Duration duration) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(value, new CompletableFuture<OUT>()),duration) );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#createBuildFuture(java.lang.Object, com.aegisql.conveyor.BuilderSupplier, java.time.Instant)
	 */
	@Override
	public CompletableFuture<OUT> createBuildFuture(K key, BuilderSupplier<OUT> value, Instant instant) {
		return this.createBuildFuture( new CreatingCart<K, OUT, L>(key,new BuilderAndFutureSupplier<>(value, new CompletableFuture<OUT>()),instant) );		
	}	
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	public <V> CompletableFuture<OUT> getFuture(Cart<K, V, L> cart) {
		CompletableFuture<OUT> future = ((FutureCart<K,OUT,L>)cart).getValue();
		CompletableFuture<Boolean> cartFuture = this.add( cart );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(java.lang.Object)
	 */
	@Override
	public CompletableFuture<OUT> getFuture(K key) {
		return getFuture(new FutureCart<K,OUT,L>(key,new CompletableFuture<>()));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(java.lang.Object, long)
	 */
	@Override
	public CompletableFuture<OUT> getFuture(K key, long expirationTime) {
		return getFuture(new FutureCart<K,OUT,L>(key,new CompletableFuture<>(),expirationTime));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public CompletableFuture<OUT> getFuture(K key, long ttl, TimeUnit unit) {
		return getFuture(new FutureCart<K,OUT,L>(key,new CompletableFuture<>(),ttl,unit));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(java.lang.Object, java.time.Duration)
	 */
	@Override
	public CompletableFuture<OUT> getFuture(K key, Duration duration) {
		return getFuture(new FutureCart<K,OUT,L>(key,new CompletableFuture<>(),duration));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getFuture(java.lang.Object, java.time.Instant)
	 */
	@Override
	public CompletableFuture<OUT> getFuture(K key, Instant instant) {
		return getFuture(new FutureCart<K,OUT,L>(key,new CompletableFuture<>(),instant));
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
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
	}

	/**
	 * Stop.
	 */
	public void stop() {
		running = false;
		lock.tell();
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
	 * @param cart the cart
	 * @param accept the accept
	 */
	private void processSite(Cart<K,?,L> cart, boolean accept) {
		K key = cart.getKey();
		if( key == null ) {
			return;
		}
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = null;
		CompletableFuture resultFuture = null;
		if( cart instanceof FutureCart ) {
			FutureCart<K,? extends OUT,L> fc = (FutureCart<K, ? extends OUT, L>) cart;
			resultFuture = fc.getValue();
		}
		FailureType failureType = FailureType.GENERAL_FAILURE;
		try {
			if(LOG.isTraceEnabled()) {
				LOG.trace("Read " + cart);
			}
			buildingSite = getBuildingSite(cart);
			if(buildingSite == null) {
				cart.getFuture().complete(Boolean.FALSE);
				if( resultFuture != null ) {
					resultFuture.completeExceptionally(new Exception("No active building site found"));
				}
				return;
			}
			if( resultFuture != null ) {
				buildingSite.addFuture(resultFuture);
				return;
			}
			if(Status.TIMED_OUT.equals(cart.getValue())) {
				failureType = FailureType.ON_TIMEOUT_FAILED;
				buildingSite.timeout((Cart<K,?,L>) cart);
			} else if( accept ) {
				failureType = FailureType.DATA_REJECTED;
				buildingSite.accept((Cart<K,?,L>) cart);
			}
			failureType = FailureType.READY_FAILED;
			if (buildingSite.ready()) {
				failureType = FailureType.BEFORE_EVICTION_FAILED;
				keyBeforeEviction.accept(key);
				failureType = FailureType.BUILD_FAILED;
				OUT res = buildingSite.build();
				failureType = FailureType.RESULT_CONSUMER_FAILED;
				resultConsumer.accept(new ProductBin<K,OUT>(key, res, buildingSite.getDelay(TimeUnit.MILLISECONDS), Status.READY));
				buildingSite.completeFuturesWithValue(res);
			}
			cart.getFuture().complete(Boolean.TRUE);
		} catch (Exception e) {
			if (buildingSite != null) {
				buildingSite.setStatus(Status.INVALID);
				buildingSite.setLastError(e);
				buildingSite.setLastCart(cart);
				scrapConsumer.accept( new ScrapBin<K,BuildingSite<K,?,?,?>>(cart.getKey(),buildingSite,"Site Processor failed",e,failureType) );
				buildingSite.completeFuturesExceptionaly(e);
			} else {
				scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Processor Failed",e,failureType) );
			}
			if( ! failureType.equals(FailureType.BEFORE_EVICTION_FAILED)) {
				try {
					keyBeforeEviction.accept(key);
				} catch (Exception e2) {
					LOG.error("BeforeEviction failed after processing failure: {} {} {}",failureType,e.getMessage(),e2.getMessage());
					collector.remove(key);
				}
			}
			cart.getFuture().completeExceptionally(e);
		}
	}
	
	/**
	 * Removes the expired.
	 */
	private void removeExpired() {
		int cnt = 0;
		for(K key: delayProvider.getAllExpiredKeys()) {	
			BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> buildingSite = collector.get(key);
			if(buildingSite == null) {
				continue;
			}
			
			if(postponeExpirationEnabled) {
				long expirationTime = buildingSite.getExpirationTime();
				if(  expirationTime > System.currentTimeMillis() ) {
					LOG.trace("Expiration has bin postponed for key={}",key);
					delayProvider.getBox(expirationTime).add(key);
					continue;
				}
			}
			
			buildingSite.setStatus(Status.TIMED_OUT);
			if (collector.containsKey(key)) {
				keyBeforeEviction.accept(key);
				cnt++;
				if (timeoutAction != null || buildingSite.getBuilder() instanceof TimeoutAction ) {
					try {
						ShoppingCart<K,Object,L> to = new ShoppingCart<K,Object,L>(buildingSite.getCreatingCart().getKey(), Status.TIMED_OUT,null);
						buildingSite.timeout((Cart<K,?,L>)to);
						if (buildingSite.ready()) {
							if(LOG.isTraceEnabled()) {
								LOG.trace("Expired and finished " + key);
							}
							OUT res = buildingSite.build();
							resultConsumer.accept(new ProductBin<K,OUT>(key, res, buildingSite.getDelay(TimeUnit.MILLISECONDS), Status.TIMED_OUT));
							buildingSite.completeFuturesWithValue(res);
						} else {
							if(LOG.isTraceEnabled()) {
								LOG.trace("Expired and not finished " + key);
							}
							scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Site expired",FailureType.BUILD_EXPIRED) );
							buildingSite.cancelFutures();
						}
					} catch (Exception e) {
						buildingSite.setStatus(Status.INVALID);
						buildingSite.setLastError(e);
						scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Timeout processor failed ",e, FailureType.BUILD_EXPIRED) );
						buildingSite.completeFuturesExceptionaly(e);
					}
				} else {
					if(LOG.isTraceEnabled()) {
						LOG.trace("Expired and removed " + key);
					}
					scrapConsumer.accept( new ScrapBin<K,BuildingSite<K, L, Cart<K,?,L>, ? extends OUT>>(key,buildingSite,"Site expired. No timeout action",FailureType.BUILD_EXPIRED) );
					buildingSite.cancelFutures();
				}
			}
		}
		if(cnt > 0) {
			if(LOG.isTraceEnabled()) {
				LOG.trace("Timeout collected: " + cnt);
			}
		}
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
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
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
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
	 * @param timeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		this.timeoutAction = timeoutAction;
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		this.resultConsumer = resultConsumer;
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		this.cartConsumer = cartConsumer;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness) {
		this.readiness = readiness;
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		this.readiness = (status,builder) -> readiness.test( builder ) ;
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		this.builderSupplier = builderSupplier;
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name= name;
		this.innerThread.setName(name);
		this.setMbean(this.name);
	}

	/*
	 * STATIC METHODS TO SUPPORT MANAGEMENT COMMANDS
	 * 
	 * */

	/**
	 * Creates the now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void createNow( AssemblingConveyor conveyor, Object cart ) {
		BuildingSite bs = conveyor.getBuildingSite((Cart) cart);
		((Cart)cart).getFuture().complete(true);
	}

	/**
	 * Cancel now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void cancelNow( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		conveyor.keyBeforeEviction.accept(key);
		((Cart)cart).getFuture().complete(true);
	}

	/**
	 * acknowledge build.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	//TODO: add something
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void acknowledge( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		((Cart)cart).getFuture().complete(true);
	}

	/**
	 * Cancel now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void rescheduleNow( AssemblingConveyor conveyor, Object cart ) {
		Cart command = ((Cart)cart);
		Object key = command.getKey();
		long newExpirationTime = command.getExpirationTime();
		conveyor.keyBeforeReschedule.accept(key, newExpirationTime);
		((Cart)cart).getFuture().complete(true);
	}

	/**
	 * Timeout now.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void timeoutNow( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		conveyor.collector.get(key);
		conveyor.keyBeforeReschedule.accept(key, System.currentTimeMillis() );
		((Cart)cart).getFuture().complete(true);
	}

	/**
	 * Check build.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	static void checkBuild( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		if(conveyor.collector.containsKey(key)) {
			conveyor.processSite((Cart) cart, false);
		} else {
			LOG.debug("Key '{}' does not exist. Ignoring check command.",key);
		}
	}
	
	/**
	 * Future build.
	 *
	 * @param conveyor the conveyor
	 * @param cart the cart
	 */
	//TODO: finish this
	static void futureBuild( AssemblingConveyor conveyor, Object cart ) {
		Object key = ((Cart)cart).getKey();
		if(conveyor.collector.containsKey(key)) {
			conveyor.processSite((Cart) cart, false);
			((Cart)cart).getFuture().complete(true);

		} else {
			LOG.debug("Key '{}' does not exist. Ignoring check command.",key);
			((Cart)cart).getFuture().complete(false);
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
	 * @param saveCarts the new keep carts on site
	 */
	public void setKeepCartsOnSite(boolean saveCarts) {
		this.saveCarts = saveCarts;
	}

	/**
	 * Sets the synchronize builder.
	 *
	 * @param synchronizeBuilder the new synchronize builder
	 */
	public void setSynchronizeBuilder(boolean synchronizeBuilder) {
		this.synchronizeBuilder = synchronizeBuilder;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCartBeforePlacementValidator(java.util.function.Consumer)
	 */
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if(cartBeforePlacementValidator != null) {
			this.cartBeforePlacementValidator = this.cartBeforePlacementValidator.andThen( cartBeforePlacementValidator );
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyEvictionAction(java.util.function.Consumer)
	 */
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		if(keyBeforeEviction != null) {
			this.keyBeforeEviction = keyBeforeEviction.andThen(this.keyBeforeEviction);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyReschedulingAction(java.util.function.BiConsumer)
	 */
	public void addBeforeKeyReschedulingAction(BiConsumer<K,Long> keyBeforeRescheduling) {
		if(keyBeforeRescheduling != null) {
			this.keyBeforeReschedule = keyBeforeRescheduling.andThen(this.keyBeforeReschedule);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getExpirationTime(java.lang.Object)
	 */
	public long getExpirationTime(K key) {
		BuildingSite<K, L, Cart<K,?,L>, ? extends OUT> bs = collector.get(key);
		if( bs == null ) {
			return -1;
		} else {
			return bs.builderExpiration;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#acceptLabels(java.lang.Object[])
	 */
	public void acceptLabels(L... labels) {
		if(labels != null && labels.length > 0) {
			for(L l:labels) {
				acceptedLabels.add(l);
			}
			this.addCartBeforePlacementValidator(cart->{
				if( ! acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException("Conveyor '"+this.name+"' cannot process label '"+cart.getLabel()+"'");					
				}
			});
			
			lBalanced = true;
			
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#forwardPartialResultTo(java.lang.Object, com.aegisql.conveyor.Conveyor)
	 */
	public void forwardPartialResultTo(L partial, Conveyor<K,L,OUT> conv) {
		this.forwardingResults  = true;
		this.forwardingTo  = conv.toString();
		this.setResultConsumer(bin->{
			LOG.debug("Forward {} from {} to {} {}",partial,this.name,conv.getName(),bin.product);
			Cart<K,OUT,L> partialResult = new ShoppingCart<>(bin.key, bin.product, partial, bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			conv.add( partialResult );
		});
	}
	
	/**
	 * Creates a close copy of current conveyor
	 * set readiness evaluator and result consumer to throw an exception
	 * acceptedLabels are not copied.
	 *
	 * @return the assembling conveyor
	 */
	public AssemblingConveyor<K,L,OUT> detach() {
		AssemblingConveyor<K,L,OUT> c = new AssemblingConveyor<>();
		c.setBuilderSupplier( builderSupplier );
		c.setDefaultBuilderTimeout( builderTimeout, TimeUnit.MILLISECONDS );
		c.setIdleHeartBeat( getExpirationCollectionIdleInterval(), getExpirationCollectionIdleTimeUnit() );
		c.setName( "copy of " + name );
		c.setScrapConsumer( scrapConsumer );
		c.setReadinessEvaluator(b->{
			throw new IllegalStateException("Readiness evaluator is not set for copy of conveyor '"+name+"'");
		});
		c.setDefaultCartConsumer( cartConsumer );
		c.setKeepCartsOnSite( saveCarts );
		c.setOnTimeoutAction( timeoutAction );
		c.setResultConsumer(bin->{
			throw new IllegalStateException("Result Consumet is not set for copy of conveyor '"+name+"'");
		});
		c.setSynchronizeBuilder( synchronizeBuilder);
		
		c.startTimeReject = this.startTimeReject;
		
		return c;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isLBalanced()
	 */
	public boolean isLBalanced() {
		return lBalanced;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getAcceptedLabels()
	 */
	public Set<L> getAcceptedLabels() {
		return acceptedLabels;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AssemblingConveyor [name=" + name + ", thread=" + innerThread.getId() + "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpiration(boolean)
	 */
	@Override
	public void enablePostponeExpiration(boolean flag) {
		this.postponeExpirationEnabled = flag;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		this.postponeExpirationMills = unit.toMillis(time);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isForwardingResults()
	 */
	@Override
	public boolean isForwardingResults() {
		return forwardingResults;
	}

	public long getCartCounter() {
		return cartCounter;
	}

	public long getCommandCounter() {
		return commandCounter;
	}

}
