package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;

class ProductSupplierEx<T,K,L> implements ProductSupplier<T>,Expireable, TimeoutAction,Testing, TestingState<K, L>,FutureSupplier<T> {

	private final ProductSupplier<T> ps;
	private final Expireable ex;
	private final TimeoutAction to;
	private final Testing tt;
	private final TestingState<K, L>ts;
	private final FutureSupplier<T> fs;
	
	
	public ProductSupplierEx(
			ProductSupplier<T> ps,
			Expireable ex,
			TimeoutAction to,
			Testing tt,
			TestingState<K, L> ts,
			FutureSupplier<T> fs) {
		super();
		this.ps = ps;
		this.ex = ex;
		this.to = to;
		this.tt = tt;
		this.ts = ts;
		this.fs = fs;
	}
	@Override
	public T get() {
		return ps.get();
	}
	@Override
	public boolean test(State<K, L> t) {
		return ts.test(t);
	}
	@Override
	public boolean test() {
		return tt.test();
	}
	@Override
	public void onTimeout() {
		to.onTimeout();
	}
	@Override
	public long getExpirationTime() {
		return ex.getExpirationTime();
	}
	@Override
	public CompletableFuture<? extends T> getFuture() {
		return fs.getFuture();
	}
}
