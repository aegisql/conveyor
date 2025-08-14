package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public abstract class ConveyorAdapter<K, L, OUT> implements Conveyor<K, L, OUT> {

    protected final Conveyor<K, L, OUT> innerConveyor;

    public ConveyorAdapter(Conveyor<K, L, OUT> innerConveyor) {
        this.innerConveyor = innerConveyor;
    }

    @Override
    public PartLoader<K, L> part() {
        return innerConveyor.part();
    }

    @Override
    public StaticPartLoader<L> staticPart() {
        return innerConveyor.staticPart();
    }

    @Override
    public BuilderLoader<K, OUT> build() {
        return innerConveyor.build();
    }

    @Override
    public FutureLoader<K, OUT> future() {
        return innerConveyor.future();
    }

    @Override
    public CommandLoader<K, OUT> command() {
        return innerConveyor.command();
    }

    @Override
    public <V> CompletableFuture<Boolean> place(Cart<K, V, L> cart) {
        return innerConveyor.place(cart);
    }

    @Override
    public <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> command) {
        return innerConveyor.command(command);
    }

    @Override
    public ResultConsumerLoader<K, OUT> resultConsumer() {
        return innerConveyor.resultConsumer();
    }

    @Override
    public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K, OUT> consumer) {
        return innerConveyor.resultConsumer(consumer);
    }

    @Override
    public ResultConsumer<K, OUT> getResultConsumer() {
        return innerConveyor.getResultConsumer();
    }

    @Override
    public int getCollectorSize() {
        return innerConveyor.getCollectorSize();
    }

    @Override
    public int getInputQueueSize() {
        return innerConveyor.getInputQueueSize();
    }

    @Override
    public int getDelayedQueueSize() {
        return innerConveyor.getDelayedQueueSize();
    }

    @Override
    public ScrapConsumerLoader<K> scrapConsumer() {
        return innerConveyor.scrapConsumer();
    }

    @Override
    public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K, ?> scrapConsumer) {
        return innerConveyor.scrapConsumer(scrapConsumer);
    }

    @Override
    public void stop() {
        innerConveyor.stop();
    }

    @Override
    public CompletableFuture<Boolean> completeAndStop() {
        return innerConveyor.completeAndStop();
    }

    @Override
    public void setIdleHeartBeat(long heartbeat, TimeUnit unit) {
        innerConveyor.setIdleHeartBeat(heartbeat, unit);
    }

    @Override
    public void setIdleHeartBeat(Duration duration) {
        innerConveyor.setIdleHeartBeat(duration);
    }

    @Override
    public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
        innerConveyor.setDefaultBuilderTimeout(builderTimeout, unit);
    }

    @Override
    public void setDefaultBuilderTimeout(Duration duration) {
        innerConveyor.setDefaultBuilderTimeout(duration);
    }

    @Override
    public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
        innerConveyor.rejectUnexpireableCartsOlderThan(timeout, unit);
    }

    @Override
    public void rejectUnexpireableCartsOlderThan(Duration duration) {
        innerConveyor.rejectUnexpireableCartsOlderThan(duration);
    }

    @Override
    public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
        innerConveyor.setOnTimeoutAction(timeoutAction);
    }

    @Override
    public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
        innerConveyor.setDefaultCartConsumer(cartConsumer);
    }

    @Override
    public void setReadinessEvaluator(BiPredicate<State<K, L>, Supplier<? extends OUT>> ready) {
        innerConveyor.setReadinessEvaluator(ready);
    }

    @Override
    public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
        innerConveyor.setReadinessEvaluator(readiness);
    }

    @Override
    public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
        innerConveyor.setBuilderSupplier(builderSupplier);
    }

    @Override
    public void setName(String string) {
        innerConveyor.setName(string);
    }

    @Override
    public boolean isRunning() {
        return innerConveyor.isRunning();
    }

    @Override
    public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
        innerConveyor.addCartBeforePlacementValidator(cartBeforePlacementValidator);
    }

    @Override
    public void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction) {
        innerConveyor.addBeforeKeyEvictionAction(keyBeforeEviction);
    }

    @Override
    public void addBeforeKeyReschedulingAction(BiConsumer<K, Long> keyBeforeRescheduling) {
        innerConveyor.addBeforeKeyReschedulingAction(keyBeforeRescheduling);
    }

    @Override
    public long getExpirationTime(K key) {
        return innerConveyor.getExpirationTime(key);
    }

    @Override
    public boolean isLBalanced() {
        return innerConveyor.isLBalanced();
    }

    @Override
    public Set<L> getAcceptedLabels() {
        return innerConveyor.getAcceptedLabels();
    }

    @Override
    public void acceptLabels(L... labels) {
        innerConveyor.acceptLabels(labels);
    }

    @Override
    public String getName() {
        return innerConveyor.getName();
    }

    @Override
    public String getGenericName() {
        return innerConveyor.getGenericName();
    }

    @Override
    public void enablePostponeExpiration(boolean flag) {
        innerConveyor.enablePostponeExpiration(flag);
    }

    @Override
    public void enablePostponeExpirationOnTimeout(boolean flag) {
        innerConveyor.enablePostponeExpirationOnTimeout(flag);
    }

    @Override
    public void setExpirationPostponeTime(long time, TimeUnit unit) {
        innerConveyor.setExpirationPostponeTime(time, unit);
    }

    @Override
    public void setExpirationPostponeTime(Duration duration) {
        innerConveyor.setExpirationPostponeTime(duration);
    }

    @Override
    public boolean isForwardingResults() {
        return innerConveyor.isForwardingResults();
    }

    @Override
    public long getCartCounter() {
        return innerConveyor.getCartCounter();
    }

    @Override
    public void setAutoAcknowledge(boolean auto) {
        innerConveyor.setAutoAcknowledge(auto);
    }

    @Override
    public void setAcknowledgeAction(Consumer<AcknowledgeStatus<K>> ackAction) {
        innerConveyor.setAcknowledgeAction(ackAction);
    }

    @Override
    public void autoAcknowledgeOnStatus(Status first, Status... other) {
        innerConveyor.autoAcknowledgeOnStatus(first, other);
    }

    @Override
    public void interrupt(String conveyorName) {
        innerConveyor.interrupt(conveyorName);
    }

    @Override
    public void interrupt(String conveyorName, K key) {
        innerConveyor.interrupt(conveyorName, key);
    }

    @Override
    public void setCartPayloadAccessor(Function<Cart<K, ?, L>, Object> payloadFunction) {
        innerConveyor.setCartPayloadAccessor(payloadFunction);
    }

    @Override
    public void suspend() {
        innerConveyor.suspend();
    }

    @Override
    public void resume() {
        innerConveyor.resume();
    }

    @Override
    public boolean isSuspended() {
        return innerConveyor.isSuspended();
    }

    @Override
    public Class<?> mBeanInterface() {
        return innerConveyor.mBeanInterface();
    }

    @Override
    public ConveyorMetaInfo<K, L, OUT> getMetaInfo() {
        return innerConveyor.getMetaInfo();
    }

    @Override
    public Object getMBeanInstance(String name) {
        return innerConveyor.getMBeanInstance(name);
    }

    @Override
    public void register(Object mbeanObject) {
        innerConveyor.register(mbeanObject);
    }

    @Override
    public void unRegister() {
        innerConveyor.unRegister();
    }

    @Override
    public void setInactiveEvictionAction(int maxCollectorSize, Consumer<CommandLoader.EvictionCommand<K>> action) {
        innerConveyor.setInactiveEvictionAction(maxCollectorSize,action);
    }

}