package com.aegisql.conveyor.reflection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParametrizedLabelTest {

    String s1 = "test"; // non parametrized
    String s2 = "test{a}"; //parametrized with String 'a'
    String s3 = "test{str 'str a', int 0}"; // parametrized with String 'str a' and Integer(0)
    String s4 = "test{#, $, str a, int 0}"; // parametrized with builder, value with auto class, String 'a' and Integer(0)
    String s5 = "test{#, $java.lang.Integer, str a, int 0}"; // parametrized with builder, value with explicit class Integer, String 'a' and Integer(0)

    @Test
    public void basicGetterTest() {
        ParametrizedLabel pl1 = new ParametrizedLabel(String.class,"test");
        assertEquals(s1,pl1.getLabel());
        assertEquals(0,pl1.getClassesForGetter("builder".getClass(),"value".getClass()).length);
        assertEquals(0,pl1.getPropertiesForGetter("builder","value").length);

        ParametrizedLabel pl2 = new ParametrizedLabel(String.class,s2);
        assertEquals("test",pl1.getLabel());
        assertEquals("a",pl2.getPropertiesForGetter("builder","value")[0]);
        assertEquals(1,pl2.getClassesForGetter("builder".getClass(),"value".getClass()).length);
        assertEquals(String.class,pl2.getClassesForGetter("builder".getClass(),Integer.valueOf(1).getClass())[0]);
    }

    @Test
    public void basicSetterTest() {
        ParametrizedLabel pl1 = new ParametrizedLabel(String.class,"test");
        assertEquals(s1,pl1.getLabel());
        assertEquals(1,pl1.getClassesForSetter("builder".getClass(),"value".getClass()).length);
        assertEquals(1,pl1.getPropertiesForSetter("builder","value").length);

        ParametrizedLabel pl2 = new ParametrizedLabel(String.class,s2);
        assertEquals("test",pl1.getLabel());
        assertEquals("a",pl2.getPropertiesForSetter("builder","value")[0]);
        assertEquals("value",pl2.getPropertiesForSetter("builder","value")[1]);
        assertEquals(2,pl2.getClassesForSetter("builder".getClass(),"value".getClass()).length);
        assertEquals(String.class,pl2.getClassesForSetter("builder".getClass(),Integer.valueOf(1).getClass())[0]);
        assertEquals(Integer.class,pl2.getClassesForSetter("builder".getClass(),Integer.valueOf(1).getClass())[1]);
    }


}