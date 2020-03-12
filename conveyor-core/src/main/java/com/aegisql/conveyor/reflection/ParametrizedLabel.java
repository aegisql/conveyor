package com.aegisql.conveyor.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ParametrizedLabel {

    private final String wholeLabel;
    private final String label;
    private Object[] properties;
    private Class<?>[] classes;
    private String tmpl;
    private final List<Integer> builderPositions = new ArrayList<>();
    private final List<Integer> valuePositions = new ArrayList<>();

    public ParametrizedLabel(Class<?> bClass, String label) {
        this(bClass,null,label);
    }

    public ParametrizedLabel(Class<?> bClass, Class<?> vClass, String label) {
        Objects.requireNonNull(label,"Label must not be null");
        this.wholeLabel = label.trim();
        if(isParametrized()) {
            String[] parts = label.split("\\{|\\}",3);
            this.label = parts[0];
            this.properties = parts[1].split(",");
        } else {
            this.label = label;
        }
    }


    public boolean isParametrized() {
        return wholeLabel.endsWith("}") && wholeLabel.contains("{");
    }

    public String getLabel() {
        return label;
    }

    public Object[] getProperties() {
        return properties;
    }

    public Object[] getProperties(Object builder, Object value) {
        return properties;
    }

    public String getTemplate() {
        if(tmpl != null) {
            return tmpl;
        }
        StringBuilder sb = new StringBuilder(label);
        if(properties != null) {
            String template = Arrays.stream(properties).map(p -> "?").collect(Collectors.joining(",","{","}"));
            sb.append(template);
        }
        tmpl = sb.toString();
        return tmpl;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ParametrizedLabel{");
        sb.append(wholeLabel);
        sb.append('}');
        return sb.toString();
    }
}
