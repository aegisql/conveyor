package com.aegisql.conveyor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelConveyor<K, L, IN extends Cart<K, ?, L>, OUT> implements Conveyor<K, L, IN, OUT> {

	private final static Logger LOG = LoggerFactory.getLogger(ParallelConveyor.class);

	private long expirationCollectionInterval = 0;

	private long builderTimeout = 0;
	
	private long startTimeReject = System.currentTimeMillis();

	private boolean onTimeoutAction = false;

	private BiConsumer<String,Object> scrapConsumer = (explanation, scrap) -> { LOG.error(explanation + " " + scrap); };
	
	private volatile boolean running = true;

	private final AssemblingConveyor<K, L, IN, OUT>[] conveyors;

	private final int pf;
	
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
	
	private int idx(K key) {
		return key.hashCode() % pf;
	}

	private AssemblingConveyor<K, L, IN, OUT> getConveyor(K key) {
		return conveyors[ idx(key) ];
	}
	
	@Override
	public boolean addCommand(Cart<K,?,Command> cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Expired command",cart);
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Command too old",cart);
			throw new IllegalStateException("Data too old");
		}
		return getConveyor( cart.getKey() ).addCommand( cart );
	}
	
	@Override
	public boolean add(IN cart) {
		Objects.requireNonNull(cart);
		if (!running) {
			scrapConsumer.accept("Not Running",cart);
			throw new IllegalStateException("Assembling Conveyor is not running");
		}
		if (cart.expired()) {
			scrapConsumer.accept("Cart expired",cart);
			throw new IllegalStateException("Data expired " + cart);
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Cart too old",cart);
			throw new IllegalStateException("Data too old");
		}
		return getConveyor( cart.getKey() ).add( cart );
	}

	@Override
	public boolean offer(IN cart) {
		if( Objects.isNull(cart) ) {
			scrapConsumer.accept("Null",null);
			return false;
		}
		if ( ! running ) {
			scrapConsumer.accept("Not Running",cart);
			return false;
		}
		if ( cart.expired() ) {
			scrapConsumer.accept("Cart expired",cart);
			return false;
		}
		if( cart.getCreationTime() < (System.currentTimeMillis() - startTimeReject )) {
			scrapConsumer.accept("Cart is too old", cart);
			return false;
		}
		return getConveyor( cart.getKey() ).offer( cart );
	}

	public int getCollectorSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors[idx].getCollectorSize();
		}
	}

	public int getInputQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors[idx].getInputQueueSize();
		}
	}

	public int getDelayedQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors[idx].getDelayedQueueSize();
		}
	}

	public void setScrapConsumer(BiConsumer<String,Object> scrapConsumer) {
		this.scrapConsumer = scrapConsumer;
		for(AssemblingConveyor<K, L, IN, OUT> conv: conveyors) {
			conv.setScrapConsumer(scrapConsumer);
		}
	}

	public void stop() {
		this.running = false;
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.stop();
		}
	}

	public long getExpirationCollectionInterval() {
		return expirationCollectionInterval;
	}

	public void setExpirationCollectionInterval(long expirationCollectionInterval, TimeUnit unit) {
		this.expirationCollectionInterval = unit.toMillis(expirationCollectionInterval);
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setExpirationCollectionInterval(expirationCollectionInterval,unit);
		}
	}

	public long getBuilderTimeout() {
		return builderTimeout;
	}

	public void setBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setBuilderTimeout(builderTimeout,unit);
		}
	}

	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.startTimeReject = unit.toMillis(timeout);
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.rejectUnexpireableCartsOlderThan(timeout,unit);
		}
	}

	
	public boolean isOnTimeoutAction() {
		return onTimeoutAction;
	}

	public void setOnTimeoutAction(boolean onTimeoutAction) {
		this.onTimeoutAction = onTimeoutAction;
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setOnTimeoutAction(onTimeoutAction);
		}
	}

	public void setResultConsumer(Consumer<OUT> resultConsumer) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setResultConsumer(resultConsumer);
		}
	}

	public void setCartConsumer(LabeledValueConsumer<L, ?, Builder<OUT>> cartConsumer) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setCartConsumer(cartConsumer);
		}
	}

	public void setReadinessEvaluator(BiPredicate<Lot<K>, Builder<OUT>> ready) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setReadinessEvaluator(ready);
		}
	}

	public void setBuilderSupplier(Supplier<Builder<OUT>> builderSupplier) {
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setBuilderSupplier(builderSupplier);
		}
	}

	public void setName(String name) {
		int i = 0;
		for(AssemblingConveyor<K,L,IN,OUT> conv: conveyors) {
			conv.setName(name+" ["+i+"]");
			i++;
		}
	}

}
