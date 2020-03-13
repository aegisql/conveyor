package com.aegisql.conveyor.reflection;

import org.junit.Test;

import static org.junit.Assert.*;

public class LabelPropertyTest {

    @Test
    public void basicTest() {
        LabelProperty lp1 = new LabelProperty("a b");
        assertEquals("a b",lp1.getPropertyStr());
        assertEquals("a b",lp1.getProperty());
        assertEquals(String.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void basicTestWithQuote() {
        LabelProperty lp1 = new LabelProperty("'a b'");
        assertEquals("a b",lp1.getPropertyStr());
        assertEquals("a b",lp1.getProperty());
        assertEquals(String.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void basicTestWithStrType() {
        LabelProperty lp1 = new LabelProperty("str a b");
        assertEquals("a b",lp1.getPropertyStr());
        assertEquals("a b",lp1.getProperty());
        assertEquals(String.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void basicTestWithStrTypeAndQuotes() {
        LabelProperty lp1 = new LabelProperty("str 'a b'");
        assertEquals("a b",lp1.getPropertyStr());
        assertEquals("a b",lp1.getProperty());
        assertEquals(String.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void basicTestWithIntType() {
        LabelProperty lp1 = new LabelProperty("i 0");
        assertEquals("0",lp1.getPropertyStr());
        assertEquals(0,lp1.getProperty());
        assertEquals(int.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void quotesWorkTest() {
        LabelProperty lp1 = new LabelProperty("'int 0'");
        assertEquals("int 0",lp1.getPropertyStr());
        assertEquals("int 0",lp1.getProperty());
        assertEquals(String.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    enum X {A,B}

    @Test
    public void basicTestWithEnumType() {
        LabelProperty lp1 = new LabelProperty("com.aegisql.conveyor.reflection.LabelPropertyTest$X A");
        assertEquals("A",lp1.getPropertyStr());
        assertEquals(X.A,lp1.getProperty());
        assertEquals(X.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void builderTest() {
        LabelProperty lp1 = new LabelProperty("#");
        assertEquals("#",lp1.getPropertyStr());
        assertEquals(null,lp1.getProperty());
        assertEquals(null,lp1.getPropertyType());
        assertTrue(lp1.isBuilder());
        assertFalse(lp1.isValue());
    }

    @Test
    public void valueTest() {
        LabelProperty lp1 = new LabelProperty("$");
        assertEquals("$",lp1.getPropertyStr());
        assertEquals(null,lp1.getProperty());
        assertEquals(null,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertTrue(lp1.isValue());
    }

    @Test
    public void valueWithTypeTest() {
        LabelProperty lp1 = new LabelProperty("$int");
        assertEquals("$",lp1.getPropertyStr());
        assertEquals(null,lp1.getProperty());
        assertEquals(int.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertTrue(lp1.isValue());
    }

    @Test
    public void valueWithFullTypeTest() {
        LabelProperty lp1 = new LabelProperty("$java.lang.Integer");
        assertEquals("$",lp1.getPropertyStr());
        assertEquals(null,lp1.getProperty());
        assertEquals(Integer.class,lp1.getPropertyType());
        assertFalse(lp1.isBuilder());
        assertTrue(lp1.isValue());
    }



}