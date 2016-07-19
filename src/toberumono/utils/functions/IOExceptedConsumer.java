package toberumono.utils.functions;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A simple functional interface that represents the equivalent of {@link Consumer} that can throw an {@link IOException}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 */
@FunctionalInterface
public interface IOExceptedConsumer<T> {
	
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t
	 *            the argument
	 * @throws IOException
	 *             if something goes wrong
	 */
	public void accept(T t) throws IOException;
	
	/**
	 * Applies this function to the given argument.<br>
	 * Forwards to {@link #accept(Object)}.
	 *
	 * @param t
	 *            the argument
	 * @throws IOException
	 *             if something goes wrong
	 * @see #accept(Object)
	 */
	@Deprecated
	public default void apply(T t) throws IOException {
		accept(t);
	}
	
	/**
	 * Returns a {@link Consumer} that wraps this {@link IOExceptedConsumer} and, if an {@link IOException} is thrown, either
	 * silently fails or prints its stack trace.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link IOException} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link Consumer} that wraps this {@link IOExceptedConsumer}
	 */
	public default Consumer<T> toBiConsumer(boolean printStackTrace) {
		if (printStackTrace)
			return t -> {
				try {
					accept(t);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			};
		else
			return t -> {
				try {
					accept(t);
				}
				catch (IOException e) {}
			};
	}
}
