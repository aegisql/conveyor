/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.AbstractCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class ParallelConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <IN> the generic type
 * @param <OUT> the generic type
 */
public class ParallelConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ParallelConveyor.class);

	/** The expiration collection interval. */
	private long expirationCollectionInterval = 0;

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
	private final AssemblingConveyor<K, L, IN, OUT>[] conveyors;

	/** The pf. */
	private final int pf;
	
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param pf the pf
	 */
	public ParallelConveyor( int pf ) {
		if( pf <=0 ) {
			throw new IllegalArgumentException("");
		}
		this.pf = pf;
		AssemblingConveyor<K, L, IN, OUT>[] conveyors = new AssemblingConveyor[pf];
		for(int i = 0; i < pf; i++) {
			conveyors[i] = new AssemblingConveyor<K, L, IN, OUT>();
		}
		this.conveyors = conveyors;
	}
	
	/**
	 * Idx.
	 *
	 * @param key the key
	 * @return the int
	 */
	private int idx(K key) {
		return key.hashCode() % pf;
	}

	/**
	 * Gets the conveyor.
	 *
	 * @param key the key
	 * @return the conveyor
	 */
	private AssemblingConveyor<K, L, IN, OUT> getConveyor(K key) {
		return conveyors[ idx(key) ];
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean addCommand(AbstractCommand<K, ?> cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Command Expired") );
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Command is too old") );
			throw new IllegalStateException("Data too old");
		}
		return getConveyor( cart.getKey() ).addCommand( cart );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean add(IN cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Expired") );
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is too old") );
			throw new IllegalStateException("Data too old");
		}
		return getConveyor( cart.getKey() ).add( cart );
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public boolean offer(IN cart) {
		if( Objects.isNull(cart) ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is NULL") );
			return false;
		}
		if ( ! running ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Conveyor Not Running") );
			return false;
		}
		if ( cart.expired() ) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart Expired") );
			return false;
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept( new ScrapBin<K,Cart<K,?,?>>(cart.getKey(),cart,"Cart is too old") );
			return false;
		}
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
		for(AssemblingConveyor<K, L, IN, OUT> conv: conveyors) {
			conv.setScrapConsumer(scrapConsumer);
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.stop();
		}
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionInterval() {
		return expirationCollectionInterval;
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
	 */
	public void setExpirationCollectionInterval(long expirationCollectionInterval, TimeUnit unit) {
		this.expirationCollectionInterval = unit.toMillis(expirationCollectionInterval);
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setExpirationCollectionInterval(expirationCollectionInterval,unit);
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
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
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
	 * @param onTimeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		this.timeoutAction = timeoutAction;
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setOnTimeoutAction(timeoutAction);
		}
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<OUT> resultConsumer) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setResultConsumer(resultConsumer);
		}
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setDefaultCartConsumer(cartConsumer);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> ready) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(ready);
		}
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(readiness);
		}
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(Supplier<Supplier<? extends OUT>> builderSupplier) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
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
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
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


}
