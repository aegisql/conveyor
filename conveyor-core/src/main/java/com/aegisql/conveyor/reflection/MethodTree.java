package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class MethodTree {

    private final Map<String,Map<Class<?>,MethodTreeNode>> namesMap = new HashMap<>();

    private final Set<String> knownLabels = new HashSet<>();

    public MethodTree() {
    }

    public MethodTree(Object instance) {
        this(Objects.requireNonNull(instance,"Failed to build MethodTree. Instance is null").getClass());
    }

    public MethodTree(Class<?> c) {
        Arrays.stream(c.getDeclaredFields()).forEach(this::visitFields);
        Arrays.stream(c.getDeclaredMethods()).forEach(this::addMethod);
        Class sClass = c.getSuperclass();
        if(sClass != null) {
            MethodTree inner = new MethodTree(sClass);
            inner.knownLabels.forEach(l->{
                if(knownLabels.contains(l)) {
                    throw new ConveyorRuntimeException("Found duplicate label "+l+" in "+c.getSimpleName()+" conflicting with "+sClass.getSimpleName());
                } else {
                    knownLabels.add(l);
                }
            });
            namesMap.putAll(inner.namesMap);
        }
    }

    private void visitFields(Field f) {
        NoLabel noLabel = f.getAnnotation(NoLabel.class);
        Label label     = f.getAnnotation(Label.class);
        if(noLabel != null) {
            if(label == null) {
                return;
            } else {
                throw new ConveyorRuntimeException("Field " + f + " has both @Label and @NoLabel annotations. Please remove one.");
            }
        }
        if(label != null) {
            Arrays.stream(label.value()).forEach(l->{
                if(knownLabels.contains(l)) {
                    throw new ConveyorRuntimeException("Duplicated label " + l + " found for field "+f);
                }
                knownLabels.add(l);
            });
        }
    }

    public void addMethod(Method method) {
        String name = method.getName();
        NoLabel noLabel = method.getAnnotation(NoLabel.class);
        Label label     = method.getAnnotation(Label.class);

        if(noLabel != null) {
            if(label == null) {
                return;
            } else {
                throw new ConveyorRuntimeException("Method " + method + " has both @Label and @NoLabel annotations. Please remove one.");
            }
        }

        Map<Class<?>, MethodTreeNode> parameterMap = namesMap.computeIfAbsent(name, n -> new HashMap<>());
        LinkedList<Class<?>> args = new LinkedList<>();
        args.addAll(Arrays.asList(method.getParameterTypes()));
        MethodTreeNode methodTreeNode;
        if(method.getParameterCount() == 0) {
            methodTreeNode = parameterMap.computeIfAbsent(null, pClass -> new MethodTreeNode());
            methodTreeNode.addNode(new LinkedList<>(),method,null, 0);
        } else {
            Class<?> pClass = args.pollFirst();
            methodTreeNode = parameterMap.computeIfAbsent(pClass, p -> new MethodTreeNode());
            methodTreeNode.addNode(args,method, pClass, 1);
        }
        if(label != null) {
            Arrays.stream(label.value())
                    .filter(l-> ! l.equals(name))
                    .peek(l->{
                        if(knownLabels.contains(l) || (namesMap.containsKey(l) && parameterMap != namesMap.get(l))) {
                            throw new ConveyorRuntimeException("Method "+method+" labeled as '"+l+"' already has entry with the same name: "+namesMap.get(l));
                        }
                        knownLabels.add(l);
                    })
                    .forEach(l->namesMap.put(l,parameterMap));
        }
    }

    public Method findMethod(String name, Class<?> ... args) {
        LinkedList<Class<?>> argList = new LinkedList<>();
        if(args != null) {
            argList.addAll(Arrays.asList(args));
        }
        if(namesMap.containsKey(name)) {
            Map<Class<?>, MethodTreeNode> parameterMap = namesMap.get(name);
            if(argList.size() == 0 && parameterMap.containsKey(null)) {
                return parameterMap.get(null).getMethod();
            } else {
                Class<?> first = argList.pollFirst();
                if(parameterMap.containsKey(first)) {
                    MethodTreeNode methodTreeNode = parameterMap.get(first);
                    return methodTreeNode.findMethod(argList);
                } else {
                    for(Class<?> cls: parameterMap.keySet()) {
                        if(cls.isAssignableFrom(first)) {
                            MethodTreeNode methodTreeNode = parameterMap.get(cls);
                            Method method = methodTreeNode.findMethod(argList);
                            parameterMap.put(first,methodTreeNode);
                            return method;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Set<Method> findMethodCandidates(String name, Class<?> ... args) {
        List<Method> candidates = new LinkedList<>();
        LinkedList<Class<?>> argList = new LinkedList<>();
        if(args != null) {
            argList.addAll(Arrays.asList(args));
        }
        if(namesMap.containsKey(name)) {
            Map<Class<?>, MethodTreeNode> parameterMap = namesMap.get(name);
            if(argList.size() == 0 && parameterMap.containsKey(null)) {
                candidates.add(parameterMap.get(null).getMethod());
            } else {
                Class<?> first = argList.pollFirst();
                if(first==null) {
                    List<Method> collect = parameterMap.values().stream().map(MethodTreeNode::getMethod).collect(Collectors.toList());
                    candidates.addAll(collect);
                } else {
                    MethodTreeNode methodTreeNode = parameterMap.get(first);
                    if(methodTreeNode != null) {
                        methodTreeNode.findMethodCandidates(argList, candidates);
                    } else {
                        for(Class<?> cls: parameterMap.keySet()) {
                            if(cls.isAssignableFrom(first)) {
                                methodTreeNode = parameterMap.get(cls);
                                parameterMap.put(first,methodTreeNode);
                                methodTreeNode.findMethodCandidates(argList, candidates);
                                return new HashSet<>(candidates);
                            }
                        }
                    }
                }
            }
        }
        return new HashSet<>(candidates);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MethodTree{");
        sb.append(namesMap);
        sb.append('}');
        return sb.toString();
    }
}
