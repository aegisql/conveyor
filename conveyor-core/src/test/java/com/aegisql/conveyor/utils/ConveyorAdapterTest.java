package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ConveyorAdapterTest {

    static class TestAdapterConveyor extends ConveyorAdapter<Integer,String,String>{

        TestAdapterConveyor(Conveyor<Integer, String, String> innerConveyor) {
            super(innerConveyor);
        }
    }

    private static Object defaultArgument(Class<?> type) {
        if (type == boolean.class) return true;
        if (type == int.class) return 1;
        if (type == long.class) return 1L;
        if (type == double.class) return 1d;
        if (type == float.class) return 1f;
        if (type == short.class) return (short) 1;
        if (type == byte.class) return (byte) 1;
        if (type == char.class) return 'a';

        if (type == String.class) return "name";
        if (type == Duration.class) return Duration.ofMillis(1);
        if (type == TimeUnit.class) return TimeUnit.MILLISECONDS;
        if (type == Status.class) return Status.READY;
        if (type == BuilderSupplier.class) return (BuilderSupplier<String>) () -> () -> "value";
        if (type == LabeledValueConsumer.class) return (LabeledValueConsumer<String, Object, Supplier<? extends String>>) (l, v, b) -> {};
        if (type == Consumer.class) return (Consumer<Object>) o -> {};
        if (type == BiConsumer.class) return (BiConsumer<Object, Object>) (a, b) -> {};
        if (type == Predicate.class) return (Predicate<Object>) o -> true;
        if (type == BiPredicate.class) return (BiPredicate<Object, Object>) (a, b) -> true;
        if (type == Function.class) return (Function<Object, Object>) o -> o;
        if (type == Cart.class) return mock(Cart.class);
        if (type == GeneralCommand.class) return mock(GeneralCommand.class);

        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        if (type.isInterface()) {
            return mock((Class) type);
        }
        return null;
    }

    @Test
    void constructorRejectsNullInnerConveyor() {
        assertThrows(NullPointerException.class, () -> new TestAdapterConveyor(null));
    }

    @Test
    void delegatesAllPublicMethods() throws Exception {
        Conveyor<Integer, String, String> inner = mock(Conveyor.class);
        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);

        for (Method method : ConveyorAdapter.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            if ("toString".equals(method.getName())) {
                continue;
            }

            Object[] args = Arrays.stream(method.getParameterTypes())
                    .map(ConveyorAdapterTest::defaultArgument)
                    .toArray();

            try {
                method.invoke(adapter, args);
            } catch (InvocationTargetException e) {
                fail(method.getName() + " should delegate without throwing, but got: " + e.getTargetException());
            }
        }

        assertSame(inner, adapter.unwrap());
        verify(inner, atLeastOnce()).stop();
        verify(inner, atLeastOnce()).unRegister();
        verify(inner, atLeastOnce()).setName("name");
    }

    @Test
    void toStringUsesMetaInfoGenericWhenAvailable() {
        Conveyor<Integer, String, String> inner = mock(Conveyor.class);
        ConveyorMetaInfo<Integer, String, String> meta = mock(ConveyorMetaInfo.class);
        when(meta.generic()).thenReturn("<Integer,String,String>");
        when(inner.getMetaInfo()).thenReturn(meta);
        when(inner.toString()).thenReturn("Adapter<?,?,?>");

        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);
        assertEquals("Adapter<Integer,String,String>", adapter.toString());
        assertEquals("Adapter<Integer,String,String>", adapter.toString());
    }

    @Test
    void toStringFallsBackWhenMetaInfoFails() {
        Conveyor<Integer, String, String> inner = mock(Conveyor.class);
        when(inner.getMetaInfo()).thenThrow(new RuntimeException("boom"));
        when(inner.toString()).thenReturn("Adapter<?,?,?>");

        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);
        assertEquals("Adapter<?,?,?>", adapter.toString());
        assertEquals("Adapter<?,?,?>", adapter.toString());
    }

}
