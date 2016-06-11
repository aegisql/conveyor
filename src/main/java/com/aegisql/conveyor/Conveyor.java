/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.AbstractCommand;

// TODO: Auto-generated Javadoc
/**
 * The Interface Conveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the target class type
 */
public interface Conveyor<K, L, OUT> {

	/**
	 * Adds the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public <V> boolean add(Cart<K,V,L> cart);
	public <V> boolean add(K key, V value, L label);
	public <V> boolean add(K key, V value, L label, long expirationTime);
	public <V> boolean add(K key, V value, L label, long ttl, TimeUnit unit);
	public <V> boolean add(K key, V value, L label, Duration duration);
	public <V> boolean add(K key, V value, L label, Instant instant);


	public boolean createBuild(K key);
	public boolean createBuild(K key, long expirationTime);
	public boolean createBuild(K key, long ttl, TimeUnit unit);
	public boolean createBuild(K key, Duration duration);
	public boolean createBuild(K key, Instant instant);
	public boolean createBuild(K key, BuilderSupplier<OUT> value);
	public boolean createBuild(K key, BuilderSupplier<OUT> value, long expirationTime);
	public boolean createBuild(K key, BuilderSupplier<OUT> value, long ttl, TimeUnit unit);
	public boolean createBuild(K key, BuilderSupplier<OUT> value, Duration duration);
	public boolean createBuild(K key, BuilderSupplier<OUT> value, Instant instant);

	
	/**
	 * Offers the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public <V> boolean offer(Cart<K,V,L> cart);
	public <V> boolean offer(K key, V value, L label);
	public <V> boolean offer(K key, V value, L label, long expirationTime);
	public <V> boolean offer(K key, V value, L label, long ttl, TimeUnit unit);
	public <V> boolean offer(K key, V value, L label, Duration duration);
	public <V> boolean offer(K key, V value, L label, Instant instant);
	
	/**
	 * Adds the command to the management queue.
	 *
	 * @param command Cart
	 * @return true, if successful
	 */
	public <V> boolean addCommand(AbstractCommand<K, V> command);
	public <V> boolean addCommand(K key, V value, CommandLabel label);
	public <V> boolean addCommand(K key, V value, CommandLabel label, long expirationTime);
	public <V> boolean addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit);
	public <V> boolean addCommand(K key, V value, CommandLabel label, Duration duration);
	public <V> boolean addCommand(K key, V value, CommandLabel label, Instant instant);
	
	
	public int getCollectorSize();
	public int getInputQueueSize();
	public int getDelayedQueueSize();
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer);
	public void stop();
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit);
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit);
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit);
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction);
	public void setResultConsumer(Consumer<ProductBin<K, OUT>> resultConsumer);
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer);
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready);
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness);
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier);
	public void setName(String string);
	public boolean isRunning();
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator);
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction);
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling);
	public long getExpirationTime(K key);
	public boolean isLBalanced();
	public Set<L> getAcceptedLabels();
	public void acceptLabels(L... labels);
}
