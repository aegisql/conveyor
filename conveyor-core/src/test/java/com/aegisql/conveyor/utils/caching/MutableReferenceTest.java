package com.aegisql.conveyor.utils.caching;

import com.aegisql.conveyor.BuilderSupplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutableReferenceTest {

    @Test
    void shouldTrackValueVersionAndSupplier() {
        MutableReference<String> ref = new MutableReference<>("v1");

        assertEquals("v1", ref.get());
        assertEquals(1L, ref.getVersion());
        assertSame(ref, ref.getSupplier());

        ref.accept("v2");
        assertEquals("v2", ref.get());
        assertEquals(2L, ref.getVersion());

        MutableReference.update(ref, "v3");
        assertEquals("v3", ref.get());
        assertEquals(3L, ref.getVersion());

        assertTrue(ref.toString().contains("Ref ver(3): v3"));
    }

    @Test
    void newInstanceFactoryShouldCreateMutableReference() {
        BuilderSupplier<String> factory = MutableReference.newInstance("seed");
        MutableReference<String> created = (MutableReference<String>) factory.get();

        assertEquals("seed", created.get());
        assertEquals(1L, created.getVersion());
        assertSame(created, created.getSupplier());
    }
}
