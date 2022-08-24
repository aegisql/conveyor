/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.word_count;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.demo.ThreadPool;
import com.aegisql.conveyor.demo.smart_conveyor_labels.Person;
import com.aegisql.conveyor.demo.smart_conveyor_labels.PersonBuilder;
import com.aegisql.conveyor.demo.smart_conveyor_labels.PersonBuilderLabel;
import com.aegisql.conveyor.loaders.PartLoader;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static com.aegisql.conveyor.demo.smart_conveyor_labels.PersonBuilderLabel.*;

public class Demo {

	// I Create static labels
	static SmartLabel<WordCounter> WORD = SmartLabel.of("WORD",WordCounter::count);
	static SmartLabel<WordCounter> DONE = SmartLabel.bare("DONE");

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException {

		if(args == null || args.length == 0) {
			System.err.println("Usage: Demo path_to_text_file");
			return;
		}

		// we'll put results here
		Queue<CountedWord> words = new ConcurrentLinkedQueue<>();

		// II - Create conveyor
		Conveyor<String, SmartLabel<WordCounter>, Integer> conveyor = new AssemblingConveyor<>();
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(WordCounter::new);
		
		// IV - Tell it where to put the Product
		conveyor.resultConsumer(new ResultConsumer<String, Integer>() {
			@Override
			public void accept(ProductBin<String, Integer> bin) {
				words.add(new CountedWord(bin.key,bin.product));
			}
		}).set();

		// V - Ready when the DONE label is received
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(DONE));

		// loader for words
		PartLoader<String, SmartLabel<WordCounter>> wordLoader = conveyor.part().label(WORD);
		// loader to complete all counts
		PartLoader<String, SmartLabel<WordCounter>> doneLoader = conveyor.part().foreach().label(DONE);

		// read a file word by word, count only text
		File file = FileUtils.getFile(args[0]);
		Scanner input = new Scanner(file);
		while (input.hasNext()) {
			String word = input.next();
			String cleanWord = word.toLowerCase().replaceAll("[^a-z_-]", "");
			if( ! cleanWord.isEmpty()) {
				wordLoader.id(cleanWord).place();
			}
		}
		// tell all input is done
		CompletableFuture<Boolean> donePlaced = doneLoader.place();
		// wait until it processed
		donePlaced.join();

		// print result with most frequently found words first
		List<CountedWord> countedWords = new ArrayList<>(words);
		Collections.sort(countedWords);
		System.out.println("Found "+words.size()+" words "+ countedWords);

		conveyor.stop();
	}

	@Test
	public void test() throws Exception {
		// Count words in the Apache License
		main(new String[]{"src/test/resources/count.me"});
	}
}
