package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public Function<Object,Object> offerGetter(String parametrizedLabel) {
        ParametrizedLabel pl = new ParametrizedLabel(aClass,parametrizedLabel);
        Function<Object,Object> getter;
        if(pl.isParametrized()) {
            getter = offerParametrizedGetter(pl);
        } else {
            getter = offerNonParametrizedGetter(pl);
        }
        return getter;
    }

    private Function<Object,Object> offerNonParametrizedGetter(ParametrizedLabel pl) {
        String label = pl.getLabel();
        Method method = methodTree.findMethod(label);
        if(method == null) {
            method = methodTree.findMethod(label, aClass);
        }
        if(method == null) {
            Field field = fieldsByName.get(label);
            if(field == null) {
                return Function.identity();
            } else {
                if(isStatic(field)) {
                    return b->getStatic(field);
                } else {
                    return b->get(field,b);
                }
            }
        } else {
            if(isStatic(method)) {
                Method finalMethod = method;
                switch (finalMethod.getParameterCount()) {
                    case 0:
                        return b -> invokeStatic(finalMethod);
                    case 1:
                        return b -> invokeStatic(finalMethod,b);
                    default:
                        throw new ConveyorRuntimeException("Too many parameters for getter "+method);
                }
            } else {
                Method finalMethod = method;
                return b->invoke(finalMethod,b);
            }
        }
    }

    private Function<Object,Object> offerParametrizedGetter(ParametrizedLabel pl) {
        String label = pl.getLabel();
        Object[] properties = pl.getProperties();
        Class<?>[] pClass = new Class[properties.length];
        Arrays.fill(pClass,String.class);

        Method method = methodTree.findMethod(label, pClass);
        if(method != null) {
            if(isStatic(method)) {
                Method finalMethod = method;
                return b->invokeStatic(finalMethod, properties);
            } else {
                Method finalMethod = method;
                return b->invoke(finalMethod, b, properties);
            }
        }
        pClass = appendFirstClass(aClass,pClass);
        method = methodTree.findMethod(label, pClass);
        if(method != null) {
            if(isStatic(method)) {
                Method finalMethod = method;
                return b->invokeStatic(finalMethod, appendFirstArgs(b,properties));
            } else {
                Method finalMethod = method;
                return b->invoke(finalMethod, b, properties);
            }
        }
        return x->{throw new ConveyorRuntimeException("getter not found for "+label);};
    }

    public BiConsumer<Object,Object> offerSetter(String parametrizedLabel, Class<?> vClass) {
        ParametrizedLabel pl = new ParametrizedLabel(aClass,parametrizedLabel);
        BiConsumer<Object,Object> setter;
        if(pl.isParametrized()) {
            if(vClass == null) {
                setter = offerParametrizedSetter(pl);
            } else {
                setter = offerParametrizedSetter(pl, vClass);
            }
        } else {
            if(vClass == null) {
                setter = offerNonParametrizedSetter(pl);
            } else {
                setter = offerNonParametrizedSetter(pl, vClass);
            }
        }
        return setter;
    }

    private BiConsumer<Object, Object> offerNonParametrizedSetter(ParametrizedLabel pl, Class<?> vClass) {
        String label = pl.getLabel();
        Method candidate = methodTree.findMethod(label,vClass);
        if(candidate != null) {
            return (b,v)->invoke(candidate,b,v);
        }
        Method candidate2 = methodTree.findMethod(label,aClass,vClass);
        if(candidate2 != null && isStatic(candidate2)) {
            return (b,v)->invokeStatic(candidate2,b,v);
        }
        return fieldSetter(label);

    }

    private BiConsumer<Object, Object> offerNonParametrizedSetter(ParametrizedLabel pl) {
        String label = pl.getLabel();
        Method candidate;
        Set<Method> candidates = methodTree.findMethodCandidates(label,null);
        if(candidates.size() > 1) {
            throw new ConveyorRuntimeException("More than one setter candidate found for "+label);
        } if(candidates.size() == 1) {
            candidate = candidates.iterator().next();
            if(isStatic(candidate)) {
                return (b, v) -> {
                    invokeStatic(candidate, args(candidate,b,null));
                };
            } else {
                return (b, v) -> {
                    invoke(candidate, b, args(candidate,new Object[]{null}));
                };
            }
        }

        Set<Method> candidates2 = methodTree.findMethodCandidates(label,aClass,null);
        if(candidates2.size() > 1) {
            throw new ConveyorRuntimeException("More than one setter candidate found for "+label);
        } if(candidates2.size() == 1) {
            candidate = candidates2.iterator().next();
            if(isStatic(candidate)) {
                return (b, v) -> {
                    invokeStatic(candidate, args(candidate,b,null));
                };
            } else {
                return (b, v) -> {
                    invoke(candidate, b, args(candidate,new Object[]{null}));
                };
            }
        }

        return fieldSetter(label);
    }

    private BiConsumer<Object, Object> offerParametrizedSetter(ParametrizedLabel pl, Class<?> vClass) {
        String label = pl.getLabel();
        Object[] properties = pl.getProperties();
        Class<?>[] types = new Class[properties.length];
        Arrays.fill(types,String.class);
        Method candidate = methodTree.findMethod(label,appendLastClass(vClass,types));
        if(candidate != null) {
            return (b,v)->invoke(candidate,b,appendLastArgs(v,properties));
        }
        Method candidate2 = methodTree.findMethod(label,appendLastClass(vClass,appendFirstClass(aClass,types)));
        if(candidate2 != null) {
            return (b,v)->invoke(candidate2,b,appendLastArgs(v,appendFirstArgs(b,properties)));
        }
        return (b,v)->{throw new ConveyorRuntimeException("Not found setter for "+pl);};
    }

    private BiConsumer<Object, Object> offerParametrizedSetter(ParametrizedLabel pl) {
        String label = pl.getLabel();
        Object[] properties = pl.getProperties();
        Class<?>[] types = new Class[properties.length];
        Arrays.fill(types,String.class);
        Set<Method> candidates = new HashSet<>(methodTree.findMethodCandidates(label, appendLastClass(null, types)));
        if(candidates.size() > 1) {
            throw new ConveyorRuntimeException("Found several setter candidates for "+pl);
        } else if(candidates.size() == 1) {
            Method candidate = candidates.iterator().next();
            return (b,v)->invoke(candidate,b,appendLastArgs(null,properties));
        }
        candidates = new HashSet<>(methodTree.findMethodCandidates(label,appendToEdgesClass(aClass,null,types)));
        if(candidates.size() > 1) {
            throw new ConveyorRuntimeException("Found several setter candidates for "+pl);
        } else if(candidates.size() == 1) {
            Method candidate = candidates.iterator().next();
            return (b,v)->invoke(candidate,b,appendToEdgesArgs(b,null,properties));
        }
        return (b,v)->{throw new ConveyorRuntimeException("Not found setter for "+pl);};
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

    private boolean isStatic(Member m) {
        return Modifier.isStatic(m.getModifiers());
    }

    private boolean isGetter(Method m) {
        return m != null && ! Void.TYPE.equals(m.getReturnType());
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

    private Object invokeStatic(final Method method, Object... params) {
        return invoke(method,null,params);
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

    private Object getStatic(Field field) {
        return get(field,null);
    }

    private void set(final Field field, Object builder, Object val) {
        try {
            field.setAccessible(true);
            field.set(builder,val);
        } catch (IllegalAccessException e) {
            throw new ConveyorRuntimeException(e);
        }
    }

    private void setStatic(final Field field, Object val) {
        set(field,null,val);
    }

    private BiConsumer<Object, Object> fieldSetter(String label) {
        Field field = fieldsByName.get(label);
        if(field != null) {
            if(isStatic(field)) {
                return (b,v)->setStatic(field,v);
            } else {
                return (b, v)->set(field, b, v);
            }
        } else {
            return (b,v)->{throw new ConveyorRuntimeException("No setter found for "+label);};
        }
    }

    private Object[] args(Method method, Object... args) {
        if(args==null) {
            args = new Object[]{null};
        }
        int size = method.getParameterCount();
        return Arrays.copyOfRange(args, 0, size);
    }

    private Class<?>[] appendFirstClass(Class<?> first, Class<?>... other) {
        if(other == null) {
            return new Class[]{first};
        } else {
            Class<?>[] res = new Class[other.length+1];
            res[0] = first;
            for(int i = 1; i <= other.length; i++) {
                res[i] = other[i-1];
            }
            return res;
        }
    }

    private Class<?>[] appendLastClass(Class<?> first, Class<?>... other) {
        if(other == null) {
            return new Class[]{first};
        } else {
            Class<?>[] res = new Class[other.length+1];
            for(int i = 0; i < other.length; i++) {
                res[i] = other[i];
            }
            res[other.length] = first;
            return res;
        }
    }

    private Class<?>[] appendToEdgesClass(Class<?> first, Class<?> last, Class<?>... other) {
        if(other == null) {
            return new Class[]{first,last};
        } else {
            Class<?>[] res = new Class[other.length+2];
            for(int i = 1; i <= other.length; i++) {
                res[i] = other[i-1];
            }
            res[0] = first;
            res[res.length-1] = last;
            return res;
        }
    }

    private Object[] appendToEdgesArgs(Object first, Object last, Object... other) {
        if(other == null) {
            return new Object[]{first,last};
        } else {
            Object[] res = new Object[other.length+2];
            for(int i = 1; i <= other.length; i++) {
                res[i] = other[i-1];
            }
            res[0] = first;
            res[res.length-1] = last;
            return res;
        }
    }

    private Object[] appendFirstArgs(Object first, Object... other) {
        if(other == null) {
            return new Object[]{first};
        } else {
            Object[] res = new Object[other.length+1];
            res[0] = first;
            for(int i = 1; i <= other.length; i++) {
                res[i] = other[i-1];
            }
            return res;
        }
    }

    private Object[] appendLastArgs(Object first, Object... other) {
        if(other == null) {
            return new Object[]{first};
        } else {
            Object[] res = new Object[other.length+1];
            for(int i = 0; i < other.length; i++) {
                res[i] = other[i];
            }
            res[other.length] = first;
            return res;
        }
    }

}
