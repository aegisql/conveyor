package com.aegisql.conveyor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReadinessPredicateTest {

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

}