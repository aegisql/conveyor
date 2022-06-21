package com.aegisql.conveyor;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Interface ReadinessPredicate.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
@FunctionalInterface
public interface ReadinessPredicate <K,L,OUT> extends BiPredicate<State<K,L>, Supplier<? extends OUT>>, Serializable {

	/**
	 * And.
	 *
	 * @param other the other
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> and(ReadinessPredicate <K,L,OUT> other) {
		return (a,b) -> this.test(a, b) && other.test(a, b);
	}

	/**
	 * And.
	 *
	 * @param other the other
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> and(Predicate<Supplier<? extends OUT>> other) {
		return (a,b) -> this.test(a, b) && other.test(b);
	}
	
	/**
	 * Or.
	 *
	 * @param other the other
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> or(ReadinessPredicate <K,L,OUT> other) {
		return (a,b) -> this.test(a, b) || other.test(a, b);
	}

	/**
	 * Or.
	 *
	 * @param other the other
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> or(Predicate<Supplier<? extends OUT>> other) {
		return (a,b) -> this.test(a, b) || other.test(b);
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.BiPredicate#negate()
	 */
	default ReadinessPredicate <K,L,OUT> negate() {
		return (a,b) -> ! this.test(a, b);
	}
	
	/**
	 * Accepted.
	 *
	 * @param x the x
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> accepted(int x) {
		return (a,b) -> a.previouslyAccepted == x;
	}

	/**
	 * Accepted.
	 *
	 * @param l the l
	 * @param x the x
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> accepted(L l, int x) {
		return (a,b) -> {
			Integer counter = a.eventHistory.get(l);

			return Objects.requireNonNullElse(counter, 0) == x;
		};
	}

	/**
	 * Accepted.
	 *
	 * @param l the l
	 * @param labels the labels
	 * @return the readiness predicate
	 */
	default ReadinessPredicate <K,L,OUT> accepted(L l, L... labels) {
		ReadinessPredicate <K,L,OUT> res = accepted(l, 1);

		if( labels != null ) {
			for(L label : labels) {
				//TODO: something
			}
		}
		
		return res;
	}
	
	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @return the readiness predicate
	 */
	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor) {
		return (a,b)->true;
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param other the other
	 * @return the readiness predicate
	 */
	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor,Predicate<Supplier<? extends OUT>> other) {
		return (a,b)->other.test(b);
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @param conveyor the conveyor
	 * @param other the other
	 * @return the readiness predicate
	 */
	static <K, L, OUT> ReadinessPredicate <K,L,OUT> of(Conveyor<K, L, OUT> conveyor,ReadinessPredicate <K,L,OUT> other) {
		return other;
	}

}
