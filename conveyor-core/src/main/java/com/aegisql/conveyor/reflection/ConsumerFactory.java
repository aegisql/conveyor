package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ConsumerFactory {

    private final Class<?> aClass;
    private final MethodTree methodTree;
    private final Map<String, Field> fieldsByName = new HashMap<>();

    public ConsumerFactory(Class<?> aClass) {
        Objects.requireNonNull(aClass,"Builder class is null");
        this.aClass = aClass;
        this.methodTree = new MethodTree(aClass);
        seedFields(aClass);
    }

    public BiFunction<Object,Object,Object> offerGetter(String parametrizedLabel, Class vClass) {
        ParametrizedLabel pl = new ParametrizedLabel(aClass,parametrizedLabel);
        Method method = null;
        if(vClass != null || pl.hasValueType()) {
            method = methodTree.findMethod(pl.getLabel(),pl.getClassesForGetter(aClass,vClass));
        } else {
            Set<Method> methods = methodTree.findMethodCandidates(pl.getLabel(),pl.getClassesForGetter(aClass,vClass));
            if(methods.size() > 1) {
                throw new ConveyorRuntimeException("More than one getter method candidates found for "+pl+"; "+methods);
            } else if(methods.size() == 1) {
                method = methods.iterator().next();
            }
        }
        if(method != null) {
            Method finalMethod = method;
            return (b, v)->invoke(finalMethod,b,pl.getPropertiesForGetter(b,v));
        } else {
            Field field = fieldsByName.get(pl.getLabel());
            if(field == null) {
                return (b,v)->b;
            } else {
                return (b,v)->get(field,b);
            }
        }
    }

    public BiConsumer<Object,Object> offerSetter(String parametrizedLabel, Class<?> vClass) {
        ParametrizedLabel pl = new ParametrizedLabel(aClass,parametrizedLabel);
        BiConsumer<Object,Object> setter;
        Method method = null;
        if(vClass != null || pl.hasValueType()) {
            method = methodTree.findMethod(pl.getLabel(),pl.getClassesForSetter(aClass,vClass));
        } else {
            Set<Method> methods = methodTree.findMethodCandidates(pl.getLabel(),pl.getClassesForSetter(aClass,vClass));
            if(methods.size() > 1) {
                throw new ConveyorRuntimeException("More than one setter method candidates found for "+pl+"; "+methods);
            } else if(methods.size() == 1) {
                method = methods.iterator().next();
            }
        }
        if(method != null) {
            Method finalMethod = method;
            if(method.getParameterCount() == 0) {
                return (b, v)->invoke(finalMethod,b,null);
            } else {
                return (b, v) -> invoke(finalMethod, b, pl.getPropertiesForSetter(b, v));
            }
        } else {
            return fieldSetter(pl.getLabel());
        }
    }

///////////////////////////////////////////

    private void seedFields(Class bClass) {
        Label label;
        NoLabel noLabel;
        String name;
        for (Field f : bClass.getDeclaredFields()) {
            label = f.getAnnotation(Label.class);
            noLabel = f.getAnnotation(NoLabel.class);
            name = f.getName();
            setField(name,f);
            if(label != null) {
                if(noLabel != null) {
                    throw new ConveyorRuntimeException("Field " + f + " annotated with both @Label and @NoLabel. Please remove one.");
                } else {
                    for (String lbl : label.value()) {
                        setField(lbl,f);
                    }
                }
            }
        }
        Class sClass = bClass.getSuperclass();
        if(sClass != null) {
            seedFields(sClass);
        }
    }

    private void setField(String name, Field f){
        if ( ! fieldsByName.containsKey(name) ) {
            fieldsByName.put(name, f);
        }
    }

    private Object invoke(final Method method, Object builder, Object... params) {
        try {
            method.setAccessible(true);
            return method.invoke(builder, params);
        } catch (IllegalAccessException e) {
            throw new ConveyorRuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new ConveyorRuntimeException(e);
        }
    }

    private Object get(final Field field, Object builder) {
        try {
            field.setAccessible(true);
            Object o = field.get(builder);
            if(o==null) {
                Constructor<?> constructor = field.getType().getConstructor(null);
                Object newInstance = constructor.newInstance(null);
                field.setAccessible(true);
                field.set(builder,newInstance);
                return newInstance;
            }
            return o;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            throw new ConveyorRuntimeException(e);
        }
    }

    private void set(final Field field, Object builder, Object val) {
        try {
            field.setAccessible(true);
            field.set(builder,val);
        } catch (IllegalAccessException e) {
            throw new ConveyorRuntimeException(e);
        }
    }

    private BiConsumer<Object, Object> fieldSetter(String label) {
        Field field = fieldsByName.get(label);
        if(field != null) {
            return (b, v)->set(field, b, v);
        } else {
            return (b,v)->{throw new ConveyorRuntimeException("No setter found for "+label);};
        }
    }

}
