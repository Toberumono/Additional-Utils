package toberumono.utils.functions;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * A simple functional interface that represents the equivalent of {@link BiPredicate} that can throw an {@link Exception}.<br>
 * {@link #and(ExceptedBiPredicate)}, {@link #negate()}, and {@link #or(ExceptedBiPredicate)} (including their documentation) are all from
 * {@link BiPredicate} in the Java API.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument to the predicate
 * @param <U>
 *            the type of the second argument the predicate
 */
@FunctionalInterface
public interface ExceptedBiPredicate<T, U> {
	
	/**
	 * Evaluates this predicate on the given arguments.
	 *
	 * @param t
	 *            the first input argument
	 * @param u
	 *            the second input argument
	 * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
	 * @throws Exception
	 *             if something goes wrong
	 */
	public boolean test(T t, U u) throws Exception;
	
	/**
	 * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another. When evaluating the composed
	 * predicate, if this predicate is {@code false}, then the {@code other} predicate is not evaluated.
	 * <p>
	 * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if evaluation of this predicate throws an exception, the
	 * {@code other} predicate will not be evaluated.
	 *
	 * @param other
	 *            a predicate that will be logically-ANDed with this predicate
	 * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other} predicate
	 * @throws NullPointerException
	 *             if other is {@code null}
	 */
	public default ExceptedBiPredicate<T, U> and(ExceptedBiPredicate<? super T, ? super U> other) {
		Objects.requireNonNull(other);
		return (T t, U u) -> test(t, u) && other.test(t, u);
	}
	
	/**
	 * Returns a predicate that represents the logical negation of this predicate.
	 *
	 * @return a predicate that represents the logical negation of this predicate
	 */
	public default ExceptedBiPredicate<T, U> negate() {
		return (T t, U u) -> !test(t, u);
	}
	
	/**
	 * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another. When evaluating the composed
	 * predicate, if this predicate is {@code true}, then the {@code other} predicate is not evaluated.
	 * <p>
	 * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if evaluation of this predicate throws an exception, the
	 * {@code other} predicate will not be evaluated.
	 *
	 * @param other
	 *            a predicate that will be logically-ORed with this predicate
	 * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other} predicate
	 * @throws NullPointerException
	 *             if other is {@code null}
	 */
	public default ExceptedBiPredicate<T, U> or(ExceptedBiPredicate<? super T, ? super U> other) {
		Objects.requireNonNull(other);
		return (T t, U u) -> test(t, u) || other.test(t, u);
	}
	
	/**
	 * @return a {@link BiPredicate} that wraps any thrown {@link Exception Exceptions} in a {@link RuntimeException}
	 */
	public default BiPredicate<T, U> toWrappingBiPredicate() {
		return (t, u) -> {
			try {
				return this.test(t, u);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	/**
	 * Returns a {@link BiPredicate} that wraps this {@link ExceptedBiPredicate} and returns {@code false} if an {@link Exception} would have been
	 * thrown and optionally prints the stack trace of said {@link Exception}.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the wrapper
	 * @return a {@link BiPredicate} that wraps this {@link ExceptedBiPredicate}
	 * @see #toBiPredicate(boolean, boolean)
	 */
	public default BiPredicate<T, U> toBiPredicate(boolean printStackTrace) {
		return toBiPredicate(printStackTrace, false);
	}
	
	/**
	 * Returns a {@link BiPredicate} that wraps this {@link ExceptedBiPredicate} and returns {@code exceptionReturn} if an {@link Exception} would
	 * have been thrown and optionally prints the stack trace of said {@link Exception}.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the wrapper
	 * @param exceptionReturn
	 *            the value to return when an {@link Exception} is thrown
	 * @return a {@link BiPredicate} that wraps this {@link ExceptedBiPredicate}
	 * @see #toBiPredicate(boolean)
	 */
	public default BiPredicate<T, U> toBiPredicate(boolean printStackTrace, boolean exceptionReturn) {
		if (printStackTrace)
			return (t, u) -> {
				try {
					return this.test(t, u);
				}
				catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			};
		else
			return (t, u) -> {
				try {
					return this.test(t, u);
				}
				catch (Exception e) {
					return false;
				}
			};
	}
}
