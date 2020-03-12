package com.aegisql.conveyor.reflection;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParametrizedLabelTest {

    String s1 = "test"; // non parametrized
    String s2 = "test{a}"; //parametrized with String 'a'
    String s3 = "test{str 'str a', int 0}"; // parametrized with String 'str a' and Integer(0)
    String s4 = "test{#, $, str a, int 0}"; // parametrized with builder, value with auto class, String 'a' and Integer(0)
    String s5 = "test{#, $java.lang.Integer, str a, int 0}"; // parametrized with builder, value with explicit class Integer, String 'a' and Integer(0)

    @Test
    public void basicTest() {
        ParametrizedLabel pl1 = new ParametrizedLabel(String.class,"test");
        assertFalse(pl1.isParametrized());
        assertEquals(s1,pl1.getLabel());

        ParametrizedLabel pl2 = new ParametrizedLabel(String.class,s2);
        assertTrue(pl2.isParametrized());
        assertEquals("test",pl1.getLabel());
        assertEquals("a",pl2.getProperties(String.class,String.class)[0]);
    }


}