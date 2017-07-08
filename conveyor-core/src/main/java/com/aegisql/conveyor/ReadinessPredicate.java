package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface ReadinessPredicate <K,L,OUT> extends BiPredicate<State<K,L>, Supplier<? extends OUT>> {

	default ReadinessPredicate <K,L,OUT> and(ReadinessPredicate <K,L,OUT> other) {
		return (a,b) -> this.test(a, b) && other.test(a, b);
	}

	default ReadinessPredicate <K,L,OUT> and(Predicate<Supplier<? extends OUT>> other) {
		return (a,b) -> this.test(a, b) && other.test(b);
	}
	
	default ReadinessPredicate <K,L,OUT> or(ReadinessPredicate <K,L,OUT> other) {
		return (a,b) -> this.test(a, b) || other.test(a, b);
	}

	default ReadinessPredicate <K,L,OUT> or(Predicate<Supplier<? extends OUT>> other) {
		return (a,b) -> this.test(a, b) || other.test(b);
	}
	
	default ReadinessPredicate <K,L,OUT> negate() {
		return (a,b) -> ! this.test(a, b);
	}
	
	default ReadinessPredicate <K,L,OUT> accepted(int x) {
		return (a,b) -> a.previouslyAccepted == x;
	}

	default ReadinessPredicate <K,L,OUT> accepted(L l, int x) {
		return (a,b) -> {
			Integer counter = a.eventHistory.get(l);
			
			if( counter == null ) {
				if(x==0) {
					return true;
				} else {
					return false;
				}
			} else {
				return counter.intValue() == x;
			}
		};
	}

	default ReadinessPredicate <K,L,OUT> accepted(L l, L... labels) {
		ReadinessPredicate <K,L,OUT> res = accepted(l, 1);

		if( labels != null ) {
			for(L label : labels) {
				//TODO: something
			}
		}
		
		return res;
	}
	
	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor) {
		return (a,b)->true;
	}

	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor,Predicate<Supplier<? extends OUT>> other) {
		return (a,b)->other.test(b);
	}

	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor,ReadinessPredicate <K,L,OUT> other) {
		return (a,b)->other.test(a,b);
	}

}
