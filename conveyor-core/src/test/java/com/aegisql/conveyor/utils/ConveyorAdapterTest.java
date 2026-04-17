package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.AssemblingConveyorMBean;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.meta.ConveyorMetaInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.time.Duration;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ConveyorAdapterTest {

    static class TestAdapterConveyor extends ConveyorAdapter<Integer,String,String>{

        private boolean partIntercepted;

        TestAdapterConveyor(Conveyor<Integer, String, String> innerConveyor) {
            super(innerConveyor);
        }

        @Override
        public PartLoader<Integer, String> part() {
            partIntercepted = true;
            return super.part();
        }
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + Long.toUnsignedString(System.nanoTime());
    }

    private static void safeUnregister(String name) {
        try {
            Conveyor.unRegister(name);
        } catch (Exception ignored) {
            // best-effort cleanup for tests
        }
    }

    private static Conveyor<Integer, String, String> mockNamedInner(String name) {
        Conveyor<Integer, String, String> inner = mock(Conveyor.class);
        when(inner.getName()).thenReturn(name);
        when(inner.mBeanInterface()).thenReturn(null);
        return inner;
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
    void setNameIsFinalToPreserveAdapterRegistrationContract() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(ConveyorAdapter.class.getMethod("setName", String.class).getModifiers()));
    }

    @Test
    void delegatesRepresentativeOperationalMethods() {
        var publicName = uniqueName("adapter-delegate");
        Conveyor<Integer, String, String> inner = mockNamedInner(publicName);
        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);

        try {
            adapter.stop();
            adapter.setDefaultBuilderTimeout(Duration.ofMillis(1));
            adapter.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);
            adapter.getAcceptedLabels();
            adapter.unwrap();

            assertSame(inner, adapter.unwrap());
            verify(inner, atLeastOnce()).stop();
            verify(inner, atLeastOnce()).setDefaultBuilderTimeout(Duration.ofMillis(1));
            verify(inner, atLeastOnce()).setIdleHeartBeat(1, TimeUnit.MILLISECONDS);
            verify(inner, atLeastOnce()).getAcceptedLabels();
            verify(inner, atLeastOnce()).setName(publicName + "#" + Integer.toUnsignedString(System.identityHashCode(adapter)));
        } finally {
            safeUnregister(publicName);
        }
    }

    @Test
    void toStringUsesMetaInfoGenericWhenAvailable() {
        var publicName = uniqueName("adapter-to-string");
        Conveyor<Integer, String, String> inner = mockNamedInner(publicName);
        ConveyorMetaInfo<Integer, String, String> meta = mock(ConveyorMetaInfo.class);
        when(meta.generic()).thenReturn("<Integer,String,String>");
        when(inner.getMetaInfo()).thenReturn(meta);
        when(inner.toString()).thenReturn("Adapter<?,?,?>");

        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);
        try {
            assertEquals("Adapter<Integer,String,String>", adapter.toString());
            assertEquals("Adapter<Integer,String,String>", adapter.toString());
        } finally {
            safeUnregister(publicName);
        }
    }

    @Test
    void toStringFallsBackWhenMetaInfoFails() {
        var publicName = uniqueName("adapter-to-string-fallback");
        Conveyor<Integer, String, String> inner = mockNamedInner(publicName);
        when(inner.getMetaInfo()).thenThrow(new RuntimeException("boom"));
        when(inner.toString()).thenReturn("Adapter<?,?,?>");

        TestAdapterConveyor adapter = new TestAdapterConveyor(inner);
        try {
            assertEquals("Adapter<?,?,?>", adapter.toString());
            assertEquals("Adapter<?,?,?>", adapter.toString());
        } finally {
            safeUnregister(publicName);
        }
    }

    @Test
    void constructorClaimsWrappedNameForLookupAndMbeans() {
        var publicName = uniqueName("adapter-public");
        var inner = new AssemblingConveyor<Integer, String, String>();
        inner.setName(publicName);

        var adapter = new TestAdapterConveyor(inner);
        var hiddenInnerName = publicName + "#" + Integer.toUnsignedString(System.identityHashCode(adapter));

        try {
            assertEquals(publicName, adapter.getName());
            assertEquals(hiddenInnerName, adapter.unwrap().getName());
            assertSame(adapter, Conveyor.byName(publicName));
            assertSame(inner, Conveyor.byName(hiddenInnerName));

            ((TestAdapterConveyor) Conveyor.byName(publicName)).part();
            assertTrue(adapter.partIntercepted);

            var publicMBean = (AssemblingConveyorMBean) adapter.getMBeanInstance(publicName);
            assertEquals(publicName, publicMBean.getName());
            assertEquals(TestAdapterConveyor.class.getSimpleName(), publicMBean.getType());
            assertSame(adapter, publicMBean.conveyor());

            var hiddenMBean = (AssemblingConveyorMBean) inner.getMBeanInstance(hiddenInnerName);
            assertEquals(hiddenInnerName, hiddenMBean.getName());
            assertEquals("AssemblingConveyor", hiddenMBean.getType());
            assertSame(inner, hiddenMBean.conveyor());
        } finally {
            adapter.unRegister();
        }
    }

    @Test
    void setNameRebindsPublicAndHiddenNames() {
        var originalPublicName = uniqueName("adapter-public-old");
        var newPublicName = uniqueName("adapter-public-new");
        var inner = new AssemblingConveyor<Integer, String, String>();
        inner.setName(originalPublicName);

        var adapter = new TestAdapterConveyor(inner);
        var hiddenOldName = originalPublicName + "#" + Integer.toUnsignedString(System.identityHashCode(adapter));
        var hiddenNewName = newPublicName + "#" + Integer.toUnsignedString(System.identityHashCode(adapter));

        try {
            adapter.setName(newPublicName);

            assertEquals(newPublicName, adapter.getName());
            assertEquals(hiddenNewName, adapter.unwrap().getName());
            assertThrows(ConveyorRuntimeException.class, () -> Conveyor.byName(originalPublicName));
            assertThrows(ConveyorRuntimeException.class, () -> Conveyor.byName(hiddenOldName));
            assertSame(adapter, Conveyor.byName(newPublicName));
            assertSame(inner, Conveyor.byName(hiddenNewName));
        } finally {
            adapter.unRegister();
        }
    }

}
