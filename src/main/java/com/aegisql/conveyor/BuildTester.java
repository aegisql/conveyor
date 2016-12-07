package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class BuildTester<K,L,OUT>  {

	BiPredicate<State<K,L>, Supplier<? extends OUT>> p = (s,b) -> true;
	
	public BuildTester() {
		
	}
	
	public BiPredicate<State<K,L>, Supplier<? extends OUT>> get() {
		return p;
	}
	
}
