package com.aegisql.conveyor.utils.reflection;

import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.java_path.StringConverter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleConveyorTest {

    static class TestStringSupplier implements Supplier<String> {
        @Override
        public String get() {
            return "value";
        }
    }

    @Test
    void shouldRejectReplacingDefaultCartConsumer() {
        SimpleConveyor<Integer, String> conveyor = new SimpleConveyor<>(TestStringSupplier::new);
        try {
            LabeledValueConsumer<String, Object, Supplier<String>> consumer = (label, value, builder) -> {};
            assertThrows(UnsupportedOperationException.class, () -> conveyor.setDefaultCartConsumer(consumer));
        } finally {
            conveyor.stop();
        }
    }

    @Test
    void shouldValidateRegistrationArgumentsAndSupportConstructors() {
        SimpleConveyor<Integer, String> defaultConveyor = new SimpleConveyor<>();
        SimpleConveyor<Integer, String> queueConveyor =
                new SimpleConveyor<>(ConcurrentLinkedQueue::new, TestStringSupplier::new);
        try {
            assertThrows(NullPointerException.class, () -> defaultConveyor.registerClass(null));
            assertThrows(NullPointerException.class, () -> defaultConveyor.registerStringConverter((Class<Integer>) null, Integer::valueOf));
            assertThrows(NullPointerException.class, () -> defaultConveyor.registerStringConverter(Integer.class, null));
            assertThrows(NullPointerException.class, () -> defaultConveyor.registerStringConverter((StringConverter<Integer>) null, "IntegerAlias"));

            defaultConveyor.registerClass(String.class, "String");
            defaultConveyor.registerStringConverter(Integer.class, Integer::valueOf);
            defaultConveyor.registerStringConverter(Integer::valueOf, "IntegerAlias");
        } finally {
            defaultConveyor.stop();
            queueConveyor.stop();
        }
    }
}
