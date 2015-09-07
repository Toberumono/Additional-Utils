package toberumono.utils.functions;

import java.util.function.Consumer;

/**
 * A simple functional interface that represents the equivalent of {@link Consumer} that can throw an {@link Exception}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 */
@FunctionalInterface
public interface ExceptedConsumer<T> {
	
	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t
	 *            the first argument
	 * @throws Exception
	 *             if something goes wrong
	 */
	public void apply(T t) throws Exception;
	
	/**
	 * Returns a {@link Consumer} that wraps this {@link ExceptedConsumer} and, if an {@link Exception} is thrown, either
	 * silently fails or prints its stack trace.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link Consumer} that wraps this {@link ExceptedConsumer}
	 */
	public default Consumer<T> toBiConsumer(boolean printStackTrace) {
		if (printStackTrace)
			return t -> {
				try {
					this.apply(t);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			};
		else
			return t -> {
				try {
					this.apply(t);
				}
				catch (Exception e) {}
			};
	}
}
