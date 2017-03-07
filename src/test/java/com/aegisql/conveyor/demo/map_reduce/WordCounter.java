package com.aegisql.conveyor.demo.map_reduce;

import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;

public class WordCounter implements Supplier<WordCount>, Testing {

	private int count = 0;
	private String word;
	private boolean done = false;
	
	@Override
	public WordCount get() {
		return new WordCount(word,count);
	}

	public static void add(WordCounter counter, String word) {
		if(counter.word == null) {
			counter.word = word;
		}
		counter.count++;
	}

	public static void done(WordCounter counter, String dumb) {
		counter.done = true;
	}

	@Override
	public boolean test() {
		return done ;
	}
	
}
