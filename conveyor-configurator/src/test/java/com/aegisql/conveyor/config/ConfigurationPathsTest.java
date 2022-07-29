package com.aegisql.conveyor.config;

import com.aegisql.conveyor.config.harness.TestBean;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationPathsTest {

    String test = "TEST";

    @Test
    public void basicTestWithoutInitializationOnly() {
        ConfigurationPaths cp = new ConfigurationPaths();

        cp.register("com.aegisql.conveyor.config.harness.TestBean");

        assertTrue(cp.containsPath("com.aegisql.conveyor.config.harness.TestBean"));
        assertTrue(cp.containsPath("TestBean"));
        assertFalse(cp.containsObject("testBean"));
    }

    @Test(expected = ConveyorConfigurationException.class)
    public void testFailWithWrongClassName() {
        ConfigurationPaths cp = new ConfigurationPaths();
        cp.register("com.aegisql.conveyor.config.harness.TestBeam");
    }

    @Test
    public void basicTestWithInitialization() {
        ConfigurationPaths cp = new ConfigurationPaths();

        cp.register("com.aegisql.conveyor.config.harness.TestBean testBean");

        assertTrue(cp.containsPath("com.aegisql.conveyor.config.harness.TestBean"));
        assertTrue(cp.containsPath("TestBean"));
        assertTrue(cp.containsObject("testBean"));
        Object testBean = cp.get("testBean");
        assertTrue(testBean instanceof TestBean);

        Object timeout = cp.evalPath("testBean.timeout");
        assertNotNull(timeout);
        assertEquals(1000,timeout);

        Object type = cp.evalPath("testBean.type");
        assertNotNull(type);
        assertEquals("value",type);
    }

    @Test
    public void testBeanRegister() {
        ConfigurationPaths cp = new ConfigurationPaths();
        cp.registerBean(this,"theTester");
        assertNotNull(cp.get("theTester"));
        assertNotNull(cp.get("configurationPathsTest"));
        assertEquals(this,cp.get("theTester"));
        assertTrue(cp.containsPath(this.getClass().getName()));
        assertTrue(cp.containsPath(this.getClass().getSimpleName()));
        Object o = cp.evalPath("theTester.test");
        assertEquals("TEST",o);
    }

    @Test
    public void basicTestWithtoutInitialization() {
        ConfigurationPaths cp = new ConfigurationPaths();

        assertFalse(cp.containsPath("com.aegisql.conveyor.config.harness.TestBean"));
        assertFalse(cp.containsPath("TestBean"));
        assertFalse(cp.containsObject("testBean"));
        Object timeout = cp.evalPath("(com.aegisql.conveyor.config.harness.TestBean testBean).timeout");

        assertTrue(cp.containsPath("com.aegisql.conveyor.config.harness.TestBean"));
        assertTrue(cp.containsPath("TestBean"));

        Object testBean = cp.get("testBean");
        assertTrue(testBean instanceof TestBean);



        assertNotNull(timeout);
        assertEquals(1000,timeout);

        Object type = cp.evalPath("testBean.type");
        assertNotNull(type);
        assertEquals("value",type);
    }

    @Test
    public void basicTestWithtInitializationShortName() {
        ConfigurationPaths cp = new ConfigurationPaths();
        cp.register("com.aegisql.conveyor.config.harness.TestBean");

        assertTrue(cp.containsPath("com.aegisql.conveyor.config.harness.TestBean"));
        assertTrue(cp.containsPath("TestBean"));
        assertFalse(cp.containsObject("testBean"));
        Object timeout = cp.evalPath("(TestBean testBean).timeout");

        assertTrue(cp.containsObject("testBean"));

        Object testBean = cp.get("testBean");
        assertTrue(testBean instanceof TestBean);



        assertNotNull(timeout);
        assertEquals(1000,timeout);

        Object type = cp.evalPath("testBean.type");
        assertNotNull(type);
        assertEquals("value",type);
    }



}