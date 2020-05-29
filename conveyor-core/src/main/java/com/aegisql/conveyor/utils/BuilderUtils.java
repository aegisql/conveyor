package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.TestingState;
import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.java_path.PathElement;

import java.util.Objects;
import java.util.function.*;

public class BuilderUtils<K, L, OUT, B> {


    public class ProductSupplierSimple implements Supplier<OUT>, Wrapped<B>, TimeoutAction {
        Supplier<B> builderSupplier;
        B builder;

        ProductSupplierSimple(B pb) {
            this.builder = pb;
        }

        @Override
        public OUT get() {
            return productSupplier.apply(builder);
        }

        @PathElement({"instance","#","#0"})
        @Override
        public B unwrap() {
            return builder;
        }

        @Override
        public void onTimeout() {
            timeoutActon.accept(builder);
        }
    }

    private class ProductSupplierTestingState extends ProductSupplierSimple implements TestingState<K, L> {
        ProductSupplierTestingState(B pb) {
            super(pb);
        }

        @Override
        public boolean test(State<K, L> state) {
            return stateTester.test(state, this.unwrap());
        }
    }

    private final Conveyor<K, L, OUT> conveyor;
    private final Supplier<B> builderSupplier;
    private final Function<B, OUT> productSupplier;
    private final BiPredicate<State<K, L>, B> stateTester;
    private final BiPredicate<State<K, L>, B> timeoutStateTester;
    private final Consumer<B> timeoutActon;

    private BuilderUtils(
            Conveyor<K, L, OUT> conveyor,
            Supplier<B> builderSupplier,
            Function<B, OUT> productSupplier,
            BiPredicate<State<K, L>, B> stateTester,
            BiPredicate<State<K, L>, B> timeoutStateTester,
            Consumer<B> timeoutActon
    ) {
        this.conveyor = conveyor;
        this.builderSupplier = builderSupplier;
        this.productSupplier = productSupplier;
        this.stateTester = stateTester;
        this.timeoutStateTester = timeoutStateTester;
        this.timeoutActon = timeoutActon;
    }

    public static <K, L, OUT, B> BuilderUtils<K, L, OUT, B> wrapBuilderSupplier(Conveyor<K, L, OUT> conveyor, Supplier<B> bs) {
        Objects.requireNonNull(conveyor, "Conveyor instance required");
        Objects.requireNonNull(bs, "Supplier instance required");
        return new BuilderUtils<>(conveyor, bs, null, null, null, b->{});
    }

    public BuilderUtils<K, L, OUT, B> productSupplier(Function<B, OUT> ps) {
        return new BuilderUtils<>(conveyor, builderSupplier, ps, stateTester, timeoutStateTester, timeoutActon);
    }

    public BuilderUtils<K, L, OUT, B> tester(Predicate<B> tester) {
        return tester((state, b) -> tester.test(b));
    }

    public BuilderUtils<K, L, OUT, B> tester(BiPredicate<State<K, L>, B> stTester) {
        BiPredicate<State<K, L>, B> newTester = null;
        if (timeoutStateTester != null) {
            newTester = (state, b) -> {
                boolean expired = state.builderExpiration > 0 && state.builderExpiration - System.currentTimeMillis() <= 0;
                if (expired) {
                    return stTester.test(state, b) || timeoutStateTester.test(state, b);
                } else {
                    return stTester.test(state, b);
                }
            };
        } else {
            newTester = stTester;
        }
        return new BuilderUtils<>(conveyor, builderSupplier, productSupplier, newTester, timeoutStateTester, timeoutActon);
    }

    public BuilderUtils<K, L, OUT, B> testerOnTimeout(Predicate<B> tTester) {
        return testerOnTimeout((state,b)->tTester.test(b));
    }

    public BuilderUtils<K, L, OUT, B> testerOnTimeout(BiPredicate<State<K, L>, B> tStTester) {
        BiPredicate<State<K, L>, B> newTester = stateTester;
        if (newTester != null) {
            newTester = (state, b) -> {
                boolean expired = state.builderExpiration > 0 && state.builderExpiration - System.currentTimeMillis() <= 0;
                if (expired) {
                    return tStTester.test(state, b) || stateTester.test(state, b);
                } else {
                    return stateTester.test(state, b);
                }
            };
        }
        return new BuilderUtils<>(conveyor, builderSupplier, productSupplier, newTester, tStTester, timeoutActon);
    }

    public BuilderUtils<K, L, OUT, B> onTimeout(Consumer<B> timeoutActon) {
        return new BuilderUtils<>(conveyor, builderSupplier, productSupplier, stateTester, timeoutStateTester, timeoutActon);
    }

    public void setBuilderSupplier() {
        Objects.requireNonNull(productSupplier, "Product Supplier instance required");
        if (stateTester != null) {
            conveyor.setBuilderSupplier(() -> new ProductSupplierTestingState(builderSupplier.get()));
        } else {
            conveyor.setBuilderSupplier(() -> new ProductSupplierSimple(builderSupplier.get()));
        }
    }
}
