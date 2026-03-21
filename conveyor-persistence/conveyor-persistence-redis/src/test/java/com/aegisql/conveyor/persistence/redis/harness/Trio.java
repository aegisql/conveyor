package com.aegisql.conveyor.persistence.redis.harness;

import java.util.Objects;

public class Trio {

    private final String text1;
    private final String text2;
    private final int number;

    public Trio(String text1, String text2, int number) {
        this.text1 = text1;
        this.text2 = text2;
        this.number = number;
    }

    public String getText1() {
        return text1;
    }

    public String getText2() {
        return text2;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "Trio[text1=" + text1 + ", text2=" + text2 + ", number=" + number + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Trio trio)) {
            return false;
        }
        return number == trio.number
                && Objects.equals(text1, trio.text1)
                && Objects.equals(text2, trio.text2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text1, text2, number);
    }
}
