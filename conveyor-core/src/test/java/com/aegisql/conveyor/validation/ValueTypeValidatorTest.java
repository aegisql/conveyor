package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ValueTypeValidatorTest {

    static class TestBuilder implements Supplier<String> {

        public void setString(String s) {
            System.out.println("STRING "+s);
        }

        public void setInteger(Integer i) {
            System.out.println("INTEGER "+i);
        }

        public void setCharSeq(CharSequence s) {
            System.out.println("CharSeq "+s);
        }

        @Override
        public String get() {
            return "TEST";
        }
    }

    @Test
    public void testValueTypeValidator() {
        ValueTypeValidator<Integer,SmartLabel<TestBuilder>> validator = new ValueTypeValidator<>();

        var SET_STRING = SmartLabel.of("SET_STRING",TestBuilder::setString).acceptType(String.class);
        var SET_CHAR_SEQ = SmartLabel.of("SET_CHAR_SEQ",TestBuilder::setCharSeq).acceptType(CharSequence.class);
        var SET_INTEGER = SmartLabel.of("SET_INTEGER",TestBuilder::setInteger).acceptType(Integer.class);

        TestBuilder b = new TestBuilder();

        SET_STRING.get().accept(b,"TEST");
        SET_CHAR_SEQ.get().accept(b,"TEST");
        SET_INTEGER.get().accept(b,1);

        Cart<Integer,?,SmartLabel<TestBuilder>> cart1 = new ShoppingCart<>(1,"TEST",SET_STRING);
        Cart<Integer,?,SmartLabel<TestBuilder>> cart2 = new ShoppingCart<>(1,1,SET_INTEGER);
        Cart<Integer,?,SmartLabel<TestBuilder>> cart3 = new ShoppingCart<>(1,"TEST",SET_CHAR_SEQ);

        validator.accept(cart1);
        validator.accept(cart2);
        validator.accept(cart3);

        Cart<Integer,?,SmartLabel<TestBuilder>> cart4 = new ShoppingCart<>(1,"1",SET_INTEGER);

        try {
            validator.accept(cart4);
            fail("Unsupported type passed");
        } catch (ConveyorRuntimeException e) {
            assertTrue(e.getMessage().startsWith("Value type is unsupported"));
        }

    }

}