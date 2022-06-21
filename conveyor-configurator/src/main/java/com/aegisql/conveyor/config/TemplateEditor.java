package com.aegisql.conveyor.config;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEditor {

    private final Map<String,String> map = new HashMap<>();
    private final static Pattern PATTERN = Pattern.compile("\\$\\{.*?}");

    private void setEnvVariables() {
        Map<String,String> env = System.getenv();
        map.putAll(env);
        Properties p = System.getProperties();
        p.forEach((key,value)-> map.put(""+key,""+value));
    }

    private String extractDefaultValue(String match) {
        String[] split = match.split(":");
        String defaultVal = split[1];
        if(split.length > 2) {
            for(int i = 2; i < split.length; i++) {
                defaultVal = defaultVal+":"+split[i];
            }
        }
        return defaultVal;
    }

    public TemplateEditor() {
    }

    public String setVariables(String label, String value) {
        map.put(label,value);
        setEnvVariables();
        Objects.requireNonNull(value,"Property with name '"+label+"' is null");
        Matcher m = PATTERN.matcher(value);
        Map<String,String> allMatches = new LinkedHashMap<>();
        while (m.find()) {
            String grouped = m.group();
            String match = grouped.substring(2, grouped.length() - 1);
            if(match.contains(":")){
                String[] split = match.split(":");
                allMatches.put(split[0],match);
            } else {
                allMatches.put(match,null);
            }
        }

        if( ! allMatches.isEmpty()) {
            String result = value;
            for(Map.Entry<String,String> matchEntry : allMatches.entrySet()) {
                String matchKey = matchEntry.getKey();
                String fullMatch = matchEntry.getValue();
                if(! map.containsKey(matchKey) && fullMatch == null) {
                    throw new ConveyorConfigurationException("Expected property with name '"+matchKey+"'");
                }
                String property = map.get(matchKey);
                if(property == null && fullMatch != null) {
                    property = extractDefaultValue(matchEntry.getValue());
                }
                Objects.requireNonNull(property,"Property with name '"+matchKey+"' is null");
                String pattern = fullMatch == null ? "${"+matchKey+"}":"${"+fullMatch+"}";
                result = result.replace(pattern, property);
                map.put(label,result);
            }
            return result;
        } else {
            return value;
        }
    }

    }
