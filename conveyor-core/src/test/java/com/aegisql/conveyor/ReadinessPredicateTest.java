package com.aegisql.conveyor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadinessPredicateTest {

    private static State<Integer, String> state(int key, int previouslyAccepted, Map<String, Integer> eventHistory) {
        return new State<>(
                key, 0, 0, 0, 0, previouslyAccepted, eventHistory, new ArrayList<>()
        );
    }

    @Test
    public void readinessPredicateTest() {

        ReadinessPredicate<Integer, String, String> rp = (st, sup) -> st.key.equals(1) && sup.get().equals("test");
        State<Integer,String> st1 = new State<>(
                1,0,0,0,0,0,new HashMap<>(),new ArrayList<>()
        );
        rp=rp.or((st, sup) -> st.key.equals(2) && sup.get().equals("TEST"));
        assertTrue(rp.test(st1, ()->"test"));
        State<Integer,String> st2 = new State<>(
                2,0,0,0,0,0,new HashMap<>(),new ArrayList<>()
        );
        assertTrue(rp.test(st2, ()->"TEST"));
        assertFalse(rp.test(st2, ()->"OTHER"));
    }

    @Test
    public void readinessPredicateDefaultMethodsCoverage() {
        Map<String, Integer> history = new HashMap<>();
        history.put("A", 2);
        history.put("B", 1);
        State<Integer, String> st = state(1, 3, history);

        ReadinessPredicate<Integer, String, String> base = (s, sup) -> s.key == 1 && "ok".equals(sup.get());
        ReadinessPredicate<Integer, String, String> alwaysFalse = (s, sup) -> false;
        Predicate<Supplier<? extends String>> supplierIsOk = sup -> "ok".equals(sup.get());

        assertTrue(base.and(supplierIsOk).test(st, () -> "ok"));
        assertFalse(base.and(alwaysFalse).test(st, () -> "ok"));
        assertTrue(alwaysFalse.or(base).test(st, () -> "ok"));
        assertTrue(alwaysFalse.or(supplierIsOk).test(st, () -> "ok"));
        assertFalse(base.negate().test(st, () -> "ok"));

        assertTrue(base.accepted(3).test(st, () -> "ignored"));
        assertFalse(base.accepted(2).test(st, () -> "ignored"));

        assertTrue(base.accepted("A", 2).test(st, () -> "ignored"));
        assertTrue(base.accepted("missing", 0).test(st, () -> "ignored"));
        assertFalse(base.accepted("B", 2).test(st, () -> "ignored"));

        assertTrue(base.accepted("B", "A").test(st, () -> "ignored"));
        assertFalse(base.accepted("C", "A").test(st, () -> "ignored"));
    }

    @Test
    public void readinessPredicateStaticFactoriesCoverage() {
        State<Integer, String> st = state(7, 0, new HashMap<>());
        Supplier<String> supplier = () -> "value";

        assertTrue(ReadinessPredicate.<Integer, String, String>of(null).test(st, supplier));

        ReadinessPredicate<Integer, String, String> fromPredicate =
                ReadinessPredicate.of(null, (Predicate<Supplier<? extends String>>) sup -> "value".equals(sup.get()));
        assertTrue(fromPredicate.test(st, supplier));
        assertFalse(fromPredicate.test(st, () -> "other"));

        ReadinessPredicate<Integer, String, String> custom = (s, sup) -> s.key == 7;
        assertSame(custom, ReadinessPredicate.<Integer, String, String>of(null, custom));
    }

}
