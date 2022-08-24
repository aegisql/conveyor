package com.aegisql.conveyor.demo.word_count;

public class CountedWord implements Comparable<CountedWord> {
    private final String word;
    private final int count;

    public CountedWord(String word, int count) {
        this.word = word;
        this.count = count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("\n");
        sb.append("'").append(word).append('\'');
        sb.append("=").append(count);
        return sb.toString();
    }

    @Override
    public int compareTo(CountedWord o) {
        return Integer.compare(o.count, this.count);
    }
}
