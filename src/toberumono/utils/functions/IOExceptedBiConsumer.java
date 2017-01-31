package toberumono.utils.functions;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * A simple functional interface that represents the equivalent of {@link BiConsumer} that can throw an {@link IOException}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <U>
 *            the type of the second argument
 */
@FunctionalInterface
public interface IOExceptedBiConsumer<T, U> extends ExceptedBiConsumer<T, U> {
	
	/**
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	public void accept(T t, U u) throws IOException;
	
	/**
	 * Applies this function to the given arguments.<br>
	 * Forwards to {@link #accept(Object, Object)}
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	@Deprecated
	public default void apply(T t, U u) throws IOException {
		accept(t, u);
	}
	
	/**
	 * Returns a {@link BiConsumer} that wraps this {@link IOExceptedBiConsumer} and, if an {@link IOException} is thrown, either
	 * silently fails or prints its stack trace.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link IOException} thrown by this function when called from within the
	 *            wrapper
	 * @return a {@link BiConsumer} that wraps this {@link IOExceptedBiConsumer}
	 */
	@Override
	public default BiConsumer<T, U> toBiConsumer(boolean printStackTrace) {
		if (printStackTrace)
			return (t, u) -> {
				try {
					accept(t, u);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			};
		else
			return (t, u) -> {
				try {
					accept(t, u);
				}
				catch (IOException e) {}
			};
	}
}
