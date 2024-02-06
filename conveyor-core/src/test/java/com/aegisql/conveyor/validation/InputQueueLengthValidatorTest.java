package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.consumers.result.ResultCounter;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputQueueLengthValidatorTest {

    static class A{
        String val;
    }
    class ABuilder implements Supplier<A>, Testing {
        A a = new A();
        @Override
        public boolean test() {
            return true;
        }

        @Override
        public A get() {
            return a;
        }

        public void setVal(String val) {
            a.val = val;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    SmartLabel<ABuilder> VAL = SmartLabel.of("VAL",ABuilder::setVal);

    @Test
    public void backPressureTest() throws InterruptedException {
        AssemblingConveyor<Integer,SmartLabel<ABuilder>,A> ac = new AssemblingConveyor<>();
        ac.setName("BackPressureTester");
        ac.setBuilderSupplier(ABuilder::new);
        ResultCounter<Integer, A> rc1 = ResultCounter.of(ac);
        ResultQueue<Integer, A> rc2 = ResultQueue.of(ac);
        ac.resultConsumer(rc1.andThen(rc2)).set();
        InputQueueLengthValidator<Integer, SmartLabel<ABuilder>> cartBeforePlacementValidator = new InputQueueLengthValidator<>(100, ac::getInputQueueSize);
        ac.addCartBeforePlacementValidator(cartBeforePlacementValidator);

        CompletableFuture<Boolean> f;
        for(int i = 1; i <= 1000; i++) {
            f = ac.part().id(i).label(VAL).value("VAL "+i).place();
            if(f.isCompletedExceptionally()) {
                throw new RuntimeException("Cart was rejected with key="+i);
            }
        }
        int size = ac.getInputQueueSize();
        assertTrue(size <=100);
        while(size > 0) {
            System.out.println("Queue size = " + size);
            size = ac.getInputQueueSize();
            Thread.sleep(500);
        }
        cartBeforePlacementValidator.reset();
        assertEquals(1000,rc1.get());
    }

    @Test
    public void backPressureWithMaxTimeTest() throws InterruptedException {
        AssemblingConveyor<Integer,SmartLabel<ABuilder>,A> ac = new AssemblingConveyor<>();
        ac.setName("BackPressureTester2");
        ac.setBuilderSupplier(ABuilder::new);
        ResultCounter<Integer, A> rc1 = ResultCounter.of(ac);
        ResultQueue<Integer, A> rc2 = ResultQueue.of(ac);
        ac.resultConsumer(rc1.andThen(rc2)).set();
        InputQueueLengthValidator<Integer, SmartLabel<ABuilder>> cartBeforePlacementValidator = new InputQueueLengthValidator<>(100, ac::getInputQueueSize,10000, TimeUnit.MILLISECONDS);
        ac.addCartBeforePlacementValidator(cartBeforePlacementValidator);

        CompletableFuture<Boolean> f;
        for(int i = 1; i <= 500; i++) {
            f = ac.part().id(i).label(VAL).value("VAL "+i).place();
            if(f.isCompletedExceptionally()) {
                throw new RuntimeException("Cart was rejected with key="+i);
            }
        }
        int size = ac.getInputQueueSize();
        assertTrue(size <=100);
        while(size > 0) {
            System.out.println("Queue size = " + size);
            size = ac.getInputQueueSize();
            Thread.sleep(500);
        }
        cartBeforePlacementValidator.reset();
        assertEquals(500,rc1.get());
    }

    @Test
    public void backPressureWithShortMaxTimeTest() throws InterruptedException {
        AssemblingConveyor<Integer,SmartLabel<ABuilder>,A> ac = new AssemblingConveyor<>();
        ac.setName("BackPressureTester2");
        ac.setBuilderSupplier(ABuilder::new);
        ResultCounter<Integer, A> rc1 = ResultCounter.of(ac);
        ResultQueue<Integer, A> rc2 = ResultQueue.of(ac);
        ac.resultConsumer(rc1.andThen(rc2)).set();
        InputQueueLengthValidator<Integer, SmartLabel<ABuilder>> cartBeforePlacementValidator = new InputQueueLengthValidator<>(100, ac::getInputQueueSize,100, TimeUnit.MILLISECONDS);
        ac.addCartBeforePlacementValidator(cartBeforePlacementValidator);

        CompletableFuture<Boolean> f;
        boolean completedExceptionally = false;
        for(int i = 1; i <= 1000; i++) {
            f = ac.part().id(i).label(VAL).value("VAL "+i).place();
            if(f.isCompletedExceptionally()) {
                completedExceptionally = true;
            }
        }
        assertTrue(completedExceptionally);
    }

}