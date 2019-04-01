package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BuilderUtils<K,L,OUT,B> {



    private class ProductSupplierSimple implements Supplier<OUT>, Wrapped<B> {
        Supplier<B> builderSupplier;
        B builder;
        ProductSupplierSimple(B pb) {
            this.builder = pb;
        }
        @Override
        public OUT get() {
            return productSupplier.apply(builder);
        }
        @Override
        public B unwrap() {
            return builder;
        }
    }

    private class ProductSupplierTesting extends ProductSupplierSimple implements Testing {
        ProductSupplierTesting(B pb) {
            super(pb);
        }
        @Override
        public boolean test() {
            return testingPredicate.test(builder);
        }
    }

    private class ProductSupplierTestingState extends ProductSupplierSimple implements TestingState<K,L> {
        ProductSupplierTestingState(B pb) {
            super(pb);
        }

        @Override
        public boolean test(State<K, L> state) {
            return stateTester.test(state,this.unwrap());
        }
    }

    private final Conveyor<K, L, OUT> conveyor;
    private final Supplier<B> builderSupplier;
    private final Function<B,OUT> productSupplier;
    private final Predicate<B> testingPredicate;
    private final BiPredicate<State<K,L>, B> stateTester;

    private BuilderUtils(
            Conveyor<K, L, OUT> conveyor,
            Supplier<B> builderSupplier,
            Function<B,OUT> productSupplier,
            Predicate<B> testingPredicate,
            BiPredicate<State<K,L>, B> stateTester
    ) {
        this.conveyor = conveyor;
        this.builderSupplier = builderSupplier;
        this.productSupplier = productSupplier;
        this.testingPredicate = testingPredicate;
        this.stateTester = stateTester;
    }

    public static <K,L,OUT> BuilderUtils<K,L,OUT,?> forConveyor(Conveyor<K,L,OUT> conveyor) {
        return new BuilderUtils<>(conveyor,null,null,null, null);
    }

    public <B> BuilderUtils<K,L,OUT,B> builderSupplier(Supplier<B> bs) {
        return new BuilderUtils(conveyor,bs,productSupplier,testingPredicate,stateTester);
    }

    public BuilderUtils<K,L,OUT,B> productSupplier(Function<B,OUT> ps) {
        return new BuilderUtils<>(conveyor,builderSupplier,ps,testingPredicate,stateTester);
    }

    public BuilderUtils<K,L,OUT,B> tester(Predicate<B> tester) {
        return new BuilderUtils<>(conveyor,builderSupplier,productSupplier,tester,stateTester);
    }

    public BuilderUtils<K,L,OUT,B> tester(BiPredicate<State<K,L>, B> stTester) {
        return new BuilderUtils<>(conveyor,builderSupplier,productSupplier,testingPredicate,stateTester);
    }

    public void setBuilderSupplier() {
        if(stateTester != null) {
            conveyor.setBuilderSupplier(()-> new ProductSupplierTestingState(builderSupplier.get()));
        } else if(testingPredicate != null) {
            conveyor.setBuilderSupplier(()-> new ProductSupplierTesting(builderSupplier.get()));
        } else {
            conveyor.setBuilderSupplier(() -> new ProductSupplierSimple(builderSupplier.get()));
        }
    }
}
