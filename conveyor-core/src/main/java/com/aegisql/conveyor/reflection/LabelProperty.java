package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class LabelProperty {


    final static Map<String, Function<String,?>> CONVERSION_MAP = new HashMap<>();

    final static Map<String,Class<?>> CLASS_MAP = new HashMap<>();

    final static Function<String,?> voidConstructorSupplier = type->{
        if(CLASS_MAP.containsKey(type)) {
            try {
                Constructor<?> constructor = CLASS_MAP.get(type).getConstructor(null);
                constructor.setAccessible(true);
                return constructor.newInstance(null);
            } catch (Exception e) {
                throw new ConveyorRuntimeException(e);
            }
        }
        try {
            Class<?> aClass = Class.forName(type);
            CLASS_MAP.put(type,aClass);
            return aClass.getConstructor(null).newInstance(null);
        } catch (Exception e) {
            throw new ConveyorRuntimeException(e);
        }
    };

    final static Function<String,Function<?,?>> defaultSupplier = alias->{
        return type->{
            if(CLASS_MAP.containsKey(type)) {
                try {
                    Constructor<?> constructor = CLASS_MAP.get(type).getConstructor(null);
                    constructor.setAccessible(true);
                    return constructor.newInstance(null);
                } catch (Exception e) {
                    throw new ConveyorRuntimeException(e);
                }
            }
            if(CLASS_MAP.containsKey(alias)) {
                try {
                    Constructor<?> constructor = CLASS_MAP.get(alias).getConstructor(null);
                    constructor.setAccessible(true);
                    return constructor.newInstance(null);
                } catch (Exception e) {
                    throw new ConveyorRuntimeException(e);
                }
            }
            try {
                return Class.forName(alias).getConstructor(null).newInstance(null);
            } catch (Exception e) {
                throw new ConveyorRuntimeException(e);
            }
        };
    };

    static {
        CLASS_MAP.put("s",String.class);
        CLASS_MAP.put("str",String.class);
        CLASS_MAP.put("string",String.class);
        CLASS_MAP.put("String",String.class);

        CLASS_MAP.put("i",int.class);
        CLASS_MAP.put("int",int.class);
        CLASS_MAP.put("integer",int.class);
        CLASS_MAP.put("I",Integer.class);
        CLASS_MAP.put("Int",Integer.class);
        CLASS_MAP.put("Integer",Integer.class);

        CLASS_MAP.put("l",long.class);
        CLASS_MAP.put("long",long.class);
        CLASS_MAP.put("L",Long.class);
        CLASS_MAP.put("Long",Long.class);

        CLASS_MAP.put("b",byte.class);
        CLASS_MAP.put("byte",byte.class);
        CLASS_MAP.put("B",Byte.class);
        CLASS_MAP.put("Byte",Byte.class);

        CLASS_MAP.put("bool",boolean.class);
        CLASS_MAP.put("boolean",boolean.class);
        CLASS_MAP.put("Bool",Boolean.class);
        CLASS_MAP.put("Boolean",Boolean.class);

        CLASS_MAP.put("c",char.class);
        CLASS_MAP.put("ch",char.class);
        CLASS_MAP.put("char",char.class);
        CLASS_MAP.put("C",Character.class);
        CLASS_MAP.put("Ch",Character.class);
        CLASS_MAP.put("Char",Character.class);

        CLASS_MAP.put("d",double.class);
        CLASS_MAP.put("double",double.class);
        CLASS_MAP.put("D",Double.class);
        CLASS_MAP.put("Double",Double.class);

        CLASS_MAP.put("f",float.class);
        CLASS_MAP.put("float",float.class);
        CLASS_MAP.put("F",Float.class);
        CLASS_MAP.put("Float",Float.class);

        CLASS_MAP.put("new",Function.class);
        CLASS_MAP.put("key->new",Function.class);

        CLASS_MAP.put("list", ArrayList.class);
        CLASS_MAP.put("map", HashMap.class);

        CONVERSION_MAP.put(String.class.getName(),str->fromConstructor(String.class,str));
        CONVERSION_MAP.put(Character.class.getName(),str->fromValueOf(Character.class,str));
        CONVERSION_MAP.put(Integer.class.getName(),str->fromValueOf(Integer.class,str));
        CONVERSION_MAP.put(Long.class.getName(),str->fromValueOf(Long.class,str));
        CONVERSION_MAP.put(Byte.class.getName(),str->fromValueOf(Byte.class,str));
        CONVERSION_MAP.put(Double.class.getName(),str->fromValueOf(Double.class,str));
        CONVERSION_MAP.put(Float.class.getName(),str->fromValueOf(Float.class,str));
        CONVERSION_MAP.put(Boolean.class.getName(),str->fromValueOf(Boolean.class,str));

        CONVERSION_MAP.put(char.class.getName(),str->fromValueOf(Character.class,str));
        CONVERSION_MAP.put(int.class.getName(),str->fromValueOf(Integer.class,str));
        CONVERSION_MAP.put(long.class.getName(),str->fromValueOf(Long.class,str));
        CONVERSION_MAP.put(byte.class.getName(),str->fromValueOf(Byte.class,str));
        CONVERSION_MAP.put(double.class.getName(),str->fromValueOf(Double.class,str));
        CONVERSION_MAP.put(float.class.getName(),str->fromValueOf(Float.class,str));
        CONVERSION_MAP.put(boolean.class.getName(),str->fromValueOf(Boolean.class,str));

        CONVERSION_MAP.put("key->new",defaultSupplier);
        CONVERSION_MAP.put("new",voidConstructorSupplier);
    }

    private final String propertyStr;
    private final Class<?> propertyType;
    private final boolean builder;
    private final boolean value;
    private final String typeAlias;

    private final Function<String,?> defaultConverter = cls -> {
        try {
            Constructor<?> constructor = getPropertyType().getConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(cls);
        } catch (Exception e) {
        }
        try {
            Method valueOf = getPropertyType().getMethod("valueOf", String.class);
            valueOf.setAccessible(true);
            return valueOf.invoke(null, cls);
        } catch (Exception e) {}
        throw new ConveyorRuntimeException("Failed to find instantiation method for " + cls);
    };

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

    private String removeQuotes(String s) {
        if(s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1,s.lastIndexOf("'"));
        }
        return s;
    }
    public LabelProperty(String p) {
        this(p,false);
    }

    LabelProperty(String p, boolean forField) {
        String property = Objects.requireNonNull(p,"Requires property").trim();
        if(property.startsWith("'") && property.endsWith("'")) {
            property = removeQuotes(property);
            propertyType = String.class;
            builder = false;
            value = false;
            typeAlias = "s";
        } else {
            String[] parts = property.split("\\s+",2);
            if(parts.length == 2) {
                this.typeAlias = parts[0];
                if(CLASS_MAP.containsKey(parts[0])) {
                    property = removeQuotes(parts[1]);
                    propertyType = CLASS_MAP.get(parts[0]);
                    builder = false;
                    value = false;
                } else {
                    Class<?> aClass = toClass(parts[0]);
                    if(aClass == null) {
                        propertyType = forField ? null : String.class;
                        builder = false;
                        value = false;
                    } else {
                        property = removeQuotes(parts[1]);
                        propertyType = CLASS_MAP.computeIfAbsent(parts[0], typeName->aClass);
                        builder = false;
                        value = false;
                    }
                }
            } else {
                this.typeAlias = null;
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
                    propertyType = forField ? null : String.class;
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
            Function<String, ?> supplier;
            if(CONVERSION_MAP.containsKey(this.typeAlias)){
                supplier = CONVERSION_MAP.get(this.typeAlias);
            } else {
                supplier = CONVERSION_MAP.computeIfAbsent(propertyType.getName(), type -> defaultConverter);
            }
            return supplier.apply(propertyStr);
        }
    }

    String getTypeAlias() {
        return typeAlias;
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
