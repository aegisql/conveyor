/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.utils.parallel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.AbstractCommand;

// TODO: Auto-generated Javadoc
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
	
	/** The start time reject. */
	private long startTimeReject = System.currentTimeMillis();

	/** The on timeout action. */
	private Consumer<Supplier<? extends OUT>> timeoutAction;
	
	/** The scrap consumer. */
	private Consumer<ScrapBin<?, ?>> scrapConsumer = (scrap) -> { LOG.error("{}",scrap); };
	
	/** The running. */
	private volatile boolean running = true;

	/** The conveyors. */
	private final AssemblingConveyor<K, L, OUT>[] conveyors;

	/** The pf. */
	private final int pf;
	
	private BalancingFunction<K> balancingFunction;
	
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param pf the pf
	 */
	public ParallelConveyor( int pf ) {
		this(AssemblingConveyor::new,pf);
	}

	public ParallelConveyor( Supplier<? extends AssemblingConveyor<K, L, OUT>> cs, int pf ) {
		if( pf <=0 ) {
			throw new IllegalArgumentException("");
		}
		this.pf = pf;
		AssemblingConveyor<K, L, OUT>[] conveyors = new AssemblingConveyor[pf];
		for(int i = 0; i < pf; i++) {
			conveyors[i] = cs.get();
		}
		this.conveyors = conveyors;
		this.balancingFunction = key -> key.hashCode() % pf;
	}

	/**
	 * Idx.
	 *
	 * @param key the key
	 * @return the int
	 */
	private int idx(K key) {
		return balancingFunction.balanceCart(key);
	}

	/**
	 * Gets the conveyor.
	 *
	 * @param key the key
	 * @return the conveyor
	 */
	private AssemblingConveyor<K, L, OUT> getConveyor(K key) {
		return conveyors[ idx(key) ];
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean addCommand(AbstractCommand<K, ?> cart) {
		return getConveyor( cart.getKey() ).addCommand( cart );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean add(Cart<K,?,L> cart) {
		return getConveyor( cart.getKey() ).add( cart );
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean offer(Cart<K,?,L> cart) {
		return getConveyor( cart.getKey() ).offer( cart );
	}

	public int getNumberOfConveyors() {
		return conveyors.length;
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
			return conveyors[idx].getCollectorSize();
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
			return conveyors[idx].getInputQueueSize();
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
			return conveyors[idx].getDelayedQueueSize();
		}
	}

	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
		for(AssemblingConveyor<K, L, OUT> conv: conveyors) {
			conv.setScrapConsumer(scrapConsumer);
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
		this.startTimeReject = unit.toMillis(timeout);
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
			conv.setOnTimeoutAction(timeoutAction);
		}
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
			conv.setResultConsumer(resultConsumer);
		}
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
			conv.setDefaultCartConsumer(cartConsumer);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> ready) {
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(ready);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(readiness);
		}
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(Supplier<Supplier<? extends OUT>> builderSupplier) {
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
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
			return conveyors[idx].isRunning();
		}
	}

	public void setBalancingFunction(BalancingFunction<K> balancingFunction) {
		this.balancingFunction = balancingFunction;
	}
	
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if(cartBeforePlacementValidator != null) {
			for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
				conv.addCartBeforePlacementValidator(cartBeforePlacementValidator);
			}
		}
	}


	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		if(keyBeforeEviction != null) {
			for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
				conv.addBeforeKeyEvictionAction(keyBeforeEviction);
			}
		}
	}

	public void addBeforeKeyReschedulingAction(BiConsumer<K,Long> keyBeforeRescheduling) {
		if(keyBeforeRescheduling != null) {
			for(AssemblingConveyor<K,L,OUT> conv: conveyors) {
				conv.addBeforeKeyReschedulingAction(keyBeforeRescheduling);
			}
		}
	}

}
