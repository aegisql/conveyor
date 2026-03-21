package com.aegisql.conveyor.persistence.redis.harness;

import java.io.Serializable;
import java.util.function.Supplier;

public class TrioBuilder implements Supplier<Trio>, Serializable {

    private String text1;
    private String text2;
    private int number;

    @Override
    public Trio get() {
        return new Trio(text1, text2, number);
    }

    public static void setText1(TrioBuilder builder, String value) {
        builder.text1 = value;
    }

    public static void setText2(TrioBuilder builder, String value) {
        builder.text2 = value;
    }

    public static void setNumber(TrioBuilder builder, Integer value) {
        builder.number = value;
    }
}
