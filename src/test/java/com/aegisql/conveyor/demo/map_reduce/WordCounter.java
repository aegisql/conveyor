/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.map_reduce;

import java.util.function.Supplier;

public class WordCounter implements Supplier<WordCount> {

	private int count = 0;
	private String word;
	
	@Override
	public WordCount get() {
		return new WordCount(word,count);
	}

	public static void add(WordCounter counter, WordCount word) {
		counter.word = word.getWord();
		counter.count += word.getCount();
	}

}
