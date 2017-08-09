package com.aegisql.conveyor.persistence.core;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

public class ThreeStageResultConsumer<K, V> implements ResultConsumer<K, V> {

	private final ResultConsumer<K, V> before;
	private final ResultConsumer<K, V> main;
	private final ResultConsumer<K, V> after;

	public ThreeStageResultConsumer(
			ResultConsumer<K, V> before,
			ResultConsumer<K, V> main,
			ResultConsumer<K, V> after
			) {
		this.before = before;
		this.main   = main;
		this.after  = after;
	}

	@Override
	public void accept(ProductBin<K, V> bin) {
		before.accept(bin);
		main.accept(bin);
		after.accept(bin);
	}

	@Override
	public Consumer<ProductBin<K, V>> andThen(Consumer<? super ProductBin<K, V>> c) {
		return new ThreeStageResultConsumer<K, V>(before,main.andThen(new ResultConsumer<K,V>(){
			@Override
			public void accept(ProductBin<K, V> t) {
				main.accept(t);
				c.accept(t);
			}
			
		}),after);
	}

	@Override
	public ResultConsumer<K, V> andThen(ResultConsumer<K, V> other) {
		return new ThreeStageResultConsumer<K,V>(before,main.andThen(other),after);
	}

	@Override
	public ResultConsumer<K, V> filter(Predicate<ProductBin<K, V>> filter) {
		return new ThreeStageResultConsumer<K,V>(before,main.filter(filter),after);
	}

	@Override
	public ResultConsumer<K, V> filterKey(Predicate<K> filter) {
		return new ThreeStageResultConsumer<K,V>(before,main.filterKey(filter),after);
	}

	@Override
	public ResultConsumer<K, V> filterResult(Predicate<V> filter) {
		return new ThreeStageResultConsumer<K,V>(before,main.filterResult(filter),after);
	}

	@Override
	public ResultConsumer<K, V> filterStatus(Predicate<Status> filter) {
		return new ThreeStageResultConsumer<K,V>(before,main.filterStatus(filter),after);
	}

	@Override
	public ResultConsumer<K, V> async(ExecutorService pool) {
		return new ThreeStageResultConsumer<K,V>(before,main.async(pool),after);
	}

	@Override
	public ResultConsumer<K, V> async() {
		return new ThreeStageResultConsumer<K,V>(before,main.async(),after);
	}

}
