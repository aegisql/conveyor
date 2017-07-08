package org.conveyor.persistence.mapdb;

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
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.ScrapBin.FailureType;
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

public class MapDbPersistentConveyor<K,L,OUT> implements Conveyor<K,L,OUT> {
	
	private final static Logger LOG = LoggerFactory.getLogger(MapDbPersistentConveyor.class);
	
	private final String dbPath;
	private final String name;
	private final long createdTimestam = System.currentTimeMillis();
	
	protected final BTreeMap<Long,Cart<K, ?, L>> inQueue;
	
	/** The scrap logger. */
	protected ScrapConsumer<K,?> scrapLogger = scrapBin->{
		LOG.error("{}",scrapBin);
	};

	
	/** The scrap consumer. */
	protected ScrapConsumer<K,?> scrapConsumer = scrapLogger;

	
	private final String dbName;
	
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
	
	private final Map<K,Set<Long>> collector = new HashMap<>();
	
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
	

	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FutureLoader<K, OUT> future() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandLoader<K, OUT> command() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		CompletableFuture<Boolean> future = cart.getFuture();
		try {
			long nano = System.nanoTime();
			cart.addProperty("PK", nano);
			inQueue.put(nano, cart);
		} catch( RuntimeException e) {
			cart.getScrapConsumer().andThen((ScrapConsumer)scrapConsumer).accept(
					new ScrapBin(cart.getKey(), cart, e.getMessage(), e, FailureType.CART_REJECTED,cart.getAllProperties()));
		} finally {
			lock.tell();
		}
		return future;
	}

	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCollectorSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInputQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDelayedQueueSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setIdleHeartBeat(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setName(String string) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getExpirationTime(K key) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isLBalanced() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<L> getAcceptedLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void acceptLabels(L... labels) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <L2, OUT2> void forwardResultTo(Conveyor<K, L2, OUT2> destination, L2 label) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <K2, L2, OUT2> void forwardResultTo(Conveyor<K2, L2, OUT2> destination,
			Function<ProductBin<K, OUT>, K2> keyConverter, L2 label) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enablePostponeExpiration(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExpirationPostponeTime(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isForwardingResults() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getCartCounter() {
		// TODO Auto-generated method stub
		return 0;
	}

}
