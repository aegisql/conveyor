package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import com.aegisql.conveyor.reflection.SimpleConveyor;
import org.junit.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DuplicateValidatorTest {

    @Test
    public void duplicateValidatorBasicTest() {
        DuplicateValidator<String,String,Integer> dv = new DuplicateValidator<>();
        Cart<String,String,String> cart = new ShoppingCart<>("key","value","label");
        dv.accept(cart);
        dv.accept(cart);
        cart.addProperty("VALUE_ID",1);
        dv.accept(cart);
    }

    @Test(expected = DuplicateValueException.class)
    public void duplicateValidatorFailureTest() {
        DuplicateValidator<String,String,Integer> dv = new DuplicateValidator<>();
        Cart<String,String,String> cart = new ShoppingCart<>("key","value","label");
        cart.addProperty("VALUE_ID",1);
        dv.accept(cart);
        dv.accept(cart);
    }

    @Test
    public void duplicateValidatorAcknowledgeTest() {
        DuplicateValidator<String,String,Integer> dv = new DuplicateValidator<>();
        Consumer<AcknowledgeStatus<String>> acknowledge = dv.acknowledge();
        Cart<String,String,String> cart = new ShoppingCart<>("key","value","label");
        cart.addProperty("VALUE_ID",1);
        dv.accept(cart);
        acknowledge.accept(new AcknowledgeStatus<>("key", Status.READY,cart.getAllProperties()));
        dv.accept(cart);
    }

    @Test
    public void duplicateValidatorMultiLabelTest() {
        DuplicateValidator<String,String,Integer> dv = new DuplicateValidator<>();
        Cart<String,String,String> cart1 = new ShoppingCart<>("key","value","label1");
        Cart<String,String,String> cart2 = new ShoppingCart<>("key","value","label2");
        cart1.addProperty("VALUE_ID",1);
        cart2.addProperty("VALUE_ID",1);
        dv.accept(cart1);
        dv.accept(cart2);
        dv.acknowledge().accept(new AcknowledgeStatus<>("key", Status.READY,cart1.getAllProperties()));
    }

    public static class SummaBuilder implements Supplier<Integer> {
        public int summa = 0;

        @Override
        public Integer get() {
            return summa;
        }

        public void add(Integer x) {
            summa+=x;
        }

        public void done() {}
    }

    @Test
    public void duplicateValueWithConveyor() {
        SimpleConveyor<Integer,Integer> sc = new SimpleConveyor<>();
        sc.setBuilderSupplier(SummaBuilder::new);
        LastResultReference<Integer,Integer> res = LastResultReference.of(sc);
        sc.resultConsumer(res).set();
        LastScrapReference<Integer> scrap = LastScrapReference.of(sc);
        sc.scrapConsumer(scrap).set();
        sc.setReadinessEvaluator(Conveyor.getTesterFor(sc).accepted("done"));
        DuplicateValidator.wrap(sc);
        var loader = sc.part().id(1).label("add");

        loader.value(10).addProperty("VALUE_ID",100).place();
        loader.value(10).addProperty("VALUE_ID",101).place();
        loader.value(10).addProperty("VALUE_ID",102).place();
        loader.value(10).addProperty("VALUE_ID",100).place();
        loader.label("done").place().join();

        assertEquals(Integer.valueOf(30),res.getCurrent());
        assertNotNull(scrap.getCurrent());
        assertEquals(DuplicateValueException.class,scrap.getCurrent().error.getClass());

        loader.value(100).addProperty("VALUE_ID",100).place();
        loader.value(100).addProperty("VALUE_ID",101).place();
        loader.value(100).addProperty("VALUE_ID",102).place().join();
        loader.label("done").place().join();
        assertEquals(Integer.valueOf(300),res.getCurrent());

        sc.completeAndStop().join();
    }

}