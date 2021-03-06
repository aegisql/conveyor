package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum ConvertersRegister {
    KEY_CONVERTERS,
    LABEL_CONVERTERS,
    VALUE_CONVERTERS;

    private final Map<String, Map<String, Function>> register = new HashMap<>();

    public Function getConverter(String conveyorName, String typeName) {
        if (register.containsKey(conveyorName)) {
            return register.get(conveyorName).get(typeName);
        }
        return null;
    }

    public Function getConverter(String conveyorName, Class type) {
        return getConverter(conveyorName, type.getCanonicalName());
    }

    public void setConverter(String conveyorName, String typeName, Function converter) {
        Map<String, Function> functionMap = register.computeIfAbsent(conveyorName, c -> new HashMap<>());
        functionMap.put(typeName, converter);
    }

    public void setConverter(String conveyorName, Class type, Function converter) {
        setConverter(conveyorName, type.getCanonicalName(), converter);
    }

}