/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ReadinessTester.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class ReadinessTester<K,L,OUT> implements BiPredicate<State<K,L>, Supplier<? extends OUT>>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** The p. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> p;
	
	/**
	 * Instantiates a new readiness tester.
	 */
	public ReadinessTester() {
		this.p = (s,b) -> true;
	}

	/**
	 * Instantiates a new readiness tester.
	 *
	 * @param p the p
	 */
	private ReadinessTester(BiPredicate<State<K,L>, Supplier<? extends OUT>> p) {
		this.p = p;
	}

	public  ReadinessTester<K,L,OUT> andThen(Predicate<Supplier<? extends OUT>> pred) {
		return new ReadinessTester<>( this.p.and((s,b)->pred.test(b)) );
	}
	/**
	 * Accepted.
	 *
	 * @param times the times
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> accepted(int times) {
		return new ReadinessTester<K,L,OUT>( p.and( (s,b) -> {
			return s.previouslyAccepted == times;
		} ) );
	}

	/**
	 * Accepted.
	 *
	 * @param label the label
	 * @param more the more
	 * @return the readiness tester
	 */
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

	/**
	 * Accepted.
	 *
	 * @param label the label
	 * @param times the times
	 * @return the readiness tester
	 */
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
	
	/**
	 * And then.
	 *
	 * @param other the other
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> andThen(BiPredicate<State<K,L>, Supplier<? extends OUT>> other) {
		return new ReadinessTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && other.test(s, b);
		}  );
	}

	/**
	 * And not.
	 *
	 * @param other the other
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> andNot(BiPredicate<State<K,L>, Supplier<? extends OUT>> other) {
		return new ReadinessTester<K,L,OUT>(  (s,b)->{
			return this.test(s, b) && ! other.test(s, b);
		}  );
	}

	/**
	 * Or.
	 *
	 * @param other the other
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> or(ReadinessTester<K,L,OUT> other) {
		return new ReadinessTester<K,L,OUT>( (s,b)->{
			return this.test(s, b) || other.test(s, b);
		}  );
	}

	/**
	 * Using builder test.
	 *
	 * @param cls the cls
	 * @return the readiness tester
	 */
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

	/**
	 * Never ready.
	 *
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> neverReady() {
		return new ReadinessTester<K,L,OUT>( (s,b)->{
			return false;
		}  );
	}

	/**
	 * Immediately ready.
	 *
	 * @return the readiness tester
	 */
	public ReadinessTester<K,L,OUT> immediatelyReady() {
		return new ReadinessTester<K,L,OUT>();
	}

	
	/* (non-Javadoc)
	 * @see java.util.function.BiPredicate#test(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean test(State<K, L> s, Supplier<? extends OUT> b) {
		return p.test(s, b);
	}
	
}
