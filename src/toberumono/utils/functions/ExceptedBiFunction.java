package toberumono.utils.functions;

import java.util.function.BiFunction;

/**
 * A simple functional interface that represents the equivalent of {@link java.util.function.BiFunction BiFunction} that can
 * throw an {@link Exception}.
 * 
 * @author Joshua Lipstone
 * @param <T>
 *            the type of the first argument
 * @param <U>
 *            the type of the second argument
 * @param <R>
 *            the type of the returned value
 */
@FunctionalInterface
public interface ExceptedBiFunction<T, U, R> {
	
	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t
	 *            the first argument
	 * @param u
	 *            the second argument
	 * @return the function result
	 * @throws Exception
	 *             if something goes wrong
	 */
	R apply(T t, U u) throws Exception;
	
	/**
	 * Returns a {@link BiFunction} that wraps this {@link ExceptedBiFunction} and returns {@code null} if an
	 * {@link Exception} would have been thrown and optionally prints the stack trace of said {@link Exception}.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link BiFunction} that wraps this {@link ExceptedBiFunction}
	 */
	public default BiFunction<T, U, R> toBiFunction(boolean printStackTrace) {
		if (printStackTrace)
			return (t, u) -> {
				try {
					return this.apply(t, u);
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			};
		else
			return (t, u) -> {
				try {
					return this.apply(t, u);
				}
				catch (Exception e) {
					return null;
				}
			};
	}
}
