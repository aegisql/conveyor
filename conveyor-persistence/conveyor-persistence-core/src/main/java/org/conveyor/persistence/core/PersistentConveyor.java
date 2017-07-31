package org.conveyor.persistence.core;

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

import org.conveyor.persistence.ack.AcknowledgeBuilder;
import org.conveyor.persistence.ack.AcknowledgeBuildingConveyor;
import org.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.MultiKeyCart;
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

public class PersistentConveyor<I,K,L,OUT> implements Conveyor<K, L, OUT> {

	
	private final Conveyor<K, L, OUT> forward;
	private final AcknowledgeBuildingConveyor<I, K> ackConveyor;
	private final PersistenceCleanupBatchConveyor<K, I> cleaner;
	
	public PersistentConveyor(Persist<K,I> persistence, Conveyor<K, L, OUT> forward, int batchSize) {
		this.forward = forward;
		this.cleaner = new PersistenceCleanupBatchConveyor<>(persistence,batchSize);
		this.ackConveyor = new AcknowledgeBuildingConveyor<>(persistence, forward, cleaner);
		forward.addBeforeKeyEvictionAction((k,status)->{
			ackConveyor.part().id(k).label(ackConveyor.COMPLETE).value(k).place();
		});
		//not empty only if previous conveyor could not complete.
		//Pers must be initialized with the previous state
		persistence.getAllCarts().forEach(cart->this.place((Cart<K, ?, L>) cart));
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FutureLoader<K, OUT> future() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommandLoader<K, OUT> command() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
		ShoppingCart<K, Cart, SmartLabel<AcknowledgeBuilder<K, I>>> ackCart = new ShoppingCart<>(cart.getKey(), cart, ackConveyor.CART);
		CompletableFuture<Boolean> forwardFuture = cart.getFuture();
		CompletableFuture<Boolean> ackFuture = ackCart.getFuture();
		CompletableFuture<Boolean> bothFutures = ackFuture.thenCombine(forwardFuture,(a,b)->a&&b);
		ackConveyor.place(ackCart);
		return bothFutures;
	}

	@Override
	public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return forward.resultConsumer();
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBeforeKeyEvictionAction(BiConsumer<K,Status> keyBeforeEviction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
		// TODO Auto-generated method stub
		
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
		//TODO add others too
	}

	@Override
	public void setExpirationPostponeTime(Duration duration) {
		// TODO Auto-generated method stub
		
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

}
