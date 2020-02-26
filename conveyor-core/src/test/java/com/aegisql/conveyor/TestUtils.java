package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TestUtils {

    public static Map<String,String> pairsToMap(String... pairs){
        HashMap<String,String> m = new HashMap<>();
        Objects.requireNonNull(pairs);
        if(pairs.length % 2 == 1) {
            throw new IllegalArgumentException("Number of parameters must be even");
        }
        for(int i = 0; i < pairs.length; i+=2) {
            m.put(pairs[i],pairs[i+1]);
        }

        return m;
    }

}
