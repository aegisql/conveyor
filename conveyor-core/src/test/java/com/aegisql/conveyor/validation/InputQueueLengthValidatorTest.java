package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.consumers.result.IgnoreResult;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

public class InputQueueLengthValidatorTest {

    class A{
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
        ac.resultConsumer(IgnoreResult.of(ac)).set();
        ac.addCartBeforePlacementValidator(new InputQueueLengthValidator<>(100,ac::getInputQueueSize));

        CompletableFuture<Boolean> f;
        for(int i = 1; i <= 1000; i++) {
            f = ac.part().id(i).label(VAL).value("VAL "+i).place();
        }
        int size = ac.getInputQueueSize();
        assertTrue(size <=100);
        while(size > 0) {
            System.out.println("Queue size = " + size);
            size = ac.getInputQueueSize();
            Thread.sleep(1000);
        }
    }

}