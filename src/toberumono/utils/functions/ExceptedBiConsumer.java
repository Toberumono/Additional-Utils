package toberumono.utils.functions;

import java.util.function.BiConsumer;

/**
 * A simple functional interface that represents the equivalent of {@link BiConsumer} that can throw an {@link Exception}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <U>
 *            the type of the second argument
 */
@FunctionalInterface
public interface ExceptedBiConsumer<T, U> {
	
	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t
	 *            the first argument
	 * @param u
	 *            the second argument
	 * @throws Exception
	 *             if something goes wrong
	 */
	public void apply(T t, U u) throws Exception;
	
	/**
	 * Returns a {@link BiConsumer} that wraps this {@link ExceptedBiConsumer} and, if an {@link Exception} is thrown, either
	 * silently fails or prints its stack trace.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link BiConsumer} that wraps this {@link ExceptedBiConsumer}
	 */
	public default BiConsumer<T, U> toBiConsumer(boolean printStackTrace) {
		if (printStackTrace)
			return (t, u) -> {
				try {
					this.apply(t, u);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			};
		else
			return (t, u) -> {
				try {
					this.apply(t, u);
				}
				catch (Exception e) {}
			};
	}
}
