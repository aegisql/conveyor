package com.aegisql.conveyor;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * The singleton MBean register.
 */
enum MBeanRegister {

    /**
     * Mbean m bean register.
     */
    MBEAN;

    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private final Map<String,Conveyor> knownConveyors = new HashMap<>();

    private String getType(String name) {
        return "com.aegisql.conveyor:type="+name;
    }

    /**
     * By name conveyor.
     *
     * @param name the name
     * @return the conveyor
     */
    public Conveyor byName(String name) {
        try {
            String type = getType(name);
            if(knownConveyors.containsKey(type)) {
                return knownConveyors.get(type);
            }
            ObjectName objectName = new ObjectName(type);
            Conveyor res = (Conveyor) mBeanServer.invoke(objectName, "conveyor", null, null);
            knownConveyors.put(type,res);
            return res;
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Conveyor with name '"+name +"' not found",e);
        }
    }

    /**
     * Register object name.
     *
     * @param conveyor    the conveyor
     * @param mbeanObject the mbean object
     * @return the object name
     */
    public ObjectName register(Conveyor conveyor, Object mbeanObject) {
        try {
            Class mBeanInterface = conveyor.mBeanInterface();
            String type = getType(conveyor.getName());
            if(mBeanInterface != null) {
                Object mbean = new StandardMBean(mbeanObject, mBeanInterface, false);
                ObjectName objectName = new ObjectName(type);
                synchronized (mBeanServer) {
                    unRegister(conveyor.getName());
                    mBeanServer.registerMBean(mbean, objectName);
                    knownConveyors.put(type,conveyor);
                }
                return objectName;
            } else {
                knownConveyors.put(type,conveyor);
                return null;
            }
        } catch( ConveyorRuntimeException cre) {
          throw cre;
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Error registering conveyor " + conveyor.getName(), e);
        }
    }

    /**
     * Un register.
     *
     * @param name the name
     */
    public void unRegister(String name) {
        try {
            String type = getType(name);
            ObjectName objectName = new ObjectName(type);
            knownConveyors.remove(type);
            synchronized (mBeanServer) {
                if (mBeanServer.isRegistered(objectName)) {
                    Conveyor.LOG.warn("Unregister existing mbean with name {}", objectName);
                    mBeanServer.unregisterMBean(objectName);
                }
            }
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Conveyor with name '"+name +"' not found",e);
        }
    }

}
