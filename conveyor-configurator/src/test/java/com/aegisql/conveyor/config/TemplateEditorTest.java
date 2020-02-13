package com.aegisql.conveyor.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TemplateEditorTest {

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty("keyP", "value");

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();


    @Test
    public void setVariableShouldWorkForKnownKey() {
        TemplateEditor te = new TemplateEditor();
        te.setVariables("key1","value");
        String kv3 = te.setVariables("key2","${key1}");
        assertNotNull(kv3);
        assertEquals("value",kv3);
    }

    @Test
    public void setVariableShouldWorkForKnownKeys() {
        TemplateEditor te = new TemplateEditor();
        te.setVariables("key1","val");
        te.setVariables("key2","ue");
        String kv4 = te.setVariables("key3","${key1}${key2}");
        assertNotNull(kv4);
        assertEquals("value",kv4);
    }

    @Test
    public void setVariableShouldWorkForKnownKeysAndMultipleOccurrences() {
        TemplateEditor te = new TemplateEditor();
        te.setVariables("key1","val");
        te.setVariables("key2","ue");
        String kv4 = te.setVariables("key3","${key1}${key2}${key1}");
        assertNotNull(kv4);
        assertEquals("valueval",kv4);
    }

    @Test
    public void setVariableShouldWorkForKnownEnvVariable() {
        environmentVariables.set("key1","value");
        TemplateEditor te = new TemplateEditor();
        te.setVariables("key1","blah"); //env has higher priority
        String kv3 = te.setVariables("key2","${key1}");
        assertNotNull(kv3);
        assertEquals("value",kv3);
    }

    @Test
    public void setVariableShouldWorkForKnownSysPropertyVariable() {
        TemplateEditor te = new TemplateEditor();
        String kv3 = te.setVariables("key2","${keyP}");
        assertNotNull(kv3);
        assertEquals("value",kv3);
    }

    @Test(expected = ConveyorConfigurationException.class)
    public void missingPropertyShouldCauseAnException() {
        TemplateEditor te = new TemplateEditor();
        String kv3 = te.setVariables("key1","${keyNone}");
    }

    @Test(expected = NullPointerException.class)
    public void missingValueShouldCauseAnException() {
        TemplateEditor te = new TemplateEditor();
        te.setVariables("keyNone",null);
    }

    @Test
    public void defaultPropertyShouldHaveAValue() {
        TemplateEditor te = new TemplateEditor();
        String kv3 = te.setVariables("key1","${keyNone:default}");
        assertNotNull(kv3);
        assertEquals("default",kv3);
    }

    @Test
    public void defaultPropertyShouldHaveAValueSetByExplicitProperty() {
        TemplateEditor te = new TemplateEditor();
        te.setVariables("keyNoneDef","value");
        String kv3 = te.setVariables("key1","${keyNoneDef:default}");
        assertNotNull(kv3);
        assertEquals("value",kv3);
    }

    @Test
    public void defaultPropertyWithDoubleColonShouldHaveAValue() {
        TemplateEditor te = new TemplateEditor();
        String kv3 = te.setVariables("key1","${keyNone:default::value}");
        assertNotNull(kv3);
        assertEquals("default::value",kv3);
    }


}