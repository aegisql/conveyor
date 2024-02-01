package com.aegisql.conveyor;

import com.aegisql.conveyor.exception.ConveyorRuntimeException;
import org.junit.Test;

import static com.aegisql.conveyor.MBeanRegister.MBEAN;
import static org.junit.Assert.*;

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

    @Test(expected = ConveyorRuntimeException.class)
    public void mbeanRegistryConvExceptionTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>(){
            public Class<?> mBeanInterface() {
                throw new ConveyorRuntimeException("test me");
            }
        };
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void mbeanRegistryNPExceptionTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>(){
            public Class<?> mBeanInterface() {
                throw new NullPointerException("test me with null");
            }
        };
    }

}