/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static com.aegisql.conveyor.ConvertersRegister.*;
import static com.aegisql.conveyor.InitiationServiceRegister.SERVICES;
import static com.aegisql.conveyor.MBeanRegister.MBEAN;

/**
 * The Interface Conveyor.
 *
 * @param <K>   the key type
 * @param <L>   the label type
 * @param <OUT> the target class type
 * @author Mikhail Teplitskiy
 * @version 1.1.0
 */
public interface Conveyor<K, L, OUT> extends ConveyorInitiatingService {

	/**
	 * The Constant LOG.
	 */
	Logger LOG = LoggerFactory.getLogger(Conveyor.class);

	/**
	 * Part.
	 *
	 * @return the part loader
	 */
	PartLoader<K,L> part();

	/**
	 * StaticPart.
	 *
	 * @return the static part loader
	 */
	StaticPartLoader<L> staticPart();

	/**
	 * Builds the.
	 *
	 * @return the builder loader
	 */
	BuilderLoader<K, OUT> build();

	/**
	 * Future.
	 *
	 * @return the future loader
	 */
	FutureLoader<K, OUT> future();

	/**
	 * Command.
	 *
	 * @return the command loader
	 */
	CommandLoader<K, OUT> command();

	/**
	 * Adds the cart to the input queue.
	 *
	 * @param <V>  the value type
	 * @param cart the cart
	 * @return true, if successful
	 */
	<V> CompletableFuture<Boolean> place(Cart<K, V, L> cart);

	/**
	 * Adds the command to the management queue.
	 *
	 * @param <V>     the value type
	 * @param command Cart
	 * @return true, if successful
	 */
	<V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command);

	/**
	 * Result consumer.
	 *
	 * @return the result consumer loader
	 */
	ResultConsumerLoader<K, OUT> resultConsumer();

	/**
	 * Result consumer.
	 *
	 * @param consumer the consumer
	 * @return the result consumer loader
	 */
	ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer);

	/**
	 * Gets the result consumer.
	 *
	 * @return the result consumer
	 */
	ResultConsumer<K,OUT> getResultConsumer();

	/**
	 * Gets the collector size.
	 *
	 * @return the collector size
	 */
	int getCollectorSize();

	/**
	 * Gets the input queue size.
	 *
	 * @return the input queue size
	 */
	int getInputQueueSize();

	/**
	 * Gets the delayed queue size.
	 *
	 * @return the delayed queue size
	 */
	int getDelayedQueueSize();

	/**
	 * Scrap consumer.
	 *
	 * @return the scrap consumer loader
	 */
	ScrapConsumerLoader<K> scrapConsumer();

	/**
	 * Scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 * @return the scrap consumer loader
	 */
	ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer);

	/**
	 * Stop.
	 */
	void stop();

	/**
	 * complete all tasks and stop.
	 *
	 * @return the completable future
	 */
	CompletableFuture<Boolean> completeAndStop();

	/**
	 * Sets the idle heart beat.
	 *
	 * @param heartbeat the heartbeat
	 * @param unit      the unit
	 */
	void setIdleHeartBeat(long heartbeat, TimeUnit unit);

	/**
	 * Sets the idle heart beat.
	 *
	 * @param duration the duration
	 */
	void setIdleHeartBeat(Duration duration);

	/**
	 * Sets the default builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit           the unit
	 */
	void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit);

	/**
	 * Sets the default builder timeout.
	 *
	 * @param duration the duration
	 */
	void setDefaultBuilderTimeout(Duration duration);

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit    the unit
	 */
	void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit);

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param duration the duration
	 */
	void rejectUnexpireableCartsOlderThan(Duration duration);


	/**
	 * Sets the on timeout action.
	 *
	 * @param timeoutAction the new on timeout action
	 */
	void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction);

	/**
	 * Sets the default cart consumer.
	 *
	 * @param <B>          the generic type
	 * @param cartConsumer the cart consumer
	 */
	<B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer);

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready);

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the new readiness evaluator
	 */
	void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness);

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier);

	/**
	 * Sets the name.
	 *
	 * @param string the new name
	 */
	void setName(String string);

	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	boolean isRunning();

	/**
	 * Adds the cart before placement validator.
	 *
	 * @param cartBeforePlacementValidator the cart before placement validator
	 */
	void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator);

	/**
	 * Adds the before key eviction action.
	 *
	 * @param keyBeforeEviction the key before eviction
	 */
	void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction);

	/**
	 * Adds the before key rescheduling action.
	 *
	 * @param keyBeforeRescheduling the key before rescheduling
	 */
	void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling);

	/**
	 * Gets the expiration time.
	 *
	 * @param key the key
	 * @return the expiration time
	 */
	long getExpirationTime(K key);

	/**
	 * Checks if is l balanced.
	 *
	 * @return true, if is l balanced
	 */
	boolean isLBalanced();

	/**
	 * Gets the accepted labels.
	 *
	 * @return the accepted labels
	 */
	Set<L> getAcceptedLabels();

	/**
	 * Accept labels.
	 *
	 * @param labels the labels
	 */
	void acceptLabels(L... labels);

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Enable postpone expiration.
	 *
	 * @param flag the flag
	 */
	void enablePostponeExpiration(boolean flag);

	/**
	 * Enable postpone expiration on timeout.
	 *
	 * @param flag the flag
	 */
	void enablePostponeExpirationOnTimeout(boolean flag);

	/**
	 * Sets the expiration postpone time.
	 *
	 * @param time the time
	 * @param unit the unit
	 */
	void setExpirationPostponeTime(long time, TimeUnit unit);

	/**
	 * Sets the expiration postpone time.
	 *
	 * @param duration the duration
	 */
	void setExpirationPostponeTime(Duration duration);

	/**
	 * Checks if is forwarding results.
	 *
	 * @return true, if is forwarding results
	 */
	boolean isForwardingResults();


	/**
	 * Gets the tester for.
	 *
	 * @param <K>      the key type
	 * @param <L>      the generic type
	 * @param <OUT>    the generic type
	 * @param conveyor the conveyor
	 * @return the tester for
	 */
	static <K, L,OUT> ReadinessTester<K, L,OUT> getTesterFor(Conveyor<K, L, OUT> conveyor) {
		return new ReadinessTester<>(conveyor::setReadinessEvaluator);
	}

	/**
	 * Gets the consumer for.
	 *
	 * @param <L>      the generic type
	 * @param <OUT>    the generic type
	 * @param conveyor the conveyor
	 * @return the consumer for
	 */
	static <L,OUT> LabeledValueConsumer<L, ?, Supplier<? extends OUT>> getConsumerFor(Conveyor<?, L, OUT> conveyor) {
		return (l,v,b)->{
			throw new IllegalStateException("undefined behavior for label '"+l+"'"+" value='"+v+"'");
		};
	}

	/**
	 * Gets the consumer for.
	 *
	 * @param <L>      the generic type
	 * @param <OUT>    the generic type
	 * @param <B>      the generic type
	 * @param conveyor the conveyor
	 * @param builder  the builder
	 * @return the consumer for
	 */
	static <L,OUT,B extends Supplier<? extends OUT>> LabeledValueConsumer<L, ?, B> getConsumerFor(Conveyor<?, L, OUT> conveyor, Class<B> builder) {
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

	/**
	 * Suspend processing thread.
	 */
	void suspend();

	/**
	 * Resume processing thread.
	 */
	void resume();

	/**
	 * Checks if is suspended.
	 *
	 * @return true, if is suspended
	 */
	boolean isSuspended();

	/**
	 * M bean interface class.
	 *
	 * @return the class
	 */
	Class<?> mBeanInterface();

	/**
	 * By name.
	 *
	 * @param name the name
	 * @return the conveyor
	 */
	static Conveyor byName(String name) {
		return MBEAN.byName(name);
	}

	/**
	 * Lazy supplier.
	 *
	 * @param name the name
	 * @return the supplier
	 */
	static Supplier<Conveyor> lazySupplier(String name) {
		return new LazyConveyorSupplier(name);
	}


	/**
	 * Un register.
	 *
	 * @param name the name
	 */
	static void unRegister(String name) {
		MBEAN.unRegister(name);
	}

	/**
	 * Un register conveyor instance.
	 */
	default void unRegister() {
		unRegister(this.getName());
	}

	static void loadServices() {
		SERVICES.reload();
		SERVICES.getLoadedConveyorNames();
	}

	static Set<String> getKnownConveyorNames() {
		loadServices();
		var set = new HashSet<>(MBEAN.getKnownConveyorNames());
		return Collections.unmodifiableSet(set);
	}

	static Set<String> getLoadedConveyorNames() {
		return Collections.unmodifiableSet(SERVICES.getLoadedConveyorNames());
	}

	/**
	 * Register.
	 *
	 * @param conveyor    the conveyor
	 * @param mbeanObject the mbean object
	 */
	static void register(Conveyor conveyor, Object mbeanObject) {
		MBEAN.register(conveyor,mbeanObject);
	}

	/**
	 * Register.
	 *
	 * @param mbeanObject the mbean object
	 */
	default void register(Object mbeanObject) {
		register(this,mbeanObject);
	}

	@Override
	default List<String> getInitiatedConveyorNames() {
		return List.of(getName());
	}

	default void registerKeyConverter(String typeName, Function<?,K> converter) {
		KEY_CONVERTERS.setConverter(getName(),typeName,converter);
	}

	default <T> void registerKeyConverter(Class<T> type, Function<T,K> converter) {
		KEY_CONVERTERS.setConverter(getName(),type,converter);
	}

	default void registerLabelConverter(String typeName, Function<?,L> converter) {
		LABEL_CONVERTERS.setConverter(getName(),typeName,converter);
	}

	default <T> void registerLabelConverter(Class<T> type, Function<T,L> converter) {
		LABEL_CONVERTERS.setConverter(getName(),type, converter);
	}

	default void registerValueConverter(String typeName, Function<?,?> converter) {
		VALUE_CONVERTERS.setConverter(getName(),typeName,converter);
	}

	default <T> void registerValueConverter(Class<T> type, Function<T,?> converter) {
		VALUE_CONVERTERS.setConverter(getName(),type,converter);
	}

	default Function<?,K> getKeyConverter(String typeName) {
		return KEY_CONVERTERS.getConverter(getName(),typeName);
	}

	default <T >Function<T,K> getKeyConverter(Class<T> type) {
		return KEY_CONVERTERS.getConverter(getName(),type);
	}

	default Function<?,L> getLabelConverter(String typeName) {
		return LABEL_CONVERTERS.getConverter(getName(),typeName);
	}

	default <T >Function<T,L> getLabelConverter(Class<T> type) {
		return LABEL_CONVERTERS.getConverter(getName(),type);
	}

	default Function<?,?> getValueConverter(String typeName) {
		return VALUE_CONVERTERS.getConverter(getName(),typeName);
	}

	default <T >Function<T,?> getValueConverter(Class<T> type) {
		return VALUE_CONVERTERS.getConverter(getName(),type);
	}

}
