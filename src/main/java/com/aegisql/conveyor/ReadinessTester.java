package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class ReadinessTester<K,L,OUT> implements BiPredicate<State<K,L>, Supplier<? extends OUT>> {

	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> p;
	
	public ReadinessTester() {
		this.p = (s,b) -> true;
	}

	private ReadinessTester(BiPredicate<State<K,L>, Supplier<? extends OUT>> p) {
		this.p = p;
	}

	public ReadinessTester<K,L,OUT> accepted(int times) {
		return new ReadinessTester<K,L,OUT>( p.and( (s,b) -> {
			return s.previouslyAccepted == times;
		} ) );
	}

	public ReadinessTester<K,L,OUT> accepted(L label, L... more) {
		ReadinessTester<K,L,OUT> f = new ReadinessTester<K,L,OUT>( p.and( (s,b) -> {
			return s.eventHistory.containsKey(label);
		} ) );
		if(more != null) {
			for(L l:more) {
				f = f.andThen(new ReadinessTester<K,L,OUT>((s,b) -> {
					return s.eventHistory.containsKey(l);
				} ) );
			}
		}
		return f;
	}

	public ReadinessTester<K,L,OUT> accepted(L label, int times) {
		return new ReadinessTester<K,L,OUT>( p.and( (s,b) -> {
			Integer counter = s.eventHistory.get(label);
			if(counter == null) {
				return false;
			} else {
				return counter == times;
			}
		} ) );
	}
	
	public ReadinessTester<K,L,OUT> andThen(BiPredicate<State<K,L>, Supplier<? extends OUT>> other) {
		return new ReadinessTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && other.test(s, b);
		}  );
	}

	public ReadinessTester<K,L,OUT> andNot(BiPredicate<State<K,L>, Supplier<? extends OUT>> other) {
		return new ReadinessTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && ! other.test(s, b);
		}  );
	}

	public ReadinessTester<K,L,OUT> or(ReadinessTester<K,L,OUT> other) {
		return new ReadinessTester<K,L,OUT>( (s,b)->{
			return this.test(s, b) || other.test(s, b);
		}  );
	}

	public ReadinessTester<K,L,OUT> usingBuilderTest(Class<? extends Supplier<OUT>> cls) {
		ReadinessTester<K,L,OUT> tester;
		if( Testing.class.isAssignableFrom(cls) ) {
			tester = new ReadinessTester<K,L,OUT>(  (s,b)->{
				Testing t = (Testing)b;
				return t.test();
			}  );
		} else if(TestingState.class.isAssignableFrom(cls)) {
			tester = new ReadinessTester<K,L,OUT>(  (s,b)->{
				TestingState<K,L> t = (TestingState<K,L>)b;
				return t.test(s);
			}  );
		} else {
			throw new ClassCastException("Builder is not implementing Testing or TestingState interface");
		}
		
		return new ReadinessTester<K,L,OUT>(  p.and(tester) );
	}

	
	@Override
	public boolean test(State<K, L> s, Supplier<? extends OUT> b) {
		return p.test(s, b);
	}
	
}
