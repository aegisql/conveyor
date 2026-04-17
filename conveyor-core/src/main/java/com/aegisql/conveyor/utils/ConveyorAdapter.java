package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.*;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.*;

public abstract class ConveyorAdapter<K, L, OUT> implements Conveyor<K, L, OUT> {

    protected final Conveyor<K, L, OUT> innerConveyor;

    private String publicName;
    private String hiddenInnerName;
    private Conveyor<?,?,?> enclosingConveyor;
    private String generic = null;

    public ConveyorAdapter(Conveyor<K, L, OUT> innerConveyor) {
        this(innerConveyor, true);
    }

    protected ConveyorAdapter(Conveyor<K, L, OUT> innerConveyor, boolean claimWrappedName) {
        Objects.requireNonNull(innerConveyor,"conveyor instance is required");
        this.innerConveyor = innerConveyor;
        if (claimWrappedName) {
            claimPublicName(innerConveyor.getName(), true);
        }
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
    public final void setName(String string) {
        claimPublicName(string, false);
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
        return publicName != null ? publicName : innerConveyor.getName();
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
    public void setEnclosingConveyor(Conveyor<?,?,?> conveyor) {
        this.enclosingConveyor = conveyor;
    }

    @Override
    public Object getMBeanInstance(String name) {
        return Conveyor.super.getMBeanInstance(name);
    }

    @Override
    public void register(Object mbeanObject) {
        Conveyor.super.register(mbeanObject);
    }

    @Override
    public void unRegister() {
        Conveyor.super.unRegister();
        try {
            innerConveyor.unRegister();
        } catch (Exception ignored) {
            // Hidden inner conveyor may already be unregistered.
        }
    }

    @Override
    public void setInactiveEvictionAction(int maxCollectorSize, Consumer<CommandLoader.EvictionCommand<K>> action) {
        innerConveyor.setInactiveEvictionAction(maxCollectorSize,action);
    }

    @Override
    public Conveyor<?,?,?> getEnclosingConveyor() {
        return enclosingConveyor;
    }

    @Override
    public String toString() {
        if(generic == null) {
            try {
                generic = getMetaInfo().generic();
            } catch (Exception e) {
                generic = "<?,?,?>";
            }
        }
        return innerConveyor.toString().replaceFirst("<\\?,\\?,\\?>",generic);
    }

    public Conveyor<K,L,OUT> unwrap() {
        return innerConveyor;
    }

    protected void setMbean(String name) {
        registerMbean(name, mBeanInterface());
    }

    protected String publicAdapterName(String requestedName) {
        return requestedName;
    }

    protected String innerConveyorName(String requestedName, String publicAdapterName) {
        return hiddenName(publicAdapterName);
    }

    protected void bindInnerConveyor(String requestedName, String publicAdapterName, String innerConveyorName) {
        innerConveyor.setName(innerConveyorName);
        innerConveyor.setEnclosingConveyor(this);
    }

    protected void renameOwnedConveyors(String requestedName, String publicAdapterName, String innerConveyorName) {
        // Default adapter owns only the wrapped conveyor.
    }

    private void claimPublicName(String name, boolean constructorPhase) {
        Objects.requireNonNull(name, "conveyor name is required");
        if (constructorPhase && this.enclosingConveyor == null) {
            this.enclosingConveyor = innerConveyor.getEnclosingConveyor();
        }
        String oldPublicName = this.publicName;
        this.publicName = publicAdapterName(name);
        this.hiddenInnerName = innerConveyorName(name, this.publicName);

        if (!constructorPhase && oldPublicName != null) {
            try {
                Conveyor.unRegister(oldPublicName);
            } catch (Exception ignored) {
                // Adapter may already be unregistered under the old public name.
            }
        }

        bindInnerConveyor(name, publicName, hiddenInnerName);
        renameOwnedConveyors(name, publicName, hiddenInnerName);

        if (constructorPhase) {
            registerMbean(publicName, innerConveyor.mBeanInterface());
        } else {
            setMbean(publicName);
        }
    }

    private String hiddenName(String name) {
        return name + "#" + Integer.toUnsignedString(System.identityHashCode(this));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerMbean(String name, Class<?> mBeanInterface) {
        if (mBeanInterface == null) {
            Conveyor.register(this, null);
            return;
        }

        final Object hiddenMBean = innerConveyor.getMBeanInstance(hiddenInnerName);
        final Conveyor<K, L, OUT> thisConveyor = this;
        final String adapterName = this.publicName != null ? this.publicName : name;
        final String adapterType = getClass().getSimpleName();

        Object proxy = Proxy.newProxyInstance(
                mBeanInterface.getClassLoader(),
                new Class[]{mBeanInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxyInstance, Method method, Object[] args) throws Throwable {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "ConveyorAdapterMBeanProxy[" + adapterName + "]";
                                case "hashCode" -> System.identityHashCode(proxyInstance);
                                case "equals" -> proxyInstance == args[0];
                                default -> method.invoke(this, args);
                            };
                        }

                        if ("conveyor".equals(method.getName()) && method.getParameterCount() == 0) {
                            return thisConveyor;
                        }
                        if ("getName".equals(method.getName()) && method.getParameterCount() == 0
                                && method.getReturnType() == String.class) {
                            return adapterName;
                        }
                        if ("getType".equals(method.getName()) && method.getParameterCount() == 0
                                && method.getReturnType() == String.class && adapterType != null && !adapterType.isBlank()) {
                            return adapterType;
                        }

                        try {
                            return method.invoke(hiddenMBean, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    }
                }
        );

        Conveyor.register(this, proxy);
    }

}
