package com.aegisql.conveyor.demo.word_count;

import java.util.function.Supplier;

public class WordCounter implements Supplier<Integer> {

    private int counter = 0;

    @Override
    public Integer get() {
        return counter;
    }

    public void count() {
        counter++;
    }
}
