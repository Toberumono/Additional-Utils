package toberumono.utils.functions;

import java.util.function.Function;

/**
 * A simple functional interface that represents the equivalent of {@link Function} that can throw an {@link Exception}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <R>
 *            the type of the returned value
 */
@FunctionalInterface
public interface ExceptedFunction<T, R> {
	
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t
	 *            the argument
	 * @return the function result
	 * @throws Exception
	 *             if something goes wrong
	 */
	public R apply(T t) throws Exception;
	
	/**
	 * Returns a {@link Function} that wraps this {@link ExceptedFunction} and returns {@code null} if an {@link Exception}
	 * would have been thrown and optionally prints the stack trace of said {@link Exception}.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link Function} that wraps this {@link ExceptedFunction}
	 */
	public default Function<T, R> toFunction(boolean printStackTrace) {
		if (printStackTrace)
			return t -> {
				try {
					return this.apply(t);
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			};
		else
			return t -> {
				try {
					return this.apply(t);
				}
				catch (Exception e) {
					return null;
				}
			};
	}
}
