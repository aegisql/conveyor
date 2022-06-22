package com.aegisql.conveyor;

import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

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
        var type = getType(name);
        try {

            if(knownConveyors.containsKey(type)) {
                return knownConveyors.get(type);
            }
            var objectName = new ObjectName(type);
            var conveyor = (Conveyor) mBeanServer.invoke(objectName, "conveyor", null, null);
            knownConveyors.put(type,conveyor);
            return conveyor;
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Conveyor with name '"+type +"' not found",e);
        }
    }

    /**
     * Register object name.
     *
     * @param conveyor    the conveyor
     * @param mbeanObject the mbean object
     */
    public void register(Conveyor conveyor, Object mbeanObject) {
        try {
            var mBeanInterface = conveyor.mBeanInterface();
            var type = getType(conveyor.getName());
            if(mBeanInterface != null) {
                var mbean = new StandardMBean(mbeanObject, mBeanInterface, false);
                var objectName = new ObjectName(type);
                synchronized (mBeanServer) {
                    unRegister(conveyor.getName());
                    mBeanServer.registerMBean(mbean, objectName);
                    knownConveyors.put(type,conveyor);
                }
                Conveyor.LOG.debug("Registered conveyor MBean {}", type);
            } else {
                knownConveyors.put(type,conveyor);
                Conveyor.LOG.debug("Registered conveyor {}", type);
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
            var type = getType(name);
            var objectName = new ObjectName(type);
            knownConveyors.remove(type);
            synchronized (mBeanServer) {
                if (mBeanServer.isRegistered(objectName)) {
                    Conveyor.LOG.warn("Unregister existing mbean with name {}", objectName);
                    mBeanServer.unregisterMBean(objectName);
                }
            }
        } catch (Exception e) {
            throw new ConveyorRuntimeException("Unregister conveyor with name '"+name +"' with issues",e);
        }
    }

    public void resetConveyor(String name) {
        knownConveyors.remove(getType(name));
    }

    public Set<String> getKnownConveyorNames() {
        return knownConveyors
                .keySet()
                .stream()
                .filter(Objects::nonNull)
                .map(longName -> {
                    var parts = longName.split("=");
                    return parts[1];
                })
                .collect(Collectors.toUnmodifiableSet());
    }

}
