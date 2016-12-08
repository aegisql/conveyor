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

	public BuildTester<K,L,OUT> accepted(L label, L... more) {
		BuildTester<K,L,OUT> f = new BuildTester<K,L,OUT>( p.and( (s,b) -> {
			return s.eventHistory.containsKey(label);
		} ) );
		if(more != null) {
			for(L l:more) {
				f = f.and(new BuildTester<K,L,OUT>((s,b) -> {
					return s.eventHistory.containsKey(l);
				} ) );
			}
		}
		return f;
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

	public BuildTester<K,L,OUT> usingBuilderTest(Class<? extends Supplier<OUT>> cls) {
		BuildTester<K,L,OUT> tester;
		if( Testing.class.isAssignableFrom(cls) ) {
			tester = new BuildTester<K,L,OUT>(  (s,b)->{
				Testing t = (Testing)b;
				return t.test();
			}  );
		} else if(TestingState.class.isAssignableFrom(cls)) {
			tester = new BuildTester<K,L,OUT>(  (s,b)->{
				TestingState<K,L> t = (TestingState<K,L>)b;
				return t.test(s);
			}  );
		} else {
			throw new RuntimeException("Builder is not implementing Testing or TestingState interface");
		}
		
		return new BuildTester<K,L,OUT>(  p.and(tester) );
	}

	
	@Override
	public boolean test(State<K, L> s, Supplier<? extends OUT> b) {
		return p.test(s, b);
	}
	
}
