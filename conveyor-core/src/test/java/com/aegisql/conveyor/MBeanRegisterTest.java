package com.aegisql.conveyor;

import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import com.aegisql.conveyor.utils.ConveyorAdapter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.aegisql.conveyor.MBeanRegister.MBEAN;
import static org.junit.jupiter.api.Assertions.*;

public class MBeanRegisterTest {

    @Test
    public void mbeanStandardRegistryTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>();
        ac.setName("MBeanRegisterTest");
        MBEAN.resetConveyor("MBeanRegisterTest");
        Conveyor ac2 = MBEAN.byName("MBeanRegisterTest");
        assertNotNull(ac2);
        assertEquals(ac,ac2);

        AssemblingConveyorMBean mBean = (AssemblingConveyorMBean) ac2.getMBeanInstance("MBeanRegisterTest");
        assertNotNull(mBean);
        assertEquals("MBeanRegisterTest",mBean.getName());
        assertEquals("AssemblingConveyor",mBean.getType());
        assertEquals(ac,mBean.conveyor());
        mBean.stop();

        MBEAN.unRegister("MBeanRegisterTest");
        try {
            MBEAN.byName("MBeanRegisterTest");
            fail("UnRegistered conveyor must not be visible");
        } catch (Exception e) {

        }


    }

    @Test
    public void mbeanNoInterfaceRegistryTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>(){
            public Class<?> mBeanInterface() {
                return null;
            }
        };
        ac.setName("MBeanRegisterTest2");
        Conveyor ac2 = MBEAN.byName(ac.getName());
        assertNotNull(ac2);
        assertEquals(ac,ac2);
        MBEAN.unRegister("MBeanRegisterTest2");
        try {
            MBEAN.byName("MBeanRegisterTest2");
            fail("UnRegistered conveyor must not be visible");
        } catch (Exception e) {
        }
    }

    @Test
    public void mbeanRegistryConvExceptionTest() {
        assertThrows(ConveyorRuntimeException.class,()->new AssemblingConveyor<>(){
            public Class<?> mBeanInterface() {
                throw new ConveyorRuntimeException("test me");
            }
        });
    }

    @Test
    public void mbeanRegistryNPExceptionTest() {
        assertThrows(ConveyorRuntimeException.class,()->new AssemblingConveyor<>(){
            public Class<?> mBeanInterface() {
                throw new NullPointerException("test me with null");
            }
        });
    }

    @Test
    public void registeredConveyorNamesTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>();
        ac.setName("MBeanRegisterNamesTest");
        assertTrue(MBEAN.getRegisteredConveyorNames().contains("MBeanRegisterNamesTest"));
        MBEAN.unRegister("MBeanRegisterNamesTest");
        assertFalse(MBEAN.getRegisteredConveyorNames().contains("MBeanRegisterNamesTest"));
    }

    @Test
    public void knownConveyorNamesAndMalformedMBeanNameTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>();
        ac.setName("MBeanKnownNamesTest");
        try {
            assertNotNull(MBEAN.byName("MBeanKnownNamesTest"));
            assertTrue(MBEAN.getKnownConveyorNames().contains("MBeanKnownNamesTest"));

            MBEAN.resetConveyor("MBeanKnownNamesTest");
            assertFalse(MBEAN.getKnownConveyorNames().contains("MBeanKnownNamesTest"));

            assertThrows(
                    ConveyorRuntimeException.class,
                    () -> MBEAN.getMBeanInstance("bad,name", AssemblingConveyorMBean.class)
            );
        } finally {
            MBEAN.unRegister("MBeanKnownNamesTest");
        }
    }

    @Test
    public void adapterAndHiddenInnerNamesAreRegisteredAndNestedInTree() {
        var publicName = "MBeanAdapterTreeTest-" + Long.toUnsignedString(System.nanoTime());
        var inner = new AssemblingConveyor<Integer, String, String>();
        inner.setName(publicName);

        var adapter = new ConveyorAdapter<Integer, String, String>(inner) { };
        var hiddenInnerName = publicName + "#" + Integer.toUnsignedString(System.identityHashCode(adapter));

        try {
            assertTrue(MBEAN.getRegisteredConveyorNames().contains(publicName));
            assertTrue(MBEAN.getRegisteredConveyorNames().contains(hiddenInnerName));

            Map<String, Map<String, ?>> tree = Conveyor.getKnownConveyorNameTree();
            assertTrue(tree.containsKey(publicName));
            assertFalse(tree.containsKey(hiddenInnerName));

            Map<String, ?> adapterSubTree = tree.get(publicName);
            assertNotNull(adapterSubTree);
            assertTrue(adapterSubTree.containsKey(hiddenInnerName));
            assertTrue(((Map<String, ?>) adapterSubTree.get(hiddenInnerName)).isEmpty());
        } finally {
            adapter.unRegister();
        }
    }

}
