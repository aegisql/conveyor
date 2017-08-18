package com.aegisql.conveyor.persistence.core;

import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
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
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;

public class PersistentConveyor<K,L,OUT> implements Conveyor<K, L, OUT> {

	
	private final Conveyor<K, L, OUT> forward;
	private final AcknowledgeBuildingConveyor<K> ackConveyor;
	private final PersistenceCleanupBatchConveyor<K> cleaner;
	private final AcknowledgeBuilder<K> staticAcknowledgeBuilder;
	private ResultConsumer<K,OUT> resultConsumer = bin->{};
	
	private final AtomicBoolean initializationMode = new AtomicBoolean(true);
	
	public PersistentConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward, int batchSize) {
		
		Persistence<K> ackPersistence     = persistence.copy();
		Persistence<K> forwardPersistence = persistence.copy();
		Persistence<K> cleanPersistence   = persistence.copy();
		Persistence<K> staticPersistence  = persistence.copy();
		
		this.forward = forward;
		this.cleaner = new PersistenceCleanupBatchConveyor<>(cleanPersistence,batchSize);
		this.ackConveyor = new AcknowledgeBuildingConveyor<>(ackPersistence, forward, cleaner);
		forward.setAcknowledgeAction((k,status)->{
			forwardPersistence.saveCompletedBuildKey(k);
			ackConveyor.part().id(k).label(ackConveyor.COMPLETE).value(status).place();
		});
		this.ackConveyor.staticPart().label(ackConveyor.MODE).value(true).place();
		if(forward != null && forward.getResultConsumer() != null) {
			this.resultConsumer = forward.getResultConsumer();
		} else {
			this.resultConsumer = bin->{};
		}
		this.staticAcknowledgeBuilder = new AcknowledgeBuilder<>(staticPersistence, forward);
		//not empty only if previous conveyor could not complete.
		//Pers must be initialized with the previous state
		Collection<Cart<K,?,L>> staticParts = persistence.<L>getAllStaticParts();
		LOG.debug("Static parts: {}",staticParts);
		Collection<Cart<K,?,L>> allParts = persistence.<L>getAllParts();
		LOG.debug("All parts: {}",allParts);
		staticParts.forEach(cart->this.place(cart));
		allParts.forEach(cart->this.place(cart));
		try {
			persistence.close();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		this.initializationMode.set(false);
		this.ackConveyor.setInitializationMode(false);
		this.ackConveyor.staticPart().label(ackConveyor.MODE).value(false).place();
	}
	
	public PersistentConveyor(Persistence<K> persistence, int batchSize) {
		this(persistence,new AssemblingConveyor<>(),batchSize);
	}
	
	public PersistentConveyor(Persistence<K> persistence, Supplier<Conveyor<K, L, OUT>> forwardSupplier, int batchSize) {
		this(persistence,forwardSupplier.get(),batchSize);
	}	
	
	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		return new PartLoader<K, L, X, OUT, Boolean>(cl -> {
			Cart <K, Object, L> cart;
			if(cl.filter != null) {
				cart = new MultiKeyCart<K, Object, L>(cl.filter, cl.partValue, cl.label, cl.creationTime, cl.expirationTime);
			} else {
				cart = new ShoppingCart<K, Object, L>(cl.key, cl.partValue, cl.label,cl.creationTime ,cl.expirationTime);
			}
			cl.getAllProperties().forEach((k,v)->cart.addProperty(k, v));

			return place(cart);
		});	}

	@Override
	public <X> StaticPartLoader<L, X, OUT, Boolean> staticPart() {
		return  new StaticPartLoader<L, X, OUT, Boolean>(cl -> {
			Map<String,Object> properties = new HashMap<>();
			properties.put("CREATE", cl.create);
			Cart<K,?,L> staticPart = new ShoppingCart<>(null, cl.staticPartValue, cl.label, System.currentTimeMillis(), 0, properties, STATIC_PART);
			return place(staticPart);
		});
	}

	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		return new BuilderLoader<K, OUT, Boolean>(cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			if(bs == null) {
				//bs = builderSupplier;
			}
			final CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs, cl.creationTime, cl.expirationTime);
			cl.getAllProperties().forEach((k,v)->{cart.addProperty(k, v);});
			return place(cart);
		},
		cl -> {
			throw new RuntimeException("Futures not supported in persistent builde suppliers");
		}
		);
	}

	@Override
	public FutureLoader<K, OUT> future() {
		return forward.future();
	}

	@Override
	public CommandLoader<K, OUT> command() {
		return forward.command();
	}

	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		if(cart.getKey() == null) {
			AcknowledgeBuilder.processCart(staticAcknowledgeBuilder, cart);
			cart.getFuture().complete(true);
			return cart.getFuture();
		} 
		if(cart.getLoadType() == LoadType.RESULT_CONSUMER) {
			AcknowledgeBuilder.processCart(staticAcknowledgeBuilder, cart);
			cart.getFuture().complete(true);
			return cart.getFuture();
		}
		Cart<K, Cart<K,?,?>, SmartLabel<AcknowledgeBuilder<K>>> ackCart = PersistenceCart.of(cart, ackConveyor.CART);
		LOG.debug("PLACING "+ackCart);
		CompletableFuture<Boolean> forwardFuture = cart.getFuture();
		CompletableFuture<Boolean> ackFuture = ackCart.getFuture();
		CompletableFuture<Boolean> bothFutures = ackFuture.thenCombine(forwardFuture,(a,b)->a&&b);
		ackConveyor.place(ackCart);
		if(cart.getLoadType()==LoadType.STATIC_PART) {
			return ackFuture;
		} else {
			return bothFutures;
		}
	}

	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		return forward.command(command);
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return new ResultConsumerLoader<>(rcl->{
			final Cart<K,?,L> cart;
			if(rcl.key != null) {
				cart = new ResultConsumerCart<K, OUT, L>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime, k->{
					return new ResultConsumerCart<K, OUT, L>(k, rcl.consumer, rcl.creationTime, rcl.expirationTime);
				});
			}
			rcl.getAllProperties().forEach((k,v)->{ cart.addProperty(k, v);});
			return this.place(cart);
		}, 
		rc->{
			this.resultConsumer = rc;
			this.forward.resultConsumer(rc).set();
		}, 
		resultConsumer );
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer) {
		return forward.resultConsumer(consumer);
	}

	@Override
	public int getCollectorSize() {
		return ackConveyor.getCollectorSize();
	}

	@Override
	public int getInputQueueSize() {
		return ackConveyor.getInputQueueSize();
	}

	@Override
	public int getDelayedQueueSize() {
		return ackConveyor.getDelayedQueueSize();
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer() {
		return forward.scrapConsumer();
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {
		return forward.scrapConsumer(scrapConsumer);
	}

	@Override
	public void stop() {
		forward.stop();
		ackConveyor.stop();
		cleaner.stop();
	}

	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		CompletableFuture<Boolean> fFuture = forward.completeAndStop();
		CompletableFuture<Boolean> aFuture = ackConveyor.completeAndStop();
		CompletableFuture<Boolean> cFuture = cleaner.completeAndStop();
		return fFuture.thenCombine(aFuture, (a,b)->a&&b).thenCombine(cFuture, (a,b)->a&&b);
	}

	@Override
	public void setIdleHeartBeat(long heartbeat, TimeUnit unit) {
		forward.setIdleHeartBeat(heartbeat,unit);
		ackConveyor.setIdleHeartBeat(heartbeat,unit);
		cleaner.setIdleHeartBeat(heartbeat,unit);
	}

	@Override
	public void setIdleHeartBeat(Duration duration) {
		forward.setIdleHeartBeat(duration);
		ackConveyor.setIdleHeartBeat(duration);
		cleaner.setIdleHeartBeat(duration);
	}

	@Override
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		forward.setDefaultBuilderTimeout(builderTimeout, unit);
	}

	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		forward.setDefaultBuilderTimeout(duration);
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		forward.rejectUnexpireableCartsOlderThan(timeout, unit);
		ackConveyor.rejectUnexpireableCartsOlderThan(timeout, unit);
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		forward.rejectUnexpireableCartsOlderThan(duration);
		ackConveyor.rejectUnexpireableCartsOlderThan(duration);		
	}

	@Override
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		forward.setOnTimeoutAction(timeoutAction);
	}

	@Override
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
		forward.setDefaultCartConsumer(cartConsumer);
	}

	@Override
	public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready) {
		forward.setReadinessEvaluator(ready);
	}

	@Override
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		forward.setReadinessEvaluator(readiness);
	}

	@Override
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		forward.setBuilderSupplier(builderSupplier);
	}

	@Override
	public void setName(String string) {
		forward.setName(string);
		ackConveyor.setName("AcknowledgeBuildingConveyor<"+string+">");
		cleaner.setName("PersistenceCleanupBatchConveyor<"+string+">");
	}

	@Override
	public boolean isRunning() {
		return forward.isRunning() && ackConveyor.isRunning() && cleaner.isRunning();
	}

	@Override
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		forward.addCartBeforePlacementValidator(cartBeforePlacementValidator);
	}

	@Override
	public void addBeforeKeyEvictionAction(BiConsumer<K,Status> keyBeforeEviction) {
		forward.addBeforeKeyEvictionAction(keyBeforeEviction);
	}

	@Override
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		forward.addBeforeKeyReschedulingAction(keyBeforeRescheduling);
	}

	@Override
	public long getExpirationTime(K key) {
		return forward.getExpirationTime(key);
	}

	@Override
	public boolean isLBalanced() {
		return forward.isLBalanced();
	}

	@Override
	public Set<L> getAcceptedLabels() {
		return forward.getAcceptedLabels();
	}

	@Override
	public void acceptLabels(L... labels) {
		forward.acceptLabels(labels);
	}

	@Override
	public String getName() {
		return "PersistentConveyor<"+forward.getName()+">";
	}

	@Override
	public <L2, OUT2> void forwardResultTo(Conveyor<K, L2, OUT2> destination, L2 label) {
		forward.forwardResultTo(destination, label);
	}

	@Override
	public <K2, L2, OUT2> void forwardResultTo(Conveyor<K2, L2, OUT2> destination,
			Function<ProductBin<K, OUT>, K2> keyConverter, L2 label) {
		forward.forwardResultTo(destination, keyConverter, label);
	}

	@Override
	public void enablePostponeExpiration(boolean flag) {
		forward.enablePostponeExpiration(flag);
	}

	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		forward.enablePostponeExpirationOnTimeout(flag);
	}

	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		forward.setExpirationPostponeTime(time, unit);
	}

	@Override
	public void setExpirationPostponeTime(Duration duration) {
		forward.setExpirationPostponeTime(duration);
	}

	@Override
	public boolean isForwardingResults() {
		return forward.isForwardingResults();
	}

	@Override
	public long getCartCounter() {
		return forward.getCartCounter();
	}

	@Override
	public void setAutoAcknowledge(boolean auto) {
		forward.setAutoAcknowledge(auto);
	}

	@Override
	public void setAcknowledgeAction(BiConsumer<K, Status> ackAction) {
		forward.setAcknowledgeAction(ackAction);
	}
	
	public Conveyor<K, L, OUT> getWorkingConveyor() {
		return forward;
	}
	
	public AcknowledgeBuildingConveyor<K> getAcknowledgeBuildingConveyor() {
		return ackConveyor;
	}
	
	public PersistenceCleanupBatchConveyor<K> getCleaningConveyor() {
		return cleaner;
	}

	@Override
	public void autoAcknowledgeOnStatus(Status first, Status... other) {
		forward.autoAcknowledgeOnStatus(first, other);
	}

	@Override
	public ResultConsumer<K, OUT> getResultConsumer() {
		return resultConsumer;
	}

}
