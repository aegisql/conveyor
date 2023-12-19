package com.aegisql.conveyor.meta;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ConveyorMetaInfoBuilderTest {

    @Test
    public void basicMetaInfoTest() {
        AssemblingConveyor<Integer,String, User> ac = new AssemblingConveyor<>(){
            @Override
            public ConveyorMetaInfo<Integer, String, User> getMetaInfo() {
                return ConveyorMetaInfoBuilder.of(this)
                        .keyType(Integer.class)
                        .labelType(String.class)
                        .productType(User.class)
                        .labels("A","B","C")
                        .builderSupplier(UserBuilder::new)
                        .supportedTypes("A", String.class)
                        .supportedTypes("B", String.class)
                        .supportedTypes("C", Long.class,Integer.class)
                        .get();
            }
        };
        ConveyorMetaInfo<Integer, String, User> metaInfo = ac.getMetaInfo();
        System.out.println(metaInfo);
        assertEquals(Integer.class,metaInfo.getKeyType());
        assertEquals(String.class,metaInfo.getLabelType());
        assertEquals(User.class,metaInfo.getProductType());
        Set<String> labels = metaInfo.getLabels();
        assertNotNull(labels);
        assertEquals(3,labels.size());
        assertTrue(labels.contains("A"));
        assertTrue(labels.contains("B"));
        assertTrue(labels.contains("C"));

        Set<Class<?>> a = metaInfo.getSupportedValueTypes("A");
        assertNotNull(a);
        assertEquals(1,a.size());
        assertTrue(a.contains(String.class));

        Set<Class<?>> b = metaInfo.getSupportedValueTypes("B");
        assertNotNull(b);
        assertEquals(1,b.size());
        assertTrue(b.contains(String.class));

        Set<Class<?>> c = metaInfo.getSupportedValueTypes("C");
        assertNotNull(c);
        assertEquals(2,c.size());
        assertTrue(c.contains(Integer.class));
        assertTrue(c.contains(Long.class));
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void exceptionTest() {
        AssemblingConveyor ac = new AssemblingConveyor();
        ac.getMetaInfo();
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void labelsMismatchExceptionTest() {
        AssemblingConveyor<Integer,String, User> ac = new AssemblingConveyor<>(){
            @Override
            public ConveyorMetaInfo<Integer, String, User> getMetaInfo() {
                return ConveyorMetaInfoBuilder.of(this)
                        .keyType(Integer.class)
                        .labelType(String.class)
                        .productType(User.class)
                        .labels("A","B","C")
                        .builderSupplier(UserBuilder::new)
                        .supportedTypes("A", String.class)
                        .supportedTypes("B", String.class)
                        .supportedTypes("C", Long.class,Integer.class)
                        .supportedTypes("D", Double.class)
                        .get();
            }
        };
        ConveyorMetaInfo<Integer, String, User> metaInfo = ac.getMetaInfo();
    }

}