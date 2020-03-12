package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodTreeNode {

    private Class<?> myClass;
    private Map<Class<?>,MethodTreeNode> parameterMap = new HashMap<>();
    private Method method;
    private int pos;

    public MethodTreeNode() {
    }

    public void addNode(LinkedList<Class<?>> classes, Method method, Class<?> pClass, int pos) {
        this.myClass = pClass;
        this.pos = pos;
        if(classes.size() == 0) {
            this.method = method;
        } else {
            MethodTreeNode methodTreeNode = parameterMap.computeIfAbsent(classes.pollFirst(), k -> new MethodTreeNode());
            methodTreeNode.addNode(classes,method, pClass, pos+1);
        }
    }

    public Method findMethod(LinkedList<Class<?>> argList) {
        if(argList.size()==0) {
            return method;
        } else {
            Class<?> next = argList.pollFirst();
            if(parameterMap.containsKey(next)) {
                return parameterMap.get(next).findMethod(argList);
            } else {
                for(Class<?> cls: parameterMap.keySet()) {
                    if(cls.isAssignableFrom(next)) {
                        MethodTreeNode methodTreeNode = parameterMap.get(cls);
                        Method method = methodTreeNode.findMethod(argList);
                        parameterMap.put(next,methodTreeNode);
                        return method;
                    }
                }
                throw new ConveyorRuntimeException("findMethod failed to find assignable class "+next+" in "+parameterMap.keySet());

            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MethodTreeNode{");
        sb.append("class=").append(myClass);
        sb.append(", parameters=").append(parameterMap);
        sb.append(", method=").append(method);
        sb.append(", pos=").append(pos);
        sb.append('}');
        return sb.toString();
    }

    public Method getMethod() {
        return method;
    }

    public void findMethodCandidates(LinkedList<Class<?>> argList, List<Method> candidates) {
        if(argList.size()==0) {
            candidates.add(method);
        } else {
            Class<?> next = argList.pollFirst();
            if(next == null) {
                List<Method> collect = parameterMap.values().stream().map(MethodTreeNode::getMethod).collect(Collectors.toList());
                candidates.addAll(collect);
            } else {
                MethodTreeNode methodTreeNode = parameterMap.get(next);
                if(methodTreeNode != null) {
                    methodTreeNode.findMethodCandidates(argList, candidates);
                } else {
                    for(Class<?> cls: parameterMap.keySet()) {
                        if(cls.isAssignableFrom(next)) {
                            methodTreeNode = parameterMap.get(cls);
                            parameterMap.put(next,methodTreeNode);
                            methodTreeNode.findMethodCandidates(argList, candidates);
                            return;
                        }
                    }
                    throw new ConveyorRuntimeException("findMethodCandidates failed to find assignable class "+next+" in "+parameterMap.keySet());
                }
            }
        }

    }
}
