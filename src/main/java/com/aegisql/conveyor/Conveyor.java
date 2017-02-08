/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.loaders.BuilderLoader;
import com.aegisql.conveyor.loaders.CommandLoader;
import com.aegisql.conveyor.loaders.FutureLoader;
import com.aegisql.conveyor.loaders.MultiKeyPartLoader;
import com.aegisql.conveyor.loaders.PartLoader;

// TODO: Auto-generated Javadoc
/**
 * The Interface Conveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the target class type
 */
public interface Conveyor<K, L, OUT> {

	/**
	 * Part.
	 *
	 * @param <X> the generic type
	 * @return the part loader
	 */
	public <X> PartLoader<K, L, X, OUT, Boolean> part();
	
	/**
	 * Builds the.
	 *
	 * @return the builder loader
	 */
	public BuilderLoader<K, OUT, Boolean> build();
	
	/**
	 * Future.
	 *
	 * @return the future loader
	 */
	public FutureLoader<K, OUT> future();
		
	/**
	 * Command.
	 *
	 * @return the command loader
	 */
	public CommandLoader<K, OUT> command();

	/**
	 * Multi key part.
	 *
	 * @param <X> the generic type
	 * @return the multi key part loader
	 */
	public <X> MultiKeyPartLoader<K, L, X, OUT, Boolean> multiKeyPart();

	/**
	 * Adds the cart to the input queue.
	 *
	 * @param <V> the value type
	 * @param cart the cart
	 * @return true, if successful
	 */
	public <V> CompletableFuture<Boolean> place(Cart<K,V,L> cart);
	
	/**
	 * Adds the command to the management queue.
	 *
	 * @param <V> the value type
	 * @param command Cart
	 * @return true, if successful
	 */
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command);
	
	/**
	 * Gets the collector size.
	 *
	 * @return the collector size
	 */
	public int getCollectorSize();
	
	/**
	 * Gets the input queue size.
	 *
	 * @return the input queue size
	 */
	public int getInputQueueSize();
	
	/**
	 * Gets the delayed queue size.
	 *
	 * @return the delayed queue size
	 */
	public int getDelayedQueueSize();
	
	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer);
	
	/**
	 * Stop.
	 */
	public void stop();
	
	/**
	 * Sets the idle heart beat.
	 *
	 * @param heartbeat the heartbeat
	 * @param unit the unit
	 */
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit);
	
	/**
	 * Sets the default builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit);
	
	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit);
	
	/**
	 * Sets the on timeout action.
	 *
	 * @param timeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction);
	
	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K, OUT>> resultConsumer);
	
	/**
	 * Sets the default cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer);
	
	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready);
	
	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the new readiness evaluator
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness);
	
	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier);
	
	/**
	 * Sets the name.
	 *
	 * @param string the new name
	 */
	public void setName(String string);
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning();
	
	/**
	 * Adds the cart before placement validator.
	 *
	 * @param cartBeforePlacementValidator the cart before placement validator
	 */
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator);
	
	/**
	 * Adds the before key eviction action.
	 *
	 * @param keyBeforeEviction the key before eviction
	 */
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction);
	
	/**
	 * Adds the before key rescheduling action.
	 *
	 * @param keyBeforeRescheduling the key before rescheduling
	 */
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling);
	
	/**
	 * Gets the expiration time.
	 *
	 * @param key the key
	 * @return the expiration time
	 */
	public long getExpirationTime(K key);
	
	/**
	 * Checks if is l balanced.
	 *
	 * @return true, if is l balanced
	 */
	public boolean isLBalanced();
	
	/**
	 * Gets the accepted labels.
	 *
	 * @return the accepted labels
	 */
	public Set<L> getAcceptedLabels();
	
	/**
	 * Accept labels.
	 *
	 * @param labels the labels
	 */
	public void acceptLabels(L... labels);
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Forward partial result to.
	 *
	 * @param partial the partial
	 * @param conv the conv
	 */
	public void forwardPartialResultTo(L partial, Conveyor<K,L,OUT> conv);
	
	/**
	 * Enable postpone expiration.
	 *
	 * @param flag the flag
	 */
	public void enablePostponeExpiration(boolean flag);
	
	/**
	 * Enable postpone expiration on timeout.
	 *
	 * @param flag the flag
	 */
	public void enablePostponeExpirationOnTimeout(boolean flag);
	
	/**
	 * Sets the expiration postpone time.
	 *
	 * @param time the time
	 * @param unit the unit
	 */
	public void setExpirationPostponeTime(long time, TimeUnit unit);
	
	/**
	 * Checks if is forwarding results.
	 *
	 * @return true, if is forwarding results
	 */
	boolean isForwardingResults();
	
	
	/**
	 * Gets the tester for.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @return the tester for
	 */
	static <K, L,OUT> ReadinessTester<K, L,OUT> getTesterFor(Conveyor<K, L, OUT> conveyor) {
		return new ReadinessTester<>();
	}
	
	/**
	 * Gets the consumer for.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @return the consumer for
	 */
	static <L,OUT> LabeledValueConsumer<L, ?, Supplier<? extends OUT>> getConsumerFor(Conveyor<?, L, OUT> conveyor) {
		return (l,v,b)->{
			throw new IllegalStateException("undefined behavior for label '"+l+"'"+" value='"+v+"'");
		};
	}
	
	/**
	 * Gets the cart counter.
	 *
	 * @return the cart counter
	 */
	long getCartCounter();

}
