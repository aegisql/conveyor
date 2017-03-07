package com.aegisql.conveyor.demo.map_reduce;

public final class WordCount {
	private final String word;
	private final int count;

	public WordCount(String word, int count) {
		this.word = word;
		this.count = count;
	}

	public String getWord() {
		return word;
	}

	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return word + " = " + count;
	}
	
}
