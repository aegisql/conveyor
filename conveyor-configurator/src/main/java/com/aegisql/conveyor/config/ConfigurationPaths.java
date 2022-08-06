package com.aegisql.conveyor.config;

import com.aegisql.java_path.ClassRegistry;
import com.aegisql.java_path.JavaPath;
import com.aegisql.java_path.StringConverter;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationPaths {

    private final Map<String, JavaPath> paths = new ConcurrentHashMap<>();
    private final Map<String, Object> objects = new ConcurrentHashMap<>();

    private final ClassRegistry classRegistry = new ClassRegistry();

    public ConfigurationPaths() {
        classRegistry.registerStringConverter(Duration.class, new StringConverter<Duration>() {
            @Override
            public Duration apply(String s) {
                return Duration.ofMillis(Long.parseLong(s));
            }
        });
        register(Duration.class.getName());
        register(Duration.class.getSimpleName());
    }

    public JavaPath register(String path) {
        Objects.requireNonNull(path,"path is null");
        String[] elements = path.trim().split("\\s+", 2);
        String classCandidate = elements[0].replace("(","");
        try {
            JavaPath jp;
            if(paths.containsKey(classCandidate)) {
                jp = paths.get(classCandidate);
            } else {
                Class<?> aClass = Class.forName(classCandidate);
                classRegistry.registerClass(aClass, aClass.getName(), aClass.getSimpleName());
                jp = new JavaPath(aClass, classRegistry);
                jp.setEnablePathCaching(true);
                paths.putIfAbsent(aClass.getName(), jp);
                paths.putIfAbsent(aClass.getSimpleName(), jp);
            }

            if(elements.length == 2) {
                if(! path.trim().startsWith("(") && ! path.endsWith(").@")) {
                    path = "("+path+").@";
                }
                Object o = jp.initPath(path);
                objects.putIfAbsent(elements[1],o);
            }
            return jp;
        } catch (ClassNotFoundException e) {
            throw new ConveyorConfigurationException("Failed to find class for "+classCandidate,e);
        }
    }

    public Object evalPath(String path) {

        Objects.requireNonNull(path,"Expected non NULL path for evaluation");

        if(path.startsWith("(") && path.contains(").")) {
            register(path.substring(1,path.indexOf(").")));
            String[] split = path.split("\\s+|\\)\\.", 3);
            path=split[1]+"."+split[2];
        }

        String[] elements = path.trim().split("\\.", 2);
        Object obj = objects.get(elements[0]);
        Objects.requireNonNull(obj,"Could not find initialized object for path "+path);
        if (! path.contains(obj.getClass().getName())) {
            register(obj.getClass().getName());
        }
        JavaPath javaPath = paths.get(obj.getClass().getName());

        return javaPath.evalPath(elements[1]+".@",obj);
    }

    public boolean containsPath(String path) {
        return paths.containsKey(path);
    }

    public boolean containsObject(String name) {
        return objects.containsKey(name);
    }

    public Object get(String name) {
        return objects.get(name);
    }

    public void registerBean(Object bean, String... names) {
        Objects.requireNonNull(bean,"Cannot register NULL object as a bean "+names==null?"":String.join(",",names));
        register(bean.getClass().getName());
        char[] simpleNameChar = bean.getClass().getSimpleName().toCharArray();
        simpleNameChar[0] = Character.toLowerCase(simpleNameChar[0]);
        String simpleName = new String(simpleNameChar);
        objects.put(simpleName,bean);
        if(names!=null) {
            Arrays.stream(names).forEach(key->objects.put(key,bean));
        }
    }


}
