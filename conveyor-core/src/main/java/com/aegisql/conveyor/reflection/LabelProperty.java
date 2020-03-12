package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class LabelProperty {

    final static Map<Class<?>, Function<String,Object>> CONVERSION_MAP = new HashMap<>();

    final static Map<String,Class<?>> CLASS_MAP = new HashMap<>();
    static {
        CLASS_MAP.put("s",String.class);
        CLASS_MAP.put("str",String.class);
        CLASS_MAP.put("string",String.class);

        CLASS_MAP.put("i",Integer.class);
        CLASS_MAP.put("int",Integer.class);
        CLASS_MAP.put("integer",Integer.class);

        CLASS_MAP.put("l",Long.class);
        CLASS_MAP.put("long",Long.class);

        CLASS_MAP.put("b",Byte.class);
        CLASS_MAP.put("byte",Byte.class);

        CLASS_MAP.put("c",Character.class);
        CLASS_MAP.put("ch",Character.class);
        CLASS_MAP.put("char",Character.class);

        CLASS_MAP.put("d",Double.class);
        CLASS_MAP.put("double",Double.class);
        CLASS_MAP.put("f",Float.class);
        CLASS_MAP.put("float",Float.class);

        CONVERSION_MAP.put(String.class,str->fromConstructor(String.class,str));
        CONVERSION_MAP.put(Integer.class,str->fromValueOf(Integer.class,str));
        CONVERSION_MAP.put(Long.class,str->fromValueOf(Long.class,str));
        CONVERSION_MAP.put(Byte.class,str->fromValueOf(Byte.class,str));
        CONVERSION_MAP.put(Double.class,str->fromValueOf(Double.class,str));
        CONVERSION_MAP.put(Float.class,str->fromValueOf(Float.class,str));
    }

    private final String propertyStr;
    private final Class<?> propertyType;
    private final boolean builder;
    private final boolean value;

    private static Object fromConstructor(Class<?> aClass, String val) {
        try {
            Constructor<?> constructor = aClass.getConstructor(String.class);
            return constructor.newInstance(val);
        } catch (Exception e) {
            throw new ConveyorRuntimeException(e);
        }
    }

    private static Object fromValueOf(Class<?> aClass, String val) {
        try {
            Method valueOf = aClass.getMethod("valueOf", String.class);
            return valueOf.invoke(null,val);
        } catch (Exception e) {
            throw new ConveyorRuntimeException(e);
        }
    }


    public LabelProperty(String p) {
        String property = Objects.requireNonNull(p,"Requires property").trim();
        if(property.startsWith("'") && property.endsWith("'")) {
            property = property.substring(1,property.lastIndexOf("'"));
            propertyType = String.class;
            builder = false;
            value = false;
        } else {
            String[] parts = property.split("\\s+",2);
            if(parts.length == 2) {
                if(CLASS_MAP.containsKey(parts[0])) {
                    property = parts[1];
                    propertyType = CLASS_MAP.get(parts[0]);
                    builder = false;
                    value = false;
                } else {
                    Class<?> aClass = toClass(parts[0]);
                    if(aClass == null) {
                        propertyType = String.class;
                        builder = false;
                        value = false;
                    } else {
                        property = parts[1];
                        propertyType = CLASS_MAP.computeIfAbsent(parts[0], typeName->aClass);
                        builder = false;
                        value = false;
                    }
                }
            } else {
                if("#".equals(property)) {
                    propertyType = null;
                    builder = true;
                    value = false;
                } else if(property.startsWith("$")) {
                    value = true;
                    builder = false;
                    if(property.length() > 1) {
                        String vClass = property.substring(1);
                        if(CLASS_MAP.containsKey(vClass)) {
                            propertyType = CLASS_MAP.get(vClass);
                        } else {
                            propertyType = toClass(vClass);
                        }
                        property = "$";
                    } else {
                        propertyType = null;
                    }
                } else {
                    propertyType = String.class;
                    builder = false;
                    value = false;
                }
            }
        }
        this.propertyStr = property;
    }

    private Class<?> toClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String getPropertyStr() {
        return propertyStr;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public boolean isBuilder() {
        return builder;
    }

    public boolean isValue() {
        return value;
    }

    public Object getProperty() {
        if(propertyType == null){
            return null;
        } else if(value) {
            return null;
        } else {
            Function<String, Object> supplier = CONVERSION_MAP.computeIfAbsent(propertyType, type ->
                    cls -> {
                        try {
                            Constructor<?> constructor = propertyType.getConstructor(String.class);
                            return constructor.newInstance(propertyStr);
                        } catch (Exception e) {
                        }
                        try {
                            Method valueOf = propertyType.getMethod("valueOf", String.class);
                            return valueOf.invoke(null, propertyStr);
                        } catch (Exception e) {
                        }
                        throw new ConveyorRuntimeException("Failed to find instantiation method for " + cls);
                    }
            );
            return supplier.apply(propertyStr);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LabelProperty{");
        sb.append("propertyStr='").append(propertyStr).append('\'');
        sb.append(", propertyType=").append(propertyType);
        sb.append(", builder=").append(builder);
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
