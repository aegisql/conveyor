/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static com.aegisql.conveyor.InitiationServiceRegister.SERVICES;
import static com.aegisql.conveyor.MBeanRegister.MBEAN;

/**
 * The interface Conveyor.
 *
 * @param <K>   the type parameter
 * @param <L>   the type parameter
 * @param <OUT> the type parameter
 */
public interface Conveyor<K, L, OUT> {

    /**
     * The constant LOG.
     */
    Logger LOG = LoggerFactory.getLogger(Conveyor.class);

    /**
     * Part part loader.
     *
     * @return the part loader
     */
    PartLoader<K,L> part();

    /**
     * Static part static part loader.
     *
     * @return the static part loader
     */
    StaticPartLoader<L> staticPart();

    /**
     * Build builder loader.
     *
     * @return the builder loader
     */
    BuilderLoader<K, OUT> build();

    /**
     * Future future loader.
     *
     * @return the future loader
     */
    FutureLoader<K, OUT> future();

    /**
     * Command command loader.
     *
     * @return the command loader
     */
    CommandLoader<K, OUT> command();

    /**
     * Place completable future.
     *
     * @param <V>  the type parameter
     * @param cart the cart
     * @return the completable future
     */
    <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart);

    /**
     * Command completable future.
     *
     * @param <V>     the type parameter
     * @param command the command
     * @return the completable future
     */
    <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command);

    /**
     * Result consumer result consumer loader.
     *
     * @return the result consumer loader
     */
    ResultConsumerLoader<K, OUT> resultConsumer();

    /**
     * Result consumer result consumer loader.
     *
     * @param consumer the consumer
     * @return the result consumer loader
     */
    ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer);

    /**
     * Gets result consumer.
     *
     * @return the result consumer
     */
    ResultConsumer<K,OUT> getResultConsumer();

    /**
     * Gets collector size.
     *
     * @return the collector size
     */
    int getCollectorSize();

    /**
     * Gets input queue size.
     *
     * @return the input queue size
     */
    int getInputQueueSize();

    /**
     * Gets delayed queue size.
     *
     * @return the delayed queue size
     */
    int getDelayedQueueSize();

    /**
     * Scrap consumer scrap consumer loader.
     *
     * @return the scrap consumer loader
     */
    ScrapConsumerLoader<K> scrapConsumer();

    /**
     * Scrap consumer scrap consumer loader.
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
     * Complete and stop completable future.
     *
     * @return the completable future
     */
    CompletableFuture<Boolean> completeAndStop();

    /**
     * Complete then force stop.
     *
     * @param timeout the timeout
     * @param unit    the unit
     */
    default void completeThenForceStop(long timeout, TimeUnit unit) {
        try {
            LOG.info("Preparing to stop {} max {} {}",getName(),timeout,unit);
            this.completeAndStop().get(timeout,unit);
            LOG.info("Conveyor {} has stopped gracefully",getName());
        } catch (Exception e) {
            LOG.error("Force stopping {}. Some data may be lost.",getName());
            this.stop();
        }
    }


    /**
     * Sets idle heart beat.
     *
     * @param heartbeat the heartbeat
     * @param unit      the unit
     */
    void setIdleHeartBeat(long heartbeat, TimeUnit unit);

    /**
     * Sets idle heart beat.
     *
     * @param duration the duration
     */
    void setIdleHeartBeat(Duration duration);

    /**
     * Sets default builder timeout.
     *
     * @param builderTimeout the builder timeout
     * @param unit           the unit
     */
    void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit);

    /**
     * Sets default builder timeout.
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
     * Sets on timeout action.
     *
     * @param timeoutAction the timeout action
     */
    void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction);

    /**
     * Sets default cart consumer.
     *
     * @param <B>          the type parameter
     * @param cartConsumer the cart consumer
     */
    <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer);

    /**
     * Sets readiness evaluator.
     *
     * @param ready the ready
     */
    void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready);

    /**
     * Sets readiness evaluator.
     *
     * @param readiness the readiness
     */
    void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness);

    /**
     * Sets builder supplier.
     *
     * @param builderSupplier the builder supplier
     */
    void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier);

    /**
     * Sets name.
     *
     * @param string the string
     */
    void setName(String string);

    /**
     * Is running boolean.
     *
     * @return the boolean
     */
    boolean isRunning();

    /**
     * Add cart before placement validator.
     *
     * @param cartBeforePlacementValidator the cart before placement validator
     */
    void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator);

    /**
     * Add before key eviction action.
     *
     * @param keyBeforeEviction the key before eviction
     */
    void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction);

    /**
     * Add before key rescheduling action.
     *
     * @param keyBeforeRescheduling the key before rescheduling
     */
    void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling);

    /**
     * Gets expiration time.
     *
     * @param key the key
     * @return the expiration time
     */
    long getExpirationTime(K key);

    /**
     * Is l balanced boolean.
     *
     * @return the boolean
     */
    boolean isLBalanced();

    /**
     * Gets accepted labels.
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
     * Gets name.
     *
     * @return the name
     */
    String getName();

    /**
     * Sets inactive eviction action.
     *
     * @param maxCollectorSize the max collector size
     * @param action           the action
     */
    void setInactiveEvictionAction(int maxCollectorSize, Consumer<CommandLoader.EvictionCommand<K>> action);

    /**
     * Gets generic name.
     *
     * @return the generic name
     */
    default String getGenericName() {
		try {
			var metaInfo = getMetaInfo();
            return getClassName(this.getClass())+metaInfo.generic();
		} catch (ConveyorRuntimeException e) {
			return getClassName(this.getClass())+"<?,?,?>";
		}
	}

    /**
     * Gets class name.
     *
     * @param c the c
     * @return the class name
     */
    static String getClassName(Class c) {
		String simpleName = c.getSimpleName();
		if(simpleName.isEmpty()) {
			return getClassName(c.getSuperclass());
		} else {
			return simpleName;
		}
	}

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
     * Sets expiration postpone time.
     *
     * @param time the time
     * @param unit the unit
     */
    void setExpirationPostponeTime(long time, TimeUnit unit);

    /**
     * Sets expiration postpone time.
     *
     * @param duration the duration
     */
    void setExpirationPostponeTime(Duration duration);

    /**
     * Is forwarding results boolean.
     *
     * @return the boolean
     */
    boolean isForwardingResults();


    /**
     * Gets tester for.
     *
     * @param <K>      the type parameter
     * @param <L>      the type parameter
     * @param <OUT>    the type parameter
     * @param conveyor the conveyor
     * @return the tester for
     */
    static <K, L,OUT> ReadinessTester<K, L,OUT> getTesterFor(Conveyor<K, L, OUT> conveyor) {
		return new ReadinessTester<>(conveyor::setReadinessEvaluator);
	}

    /**
     * Gets consumer for.
     *
     * @param <L>      the type parameter
     * @param <OUT>    the type parameter
     * @param conveyor the conveyor
     * @return the consumer for
     */
    static <L,OUT> LabeledValueConsumer<L, ?, Supplier<? extends OUT>> getConsumerFor(Conveyor<?, L, OUT> conveyor) {
		return (l,v,b)->{
			throw new IllegalStateException("undefined behavior for label '"+l+"'"+" value='"+v+"'");
		};
	}

    /**
     * Gets consumer for.
     *
     * @param <L>      the type parameter
     * @param <OUT>    the type parameter
     * @param <B>      the type parameter
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
     * Gets cart counter.
     *
     * @return the cart counter
     */
    long getCartCounter();

    /**
     * Sets auto acknowledge.
     *
     * @param auto the auto
     */
    void setAutoAcknowledge(boolean auto);

    /**
     * Sets acknowledge action.
     *
     * @param ackAction the ack action
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
     * Interrupt.
     *
     * @param conveyorName the conveyor name
     * @param key          the key
     */
    void interrupt(String conveyorName, K key);

    /**
     * Kill completable future.
     *
     * @param id the id
     * @return the completable future
     */
    default CompletableFuture<Boolean> kill(K id) {
		LOG.debug("Build for id={} is about to be killed.",id);
		CompletableFuture<Boolean> cancel = this.command().id(id).cancel();
		this.interrupt(this.getName(),id);
		return cancel;
	}

    /**
     * Sets cart payload accessor.
     *
     * @param payloadFunction the payload function
     */
    void setCartPayloadAccessor(Function<Cart<K,?,L>,Object> payloadFunction);

    /**
     * Suspend.
     */
    void suspend();

    /**
     * Resume.
     */
    void resume();

    /**
     * Is suspended boolean.
     *
     * @return the boolean
     */
    boolean isSuspended();

    /**
     * M bean interface class.
     *
     * @return the class
     */
    Class<?> mBeanInterface();

    /**
     * Gets meta info.
     *
     * @return the meta info
     */
    ConveyorMetaInfo<K,L,OUT> getMetaInfo();

    /**
     * Gets enclosing conveyor.
     *
     * @return the enclosing conveyor
     */
    Conveyor<?,?,?> getEnclosingConveyor();

    /**
     * Sets enclosing conveyor.
     *
     * @param conveyor the conveyor
     */
    void setEnclosingConveyor(Conveyor<?,?,?> conveyor);

    /**
     * By name conveyor.
     *
     * @param name the name
     * @return the conveyor
     */
    static Conveyor byName(String name) {
		return MBEAN.byName(name);
	}

    /**
     * Gets m bean instance.
     *
     * @param name the name
     * @return the m bean instance
     */
    default Object getMBeanInstance(String name) {
		return MBEAN.getMBeanInstance(name, mBeanInterface());
	}

    /**
     * Lazy supplier supplier.
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
     * Un register tree.
     *
     * @param name the name
     */
    static void unRegisterTree(String name) {
        Map<String, ?> children = getKnownConveyorNameTree().get(name);
        LOG.info("Unregister "+name);
        MBEAN.unRegister(name);
        if(children != null && ! children.isEmpty()) {
            children.keySet().forEach(Conveyor::unRegisterTree);
        }
    }

    /**
     * Un register.
     */
    default void unRegister() {
		unRegister(this.getName());
	}

    /**
     * Load services.
     */
    static void loadServices() {
		SERVICES.reload();
		SERVICES.getLoadedConveyorNames();
	}

    /**
     * Gets known conveyor names.
     *
     * @return the known conveyor names
     */
    static Set<String> getKnownConveyorNames() {
		loadServices();
		var set = new HashSet<>(MBEAN.getKnownConveyorNames());
		set.addAll(MBEAN.getRegisteredConveyorNames());
		return Collections.unmodifiableSet(set);
	}

    /**
     * Gets registered conveyor names.
     *
     * @return the registered conveyor names
     */
    static Set<String> getRegisteredConveyorNames() {
		return Collections.unmodifiableSet(new HashSet<>(MBEAN.getRegisteredConveyorNames()));
	}

    /**
     * Gets known conveyor name tree.
     *
     * @return the known conveyor name tree
     */
    static Map<String, Map<String, ?>> getKnownConveyorNameTree() {
		var knownNames = getKnownConveyorNames();
		var parentByChild = new HashMap<String, String>();
		for (String name : knownNames) {
			try {
				var conveyor = byName(name);
				var enclosingConveyor = conveyor.getEnclosingConveyor();
				if (enclosingConveyor != null) {
					var parentName = enclosingConveyor.getName();
					if (parentName != null && knownNames.contains(parentName) && !parentName.equals(name)) {
						parentByChild.put(name, parentName);
					}
				}
			} catch (Exception ignored) {
				// Skip names that cannot be resolved to a conveyor instance.
			}
		}
		var childrenByParent = new HashMap<String, List<String>>();
		for (Map.Entry<String, String> entry : parentByChild.entrySet()) {
			childrenByParent.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
		}
		childrenByParent.values().forEach(Collections::sort);
		var sortedNames = new ArrayList<>(knownNames);
		Collections.sort(sortedNames);
		var roots = sortedNames.stream()
				.filter(name -> !parentByChild.containsKey(name))
				.toList();
		var visited = new HashSet<String>();
		var tree = new LinkedHashMap<String, Map<String, ?>>();
		for (String root : roots) {
			tree.put(root, toSubTree(root, childrenByParent, new HashSet<>(), visited));
		}
		for (String name : sortedNames) {
			if (!visited.contains(name)) {
				tree.put(name, toSubTree(name, childrenByParent, new HashSet<>(), visited));
			}
		}
		return Collections.unmodifiableMap(tree);
	}

	private static Map<String, ?> toSubTree(
			String name,
			Map<String, List<String>> childrenByParent,
			Set<String> recursionPath,
			Set<String> visited
	) {
		if (!recursionPath.add(name)) {
			return Map.of();
		}
		visited.add(name);
		var childrenTree = new LinkedHashMap<String, Map<String, ?>>();
		for (String childName : childrenByParent.getOrDefault(name, List.of())) {
			childrenTree.put(childName, toSubTree(childName, childrenByParent, new HashSet<>(recursionPath), visited));
		}
		return Collections.unmodifiableMap(childrenTree);
	}

    /**
     * Gets loaded conveyor names.
     *
     * @return the loaded conveyor names
     */
    static Set<String> getLoadedConveyorNames() {
		return Collections.unmodifiableSet(SERVICES.getLoadedConveyorNames());
	}

    /**
     * Gets loaded conveyor services.
     *
     * @return the loaded conveyor services
     */
    static List<ConveyorInitiatingService> getLoadedConveyorServices() {
		return Collections.unmodifiableList(SERVICES.getLoadedConveyorServices());
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

}
