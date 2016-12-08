package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class BuildTester<K,L,OUT> implements BiPredicate<State<K,L>, Supplier<? extends OUT>> {

	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> p;
	
	public BuildTester() {
		this.p = (s,b) -> true;
	}

	private BuildTester(BiPredicate<State<K,L>, Supplier<? extends OUT>> p) {
		this.p = p;
	}

	public BuildTester<K,L,OUT> accepted(int times) {
		return new BuildTester<K,L,OUT>( p.and( (s,b) -> {
			return s.previouslyAccepted == times;
		} ) );
	}

	public BuildTester<K,L,OUT> accepted(L label) {
		return new BuildTester<K,L,OUT>( p.and( (s,b) -> {
			return s.eventHistory.containsKey(label);
		} ) );
	}

	public BuildTester<K,L,OUT> accepted(L label, int times) {
		return new BuildTester<K,L,OUT>( p.and( (s,b) -> {
			Integer counter = s.eventHistory.get(label);
			if(counter == null) {
				return false;
			} else {
				return counter == times;
			}
		} ) );
	}
	
	public BuildTester<K,L,OUT> and(BuildTester<K,L,OUT> other) {
		return new BuildTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && other.test(s, b);
		}  );
	}

	public BuildTester<K,L,OUT> andNot(BuildTester<K,L,OUT> other) {
		return new BuildTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && ! other.test(s, b);
		}  );
	}

	public BuildTester<K,L,OUT> or(BuildTester<K,L,OUT> other) {
		return new BuildTester<K,L,OUT>( (s,b)->{
			return this.test(s, b) || other.test(s, b);
		}  );
	}
	
	@Override
	public boolean test(State<K, L> s, Supplier<? extends OUT> b) {
		return p.test(s, b);
	}
	
}
