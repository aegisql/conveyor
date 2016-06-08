/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.utils.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.AbstractCommand;

/**
 * The Class ParallelConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the Product type
 */
public class ParallelConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ParallelConveyor.class);

	/** The expiration collection interval. */
	private long expirationCollectionInterval = 0;
	private TimeUnit expirationCollectionUnit = TimeUnit.MILLISECONDS;
	
	/** The builder timeout. */
	private long builderTimeout = 0;
	
	/** The on timeout action. */
	private Consumer<Supplier<? extends OUT>> timeoutAction;
	
	/** The running. */
	private volatile boolean running = true;

	/** The conveyors. */
	private final List<Conveyor<K, L, OUT>> conveyors = new ArrayList<>();

	/** The pf. */
	private final int pf;
	
	private Function<AbstractCommand<K,?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand;
	private Function<Cart<K,?,L>, List<? extends Conveyor<K, L, OUT>>> balancingCart;
	
	//private BalancingFunction<K> balancingFunction;
	
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param pf the pf
	 */
	public ParallelConveyor( int pf ) {
		this(AssemblingConveyor::new,pf);
	}
	
	public ParallelConveyor(Conveyor<K, L, OUT>[] conveyors) {
		this.pf = conveyors.length;
		if( this.pf == 0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		//this.balancingFunction = key -> key.hashCode() % this.pf;
		for(Conveyor<K, L, OUT> c:conveyors) {
			this.conveyors.add(c);
		}
		//here we implement different logic for L and K balanced conveyors
		this.balancingCart = cart -> { 
			int index = cart.getKey().hashCode() % pf;
			return this.conveyors.subList(index, index+1);
		};
		this.balancingCommand = command -> { 
			int index = command.getKey().hashCode() % pf;
			return this.conveyors.subList(index, index+1);
		};
	}

	public ParallelConveyor( Supplier<? extends Conveyor<K, L, OUT>> cs, int pf ) {
		if( pf <=0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		this.pf = pf;
		for(int i = 0; i < pf; i++) {
			this.conveyors.add(cs.get());
		}
		//this.balancingFunction = key -> key.hashCode() % pf;
		this.balancingCart = cart -> { 
			int index = cart.getKey().hashCode() % pf;
			return this.conveyors.subList(index, index+1);
		};
		this.balancingCommand = command -> { 
			int index = command.getKey().hashCode() % pf;
			return this.conveyors.subList(index, index+1);
		};
	}

	/**
	 * Idx.
	 *
	 * @param key the key
	 * @return the int
	 */
//	private int idx(K key) {
//		return balancingFunction.balanceCart(key);
//	}

	/**
	 * Gets the conveyor.
	 *
	 * @param key the key
	 * @return the conveyor
	 */
//	private Conveyor<K, L, OUT> getConveyor(K key) {
//		return conveyors.get( idx(key) );
//	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> boolean addCommand(AbstractCommand<K, V> cart) {
		try {
			this.balancingCommand.apply(cart).forEach(c->c.addCommand(cart));
			return true;
		} catch(RuntimeException e) {
			return false;
		}
		//return this.getConveyor( cart.getKey() ).addCommand( cart );
	}
	
	@Override
	public <V> boolean addCommand(K key, V value, CommandLabel label) {
		return this.addCommand( new AbstractCommand<K,V>(key, value, label){ private static final long serialVersionUID = 1L;} );
	}
	
	@Override
	public <V> boolean addCommand(K key, V value, CommandLabel label, long expirationTime) {
		return this.addCommand( new AbstractCommand<K,V>(key, value, label, expirationTime ){ private static final long serialVersionUID = 1L;} );
	}

	@Override
	public <V> boolean addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit) {
		return this.addCommand( new AbstractCommand<K,V>(key, value, label, ttl, unit){ private static final long serialVersionUID = 1L;} );		
	}

	@Override
	public <V> boolean addCommand(K key, V value, CommandLabel label, Duration duration) {
		return this.addCommand( new AbstractCommand<K,V>(key, value, label, duration){ private static final long serialVersionUID = 1L;} );		
	}

	@Override
	public <V> boolean addCommand(K key, V value, CommandLabel label, Instant instant) {		
		return this.addCommand( new AbstractCommand<K,V>(key, value, label, instant){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> boolean add(Cart<K,V,L> cart) {
		Objects.requireNonNull(cart, "Cart is null");
		try {
			this.balancingCart.apply(cart).forEach(c->c.add(cart));
			return true;
		} catch(RuntimeException e) {
			return false;
		}
		//return getConveyor( cart.getKey() ).add( cart );
	}
	@Override
	public <V> boolean add(K key, V value, L label) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label));
	}

	@Override
	public <V> boolean add(K key, V value, L label, long expirationTime) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	@Override
	public <V> boolean add(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	@Override
	public <V> boolean add(K key, V value, L label, Duration duration) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	@Override
	public <V> boolean add(K key, V value, L label, Instant instant) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,instant));
	}

	@Override
	public boolean createBuild(K key) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key) );
	}
	
	@Override
	public boolean createBuild(K key, long expirationTime) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,expirationTime) );		
	}
	
	@Override
	public boolean createBuild(K key, long ttl, TimeUnit unit) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,ttl,unit) );		
	}
	
	@Override
	public boolean createBuild(K key, Duration duration) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,duration) );		
	}
	
	@Override
	public boolean createBuild(K key, Instant instant) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,instant) );				
	}
	
	@Override
	public boolean createBuild(K key, BuilderSupplier<OUT> value) {
		return this.add( new CreatingCart<K, OUT, L>(key,value) );		
	}
	
	@Override
	public boolean createBuild(K key, BuilderSupplier<OUT> value, long expirationTime) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,expirationTime) );				
	}
	
	@Override
	public boolean createBuild(K key, BuilderSupplier<OUT> value, long ttl, TimeUnit unit) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,ttl,unit) );				
	}
	
	@Override
	public boolean createBuild(K key, BuilderSupplier<OUT> value, Duration duration) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,duration) );				
	}
	
	@Override
	public boolean createBuild(K key, BuilderSupplier<OUT> value, Instant instant) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,instant) );						
	}
	
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> boolean offer(Cart<K,V,L> cart) {
		try {
			this.balancingCart.apply(cart).forEach(c->c.add(cart));
			return true;
		} catch(Exception e) {
			return false;
		}
//		return getConveyor( cart.getKey() ).offer( cart );
	}

	@Override
	public <V> boolean offer(K key, V value, L label) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label));
	}

	@Override
	public <V> boolean offer(K key, V value, L label, long expirationTime) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	@Override
	public <V> boolean offer(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	@Override
	public <V> boolean offer(K key, V value, L label, Duration duration) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	@Override
	public <V> boolean offer(K key, V value, L label, Instant instant) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,instant));
	}

	public int getNumberOfConveyors() {
		return conveyors.size();
	}
	
	/**
	 * Gets the collector size.
	 *
	 * @param idx the idx
	 * @return the collector size
	 */
	public int getCollectorSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getCollectorSize();
		}
	}

	/**
	 * Gets the input queue size.
	 *
	 * @param idx the idx
	 * @return the input queue size
	 */
	public int getInputQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getInputQueueSize();
		}
	}

	/**
	 * Gets the delayed queue size.
	 *
	 * @param idx the idx
	 * @return the delayed queue size
	 */
	public int getDelayedQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getDelayedQueueSize();
		}
	}

	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer) {
		for(Conveyor<K, L, OUT> conv: conveyors) {
			conv.setScrapConsumer(scrapConsumer);
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.stop();
		}
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionIdleInterval() {
		return expirationCollectionInterval;
	}
	public TimeUnit getExpirationCollectionIdleTimeUnit() {
		return expirationCollectionUnit;
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
	 */
	public void setExpirationCollectionIdleInterval(long expirationCollectionInterval, TimeUnit unit) {
		this.expirationCollectionInterval = expirationCollectionInterval;
		this.expirationCollectionUnit = unit;
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setExpirationCollectionIdleInterval(expirationCollectionInterval,unit);
		}
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
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setDefaultBuilderTimeout(builderTimeout,unit);
		}
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.rejectUnexpireableCartsOlderThan(timeout,unit);
		}
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
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setOnTimeoutAction(timeoutAction);
		}
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setResultConsumer(resultConsumer);
		}
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setDefaultCartConsumer(cartConsumer);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> ready) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(ready);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(readiness);
		}
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setBuilderSupplier(builderSupplier);
		}
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		int i = 0;
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setName(name+" ["+i+"]");
			i++;
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
	 * Checks if is running.
	 *
	 * @param idx the idx
	 * @return true, if is running
	 */
	public boolean isRunning(int idx) {
		if(idx < 0 || idx >= pf) {
			return false;
		} else {
			return conveyors.get(idx).isRunning();
		}
	}

//	public void setBalancingFunction(BalancingFunction<K> balancingFunction) {
//		this.balancingFunction = balancingFunction;
//	}
	
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if(cartBeforePlacementValidator != null) {
			for(Conveyor<K,L,OUT> conv: conveyors) {
				conv.addCartBeforePlacementValidator(cartBeforePlacementValidator);
			}
		}
	}


	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		if(keyBeforeEviction != null) {
			for(Conveyor<K,L,OUT> conv: conveyors) {
				conv.addBeforeKeyEvictionAction(keyBeforeEviction);
			}
		}
	}

	public void addBeforeKeyReschedulingAction(BiConsumer<K,Long> keyBeforeRescheduling) {
		if(keyBeforeRescheduling != null) {
			for(Conveyor<K,L,OUT> conv: conveyors) {
				conv.addBeforeKeyReschedulingAction(keyBeforeRescheduling);
			}
		}
	}
	
	public long getExpirationTime(K key) {
		//return getConveyor( key ).getExpirationTime(key);
		return 0;
	}

	@Override
	public int getCollectorSize() {
		return -1;
	}

	@Override
	public int getInputQueueSize() {
		return -1;
	}

	@Override
	public int getDelayedQueueSize() {
		return -1;
	}

}
