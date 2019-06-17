package com.aegisql.conveyor;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

enum MBeanRegister {

    MBEAN;

    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private final Map<String,Conveyor> knownConveyors = new HashMap<>();

    private String getType(String name) {
        return "com.aegisql.conveyor:type="+name;
    }

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

    public ObjectName register(Conveyor conveyor, Object mbeanObject) {
        try {
            if(mbeanObject.getClass().getInterfaces().length != 1) {
                throw new ConveyorRuntimeException("Conveyor Mbean Object must implement a single interface");
            }
            String type = getType(conveyor.getName());
            Object mbean = new StandardMBean(mbeanObject, (Class) mbeanObject.getClass().getInterfaces()[0], false);
            ObjectName objectName = new ObjectName(type);
            synchronized (mBeanServer) {
                unRegister(conveyor.getName());
                mBeanServer.registerMBean(mbean, objectName);
                knownConveyors.put(type,conveyor);
            }
            return objectName;
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Error registering conveyor " + conveyor.getName(), e);
        }
    }

    public void unRegister(String name) {
        try {
            String type = getType(name);
            ObjectName objectName = new ObjectName(type);
            synchronized (mBeanServer) {
                if (mBeanServer.isRegistered(objectName)) {
                    Conveyor.LOG.warn("Unregister existing mbean with name {}", objectName);
                    mBeanServer.unregisterMBean(objectName);
                    knownConveyors.remove(type);
                }
            }
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Conveyor with name '"+name +"' not found",e);
        }
    }

}
