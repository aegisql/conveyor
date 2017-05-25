/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.map_reduce;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.demo.ThreadPool;
import com.aegisql.conveyor.loaders.PartLoader;

import org.junit.Test;

public class Demo {
	
	//We will be counting words coming
	//from three independent resources
	ThreadPool pool = new ThreadPool();
	
	//Define labels for two basic operations
	SmartLabel<WordCounter> ADD  = SmartLabel.of("ADD",WordCounter::add);
	//Define label to forward results to the collectingConveyor
	//note that MERGE and ADD labels associated with the same action
	SmartLabel<WordCounter> MERGE  = SmartLabel.of("MERGE",WordCounter::add);
	//We do not need to do anything
	//Just register the label as a trigger for the readiness algorithm
	SmartLabel<WordCounter> DONE = SmartLabel.of("DONE",()->{});
	
	Future<?> countWordsAsynch(AssemblingConveyor<String, SmartLabel<WordCounter>, WordCount> collectingConveyor, String... words ) {
		//Run code in its own thread with some random delay
		return pool.runAsynchWithDelay(System.nanoTime() % 100, 
			()->{
			//Detaching new conveyor from collectingConveyor helps to make further 
			//configuration easier
			AssemblingConveyor<String, SmartLabel<WordCounter>, WordCount> source = collectingConveyor.detach();
			//Giving conveyor a distinctive name is a good practice 
			source.setName("COUNTER_"+Thread.currentThread().getId());
			//Results will be forwrded to collectingConveyor with the MERGE label
			source.forwardResultTo(collectingConveyor,MERGE);
			//Ready when "DONE" command is received
			source.setReadinessEvaluator(Conveyor.getTesterFor(source).accepted(DONE));
			//Extract "common" part of the word loader into a variable
			//This will just improve readability and highlight our intentions.
			PartLoader<String, SmartLabel<WordCounter>,?,?,?> wordLoader = source.part().label(ADD);
			//Stream all words to the conveyor
			Arrays.stream(words).forEach(word->wordLoader.id(word).value(new WordCount(word, 1)).place());
			//Send "DONE" message to all words and wait 
			//until command is delivered to all of them.
			source.part().foreach().label(DONE).place();
			CompletableFuture<Boolean> f = source.completeAndStop();
			//we need to wait to synchronize with the pool's future
			//If pool's future is not required, the next call can be omitted.
			try {
				f.get();
			} catch (Exception e) {
				throw new RuntimeException("Multi-Key DONE message failed in "+source.getName(),e);
			}
		});
	}

	public void runDemo() throws InterruptedException, ExecutionException {
		//Simple container for demo results
		LinkedList<WordCount> wordList = new LinkedList<>();
		//Creating conveyor
		//Each word is a unique key of String type
		//Label is a SmartLabel<WordCounter>
		//For this example we created ADD, MERGE and DONE labels
		//Product is a WordCount - immutable pair of word and number of occurrences
		AssemblingConveyor<String, SmartLabel<WordCounter>, WordCount> collectingConveyor = new AssemblingConveyor<>();
		collectingConveyor.setName("COLLECTOR");
		collectingConveyor.setDefaultBuilderTimeout(Duration.ofSeconds(100));
		collectingConveyor.setBuilderSupplier(WordCounter::new);
		//Ready when "DONE" command is received
		//Note that though MERGE command is associated with the same label
		//It does not result in finishing the word count task
		collectingConveyor.setReadinessEvaluator(Conveyor.getTesterFor(collectingConveyor).accepted(DONE));
		collectingConveyor.resultConsumer().first(bin->{
			//We assume that in this demo 
			//this is the only thread accessing the wordList
			wordList.add(bin.product);
		}).andThen(LogResult.debug(collectingConveyor)).set();

		//Start counting words in three parallel threads
		Future<?> f1 = countWordsAsynch(collectingConveyor,"a","b","c","d","b");
		Future<?> f2 = countWordsAsynch(collectingConveyor,"b","c","d","e","c");
		Future<?> f3 = countWordsAsynch(collectingConveyor,"c","d","e","f","g");
		
		//Code above is running asynchronously
		//But for demo purposes we would like
		//to finally synchronize all sources in order to 
		//see result output of the code.
		f1.get();
		f2.get();
		f3.get();
		collectingConveyor.part().foreach().label(DONE).place();
		CompletableFuture<Boolean> mainFuture = collectingConveyor.completeAndStop();
		mainFuture.get();
		//We expect something like this
		//[a = 1, b = 3, c = 4, d = 3, e = 2, f = 1, g = 1]
		System.out.println(wordList);
		pool.shutdown();
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		new Demo().runDemo();
	}

	@Test
	public void test() throws Exception {
		new Demo().runDemo();
	}
}
