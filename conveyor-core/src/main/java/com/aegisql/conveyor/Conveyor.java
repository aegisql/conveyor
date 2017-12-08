/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The Interface Conveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the target class type
 */
public interface Conveyor<K, L, OUT> {
	
	/** The Constant LOG. */
	public final static Logger LOG = LoggerFactory.getLogger(Conveyor.class);

	/**
	 * Part.
	 *
	 * @param <X> the generic type
	 * @return the part loader
	 */
	public <X> PartLoader<K, L, X, OUT, Boolean> part();

	/**
	 * StaticPart.
	 *
	 * @param <X> the generic type for the value
	 * @return the static part loader
	 */
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart();

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
	 * Result consumer.
	 *
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K, OUT> resultConsumer();

	/**
	 * Result consumer.
	 *
	 * @param consumer the consumer
	 * @return the result consumer loader
	 */
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K,OUT> consumer);

	public ResultConsumer<K,OUT> getResultConsumer();
	
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
	 * Scrap consumer.
	 *
	 * @return the scrap consumer loader
	 */
	public ScrapConsumerLoader<K> scrapConsumer();

	/**
	 * Scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 * @return the scrap consumer loader
	 */
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K,?> scrapConsumer);
	
	/**
	 * Stop.
	 */
	public void stop();

	/**
	 * complete all tasks and stop.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> completeAndStop();

	/**
	 * Sets the idle heart beat.
	 *
	 * @param heartbeat the heartbeat
	 * @param unit the unit
	 */
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit);

	/**
	 * Sets the idle heart beat.
	 *
	 * @param duration the duration
	 */
	public void setIdleHeartBeat(Duration duration);

	/**
	 * Sets the default builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit);

	/**
	 * Sets the default builder timeout.
	 *
	 * @param duration the duration
	 */
	public void setDefaultBuilderTimeout(Duration duration);

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit);

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param duration the duration
	 */
	public void rejectUnexpireableCartsOlderThan(Duration duration);

	
	/**
	 * Sets the on timeout action.
	 *
	 * @param timeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction);
	
	/**
	 * Sets the default cart consumer.
	 *
	 * @param <B> the generic type
	 * @param cartConsumer the cart consumer
	 */
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer);
	
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
	public void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction);
	
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
	 * @param <L2> the generic type
	 * @param <OUT2> the generic type
	 * @param destination the destination
	 * @param label the label
	 */
	public <L2,OUT2> void forwardResultTo(Conveyor<K,L2,OUT2> destination, L2 label);

	/**
	 * Forward partial result to.
	 *
	 * @param <K2> the generic type
	 * @param <L2> the generic type
	 * @param <OUT2> the generic type
	 * @param destination the destination
	 * @param keyConverter the keyConverter
	 * @param label the label
	 */
	public <K2,L2,OUT2> void forwardResultTo(Conveyor<K2,L2,OUT2> destination, Function<ProductBin<K,OUT>,K2>keyConverter, L2 label);

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
	 * Sets the expiration postpone time.
	 *
	 * @param duration the duration
	 */
	public void setExpirationPostponeTime(Duration duration);

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
	public static <K, L,OUT> ReadinessTester<K, L,OUT> getTesterFor(Conveyor<K, L, OUT> conveyor) {
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
	public static <L,OUT> LabeledValueConsumer<L, ?, Supplier<? extends OUT>> getConsumerFor(Conveyor<?, L, OUT> conveyor) {
		return (l,v,b)->{
			throw new IllegalStateException("undefined behavior for label '"+l+"'"+" value='"+v+"'");
		};
	}

	/**
	 * Gets the consumer for.
	 *
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param <B> the generic type
	 * @param conveyor the conveyor
	 * @param builder the builder
	 * @return the consumer for
	 */
	public static <L,OUT,B extends Supplier<? extends OUT>> LabeledValueConsumer<L, ?, B> getConsumerFor(Conveyor<?, L, OUT> conveyor,Class<B> builder) {
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
	
	/**
	 * Sets the auto acknowledge. Default value is true
	 *
	 * @param auto the new auto acknowledge
	 */
	void setAutoAcknowledge(boolean auto);
	
	/**
	 * Sets the acknowledge action.
	 *
	 * @param ackAction the new acknowledge action
	 */
	void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction);

	/**
	 * Auto acknowledge on status.
	 *
	 * @param first the first
	 * @param other the other
	 */
	void autoAcknowledgeOnStatus(Status first, Status... other);
	
	/**
	 * Interrupt.
	 *
	 * @param conveyorName the conveyor name
	 */
	void interrupt(String conveyorName);
	
	/**
	 * Sets the cart payload accessor.
	 *
	 * @param payloadFunction the payload function
	 */
	void setCartPayloadAccessor(Function<Cart<K,?,L>,Object> payloadFunction);
	
	final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	public static Conveyor byName(String name) {
		ObjectName objectName;
		try {
			objectName = new ObjectName("com.aegisql.conveyor:type=" + name);
			Object res = mBeanServer.invoke(objectName, "conveyor", null, null);
			return (Conveyor) res;
		} catch (Exception e) {
			throw new RuntimeException("Conveyor with name '"+name +"' not found",e);
		}
	}
	
}
