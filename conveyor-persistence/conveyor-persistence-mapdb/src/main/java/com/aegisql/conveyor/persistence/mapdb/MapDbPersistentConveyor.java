package com.aegisql.conveyor.persistence.mapdb;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
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

// TODO: Auto-generated Javadoc
/**
 * The Class MapDbPersistentConveyor.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class MapDbPersistentConveyor<K,L,OUT> implements Conveyor<K,L,OUT> {
	
	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(MapDbPersistentConveyor.class);
	
	/** The db path. */
	private final String dbPath;
	
	/** The name. */
	private final String name;
	
	/** The created timestam. */
	private final long createdTimestam = System.currentTimeMillis();
	
	/** The in queue. */
	protected final BTreeMap<Long,Cart<K, ?, L>> inQueue;
	
	/** The scrap logger. */
	protected ScrapConsumer<K,?> scrapLogger = scrapBin->{
		LOG.error("{}",scrapBin);
	};

	
	/** The scrap consumer. */
	protected ScrapConsumer<K,?> scrapConsumer = scrapLogger;

	
	/** The db name. */
	private final String dbName;
	
	/** The forward. */
	Conveyor<K, L, OUT> forward;
	
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

		/** The cursor. */
		protected Long cursor = 0L;

		/**
		 * Wait data.
		 *
		 * @param q
		 *            the q
		 * @throws InterruptedException
		 *             the interrupted exception
		 */
		public void waitData(BTreeMap<Long,?> q) throws InterruptedException {
			rLock.lock();
			try {
				Long ceiling = q.ceilingKey(cursor);
//				LOG.debug("cursor {} ceiling {}",cursor,ceiling);
				if ( cursor.equals(ceiling) ) {
					hasCarts.await(expirationCollectionInterval, expirationCollectionUnit);
				} else if( ceiling != null ){
					cursor = ceiling;
				}
			} finally {
				rLock.unlock();
			}
		}

	}

	/** The lock. */
	private final Lock lock = new Lock();
	
	/** The running. */
	protected volatile boolean running = true;
		
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

	/** The inner thread. */
	private final Thread innerThread;
	
	/** The collector. */
	private final Map<K,Set<Long>> collector = new HashMap<>();
	
	/**
	 * Instantiates a new map db persistent conveyor.
	 *
	 * @param name the name
	 * @param dbPath the db path
	 * @param forward the forward
	 */
	public MapDbPersistentConveyor(String name, String dbPath, Conveyor<K, L, OUT> forward) {
		this.dbPath = dbPath;
		this.name = name;
		this.dbName = this.dbPath+"/"+this.name+"."+this.createdTimestam+".db";
		DB db = DBMaker.fileDB(this.dbName).make();
		inQueue = (BTreeMap<Long, Cart<K, ?, L>>) db.treeMap("inQueue").create();
		this.forward = forward;
		this.forward.setName(name);
		
		this.innerThread = new Thread( ()->{
			try {
				while(running) {
					if (!waitData())
						break; //When interrupted, which is exceptional behavior, should return right away
					LOG.debug("Running...");
					Map<Long,Cart<K,?,L>> m = inQueue.tailMap(lock.cursor);
					LOG.debug("MAP: {}",m);
					m.forEach((k,cart)->{
						Set<Long> ids = collector.get(cart.getKey());
						if(ids == null) {
							ids = new LinkedHashSet<>();
							collector.put(cart.getKey(),ids);
						}
						if(! ids.contains(k)) {
							ids.add(k);
							forward.resultConsumer().id(cart.getKey()).andThen(bin->{
								LOG.debug("Removing {}",cart.getKey());
								collector.get(cart.getKey()).forEach(pk->{ inQueue.remove(pk); });
							}).set();
							forward.place(cart); 
						}
					});
				}
			} catch(Throwable e) {
				stop();
				throw e;
			}
		} );
		this.innerThread.setDaemon(false);
		this.innerThread.setName(name+".persistence");
		this.innerThread.start();
		
	}
	

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#part()
	 */
	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#staticPart()
	 */
	@Override
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#build()
	 */
	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#future()
	 */
	@Override
	public FutureLoader<K, OUT> future() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#command()
	 */
	@Override
	public CommandLoader<K, OUT> command() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#place(com.aegisql.conveyor.cart.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		CompletableFuture<Boolean> future = cart.getFuture();
		try {
			long nano = System.nanoTime();
			cart.addProperty("PK", nano);
			inQueue.put(nano, cart);
		} catch( RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.CART_REJECTED,cart.getAllProperties(), null));
		} finally {
			lock.tell();
		}
		return future;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#command(com.aegisql.conveyor.cart.command.GeneralCommand)
	 */
	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer()
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#resultConsumer(com.aegisql.conveyor.consumers.result.ResultConsumer)
	 */
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getCollectorSize()
	 */
	@Override
	public int getCollectorSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getInputQueueSize()
	 */
	@Override
	public int getInputQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getDelayedQueueSize()
	 */
	@Override
	public int getDelayedQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer()
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#scrapConsumer(com.aegisql.conveyor.consumers.scrap.ScrapConsumer)
	 */
	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#completeAndStop()
	 */
	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setIdleHeartBeat(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setIdleHeartBeat(java.time.Duration)
	 */
	@Override
	public void setIdleHeartBeat(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setDefaultBuilderTimeout(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setDefaultBuilderTimeout(java.time.Duration)
	 */
	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#rejectUnexpireableCartsOlderThan(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#rejectUnexpireableCartsOlderThan(java.time.Duration)
	 */
	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setOnTimeoutAction(java.util.function.Consumer)
	 */
	@Override
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setDefaultCartConsumer(com.aegisql.conveyor.LabeledValueConsumer)
	 */
	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setReadinessEvaluator(java.util.function.BiPredicate)
	 */
	@Override
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setReadinessEvaluator(java.util.function.Predicate)
	 */
	@Override
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setBuilderSupplier(com.aegisql.conveyor.BuilderSupplier)
	 */
	@Override
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setName(java.lang.String)
	 */
	@Override
	public void setName(String string) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isRunning()
	 */
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCartBeforePlacementValidator(java.util.function.Consumer)
	 */
	@Override
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyEvictionAction(java.util.function.Consumer)
	 */
	@Override
	public void addBeforeKeyEvictionAction(BiConsumer<K,Status> keyBeforeEviction) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyReschedulingAction(java.util.function.BiConsumer)
	 */
	@Override
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getExpirationTime(java.lang.Object)
	 */
	@Override
	public long getExpirationTime(K key) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isLBalanced()
	 */
	@Override
	public boolean isLBalanced() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getAcceptedLabels()
	 */
	@Override
	public Set<L> getAcceptedLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#acceptLabels(java.lang.Object[])
	 */
	@Override
	public void acceptLabels(L... labels) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#forwardResultTo(com.aegisql.conveyor.Conveyor, java.lang.Object)
	 */
	@Override
	public <L2, OUT2> void forwardResultTo(Conveyor<K, L2, OUT2> destination, L2 label) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#forwardResultTo(com.aegisql.conveyor.Conveyor, java.util.function.Function, java.lang.Object)
	 */
	@Override
	public <K2, L2, OUT2> void forwardResultTo(Conveyor<K2, L2, OUT2> destination,
			Function<ProductBin<K, OUT>, K2> keyConverter, L2 label) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpiration(boolean)
	 */
	@Override
	public void enablePostponeExpiration(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpirationOnTimeout(boolean)
	 */
	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(java.time.Duration)
	 */
	@Override
	public void setExpirationPostponeTime(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isForwardingResults()
	 */
	@Override
	public boolean isForwardingResults() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getCartCounter()
	 */
	@Override
	public long getCartCounter() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void setAutoAcknowledge(boolean auto) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setAcknowledgeAction(BiConsumer<K, Status> ackAction) {
		// TODO Auto-generated method stub
		
	}

}
