/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.utils.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;

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
	
	private Function<GeneralCommand<K,?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand;
	private Function<Cart<K,?,L>, List<? extends Conveyor<K, L, OUT>>> balancingCart;

	private String name = "ParallelConveyor";

	private boolean lBalanced = false;

	private Set<L> acceptedLabels = new HashSet<>();
	
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param pf the pf
	 */
	public ParallelConveyor( int pf ) {
		this(AssemblingConveyor::new,pf);
	}
	
	public ParallelConveyor(Conveyor<K, L, OUT>... conveyors) {
		init(conveyors);
		this.pf = conveyors.length;
	}

	public ParallelConveyor( Supplier<? extends Conveyor<K, L, OUT>> cs, int pf ) {
		if( pf <=0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		Conveyor<K, L, OUT>[] cArray = new Conveyor[pf];
		for(int i = 0; i < pf; i++) {
			cArray[i] = cs.get();
		}
		init(cArray);
		this.pf = pf;
	}
	
	private void init(Conveyor<K, L, OUT>... conveyors) {
		int pf = conveyors.length;
		if( pf == 0 ) {
			throw new IllegalArgumentException("Parallelism Factor must be >=1");
		}
		int kBalanced = 0;
		int lBalanced = 0;
		List<Conveyor<K, L, OUT>> defaultConv = new ArrayList<>();
		Map<L,List<Conveyor<K, L, OUT>>> map = new HashMap<>(); 
		for(Conveyor<K, L, OUT> c:conveyors) {
			this.conveyors.add(c);
			if(c.isLBalanced()) {
				lBalanced++;
				Set<L> labels = c.getAcceptedLabels();
				for(L l:labels) {
					List<Conveyor<K, L, OUT>> convs = map.get(l);
					if(convs == null) {
						convs = new ArrayList<>();
					}
					convs.add(c);
					map.put(l, convs);
				}
			} else {
				kBalanced++;
				defaultConv.add( c );
			}
		}

		if(lBalanced == 0) {
			LOG.debug("K-Balanced Parallel conveyor parallelism:{}",pf);
			this.balancingCart = cart -> { 
				int index = cart.getKey().hashCode() % pf;
				return this.conveyors.subList(index, index+1);
			};
			this.balancingCommand = command -> { 
				int index = command.getKey().hashCode() % pf;
				return this.conveyors.subList(index, index+1);
			};
		} else {
			if( kBalanced > 1 ) {
				throw new RuntimeException("L-Balanced parallel conveyor cannot have more than one K-balanced default conveyor");
			} else if(kBalanced == 1) {
				LOG.debug("L-Balanced Parallel conveyor with default labels. {}",map);
				this.balancingCart = cart -> { 
					if(map.containsKey(cart.getLabel())) {
						return map.get(cart.getLabel());
					} else {
						return defaultConv;
					}
				};
			} else {
				LOG.debug("L-Balanced Parallel conveyor. {}",map);
				this.balancingCart = cart -> { 
					if(map.containsKey(cart.getLabel())) {
						return map.get(cart.getLabel());
					} else {
						throw new RuntimeException("L-Balanced parallel conveyor "+this.name+"has no default conveyor for label "+cart.getLabel());
					}
				};
			}
			this.balancingCommand = command -> { 
				return this.conveyors;
			};
			this.lBalanced = true;
		}
	}

	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(GeneralCommand<K, V> cart) {
		CompletableFuture<Boolean> allFutures = new CompletableFuture<Boolean>();
		
		this.balancingCommand.apply(cart).forEach(c->c.addCommand(cart));
		return allFutures;
	}
	
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label){ private static final long serialVersionUID = 1L;} );
	}
	
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long expirationTime) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, expirationTime ){ private static final long serialVersionUID = 1L;} );
	}

	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, ttl, unit){ private static final long serialVersionUID = 1L;} );		
	}

	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Duration duration) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, duration){ private static final long serialVersionUID = 1L;} );		
	}

	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Instant instant) {		
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, instant){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> add(Cart<K,V,L> cart) {
		Objects.requireNonNull(cart, "Cart is null");
		CompletableFuture<Boolean> combinedFuture = null;
		for(Conveyor<K,L,OUT> conv : this.balancingCart.apply(cart)) {
			Cart<K,V,L> cc = cart.copy();
			CompletableFuture<Boolean> cartFuture = conv.add(cc);
			if(combinedFuture == null) {
				combinedFuture = cartFuture;
			} else {
				combinedFuture = combinedFuture.thenCombine(cartFuture, ( a, b ) -> a && b );
			}
		}
		return combinedFuture;
	}
	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label));
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, long expirationTime) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, Duration duration) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	@Override
	public <V> CompletableFuture<Boolean> add(K key, V value, L label, Instant instant) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,instant));
	}

	@Override
	public CompletableFuture<Boolean> createBuild(K key) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key) );
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, long expirationTime) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,expirationTime) );		
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, long ttl, TimeUnit unit) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,ttl,unit) );		
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, Duration duration) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,duration) );		
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, Instant instant) {
		return this.add( new CreatingCart<K, Supplier<OUT>, L>(key,instant) );				
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value) {
		return this.add( new CreatingCart<K, OUT, L>(key,value) );		
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, long expirationTime) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,expirationTime) );				
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, long ttl, TimeUnit unit) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,ttl,unit) );				
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, Duration duration) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,duration) );				
	}
	
	@Override
	public CompletableFuture<Boolean> createBuild(K key, BuilderSupplier<OUT> value, Instant instant) {
		return this.add( new CreatingCart<K, OUT, L>(key,value,instant) );						
	}
	
	@Override
	public CompletableFuture<OUT> getFuture(K key) {
		CompletableFuture<OUT> future = new CompletableFuture<>();
		CompletableFuture<Boolean> cartFuture = this.add( new FutureCart<K,OUT,L>(key,future) );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}

	@Override
	public CompletableFuture<OUT> getFuture(K key, long expirationTime) {
		CompletableFuture<OUT> future = new CompletableFuture<>();
		CompletableFuture<Boolean> cartFuture = this.add( new FutureCart<K,OUT,L>(key,future,expirationTime) );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}

	@Override
	public CompletableFuture<OUT> getFuture(K key, long ttl, TimeUnit unit) {
		CompletableFuture<OUT> future = new CompletableFuture<>();
		CompletableFuture<Boolean> cartFuture = this.add( new FutureCart<K,OUT,L>(key,future,ttl,unit) );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}

	@Override
	public CompletableFuture<OUT> getFuture(K key, Duration duration) {
		CompletableFuture<OUT> future = new CompletableFuture<>();
		CompletableFuture<Boolean> cartFuture = this.add( new FutureCart<K,OUT,L>(key,future,duration) );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}

	@Override
	public CompletableFuture<OUT> getFuture(K key, Instant instant) {
		CompletableFuture<OUT> future = new CompletableFuture<>();
		CompletableFuture<Boolean> cartFuture = this.add( new FutureCart<K,OUT,L>(key,future,instant) );
		if(cartFuture.isCancelled()) {
			future.cancel(true);
		}
		return future;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#offer(com.aegisql.conveyor.Cart)
	 */
	@Override
	public <V> CompletableFuture<Boolean> offer(Cart<K,V,L> cart) {
		CompletableFuture<Boolean> combinedFuture = null;
		try {
			for(Conveyor<K,L,OUT> conv : this.balancingCart.apply(cart)) {
				Cart<K,V,L> cc = cart.copy();
				CompletableFuture<Boolean> cartFuture = conv.add(cc);
				if(combinedFuture == null) {
					combinedFuture = cartFuture;
				} else {
					combinedFuture = combinedFuture.thenCombine(cartFuture, ( a, b ) -> a && b );
				}
			}
			return combinedFuture;
		} catch(Exception e) {
			if( combinedFuture == null ) {
				combinedFuture = new CompletableFuture<Boolean>();
			}
			combinedFuture.cancel(true);
			return combinedFuture;
		}
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label));
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, long expirationTime) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,expirationTime));
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, long ttl, TimeUnit unit) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,ttl, unit));
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, Duration duration) {
		return this.add( new ShoppingCart<K,V,L>(key,value,label,duration));
	}

	@Override
	public <V> CompletableFuture<Boolean> offer(K key, V value, L label, Instant instant) {
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
		this.conveyors.forEach(conv->conv.setScrapConsumer(scrapConsumer));
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		this.conveyors.forEach(conv->conv.stop());
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
	public void setIdleHeartBeat(long expirationCollectionInterval, TimeUnit unit) {
		this.expirationCollectionInterval = expirationCollectionInterval;
		this.expirationCollectionUnit = unit;
		this.conveyors.forEach(conv->conv.setIdleHeartBeat(expirationCollectionInterval,unit));
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
		this.conveyors.forEach(conv->conv.setDefaultBuilderTimeout(builderTimeout,unit));
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.conveyors.forEach(conv->conv.rejectUnexpireableCartsOlderThan(timeout,unit));
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
		this.conveyors.forEach(conv->conv.setOnTimeoutAction(timeoutAction));
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		this.conveyors.forEach(conv->conv.setResultConsumer(resultConsumer));
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		this.conveyors.forEach(conv->conv.setDefaultCartConsumer(cartConsumer));
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> ready) {
		this.conveyors.forEach(conv->conv.setReadinessEvaluator(ready));
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		this.conveyors.forEach(conv->conv.setReadinessEvaluator(readiness));
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		this.conveyors.forEach(conv->conv.setBuilderSupplier(builderSupplier));
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
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
	
	//Think about consequences
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if(cartBeforePlacementValidator != null) {
			this.conveyors.forEach(conv->conv.addCartBeforePlacementValidator(cartBeforePlacementValidator));
		}
	}


	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		if(keyBeforeEviction != null) {
			this.conveyors.forEach(conv->conv.addBeforeKeyEvictionAction(keyBeforeEviction));
		}
	}

	public void addBeforeKeyReschedulingAction(BiConsumer<K,Long> keyBeforeRescheduling) {
		if(keyBeforeRescheduling != null) {
			this.conveyors.forEach(conv->conv.addBeforeKeyReschedulingAction(keyBeforeRescheduling));
		}
	}

	public long getExpirationTime(K key) {
		if(!lBalanced) {
			return this.conveyors.get(0).getExpirationTime(key);
		} else {
			throw new RuntimeException("Method cannot be called for L-Balanced conveyor '"+name+"'. Use getExpirationTime(K key, L label)");
		}
	}

	public long getExpirationTime(K key, L label) {
		throw new RuntimeException("Unimplemented method");
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

	public void setBalancingCommandAlgorithm(Function<GeneralCommand<K, ?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand) {
		this.balancingCommand = balancingCommand;
	}

	public void setBalancingCartAlgorithm(Function<Cart<K, ?, L>, List<? extends Conveyor<K, L, OUT>>> balancingCart) {
		this.balancingCart = balancingCart;
	}

	@Override
	public boolean isLBalanced() {
		return lBalanced;
	}

	@Override
	public Set<L> getAcceptedLabels() {
		return acceptedLabels ;
	}

	@Override
	public void acceptLabels(L... labels) {
		if(labels != null && labels.length > 0) {
			for(L l:labels) {
				acceptedLabels.add(l);
			}
			this.addCartBeforePlacementValidator(cart->{
				if( ! acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException("Parallel Conveyor '"+this.name+"' cannot process label '"+cart.getLabel()+"'");					
				}
			});
			
			lBalanced = true;
			
		}		
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ParallelConveyor [name=" + name + ", pf=" + pf + ", lBalanced=" + lBalanced + "]";
	}

	public void forwardPartialResultTo(L partial, Conveyor<K,L,OUT> conv) {
		this.setResultConsumer(bin->{
			LOG.debug("Forward {} from {} to {} {}",partial,this.name,conv.getName(),bin.product);
			Cart<K,OUT,L> partialResult = new ShoppingCart<>(bin.key, bin.product, partial, bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			conv.add( partialResult );
		});
	}

	@Override
	public void enablePostponeExpiration(boolean flag) {
		this.conveyors.forEach(conv -> conv.enablePostponeExpiration(flag));
	}

	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		this.conveyors.forEach(conv -> conv.setExpirationPostponeTime(time, unit));	
	}

}
