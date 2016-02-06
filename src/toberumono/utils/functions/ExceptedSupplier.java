package toberumono.utils.functions;

import java.util.function.Supplier;

/**
 * A simple functional interface that represents the equivalent of {@link Supplier} that can throw an {@link Exception}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of results supplied by this supplier
 */
@FunctionalInterface
public interface ExceptedSupplier<T> {
	
	/**
	 * Gets a result.
	 *
	 * @return a result
	 * @throws Exception
	 *             if something goes wrong
	 */
	public T get() throws Exception;
	
	/**
	 * Returns a {@link Supplier} that wraps this {@link ExceptedSupplier} and returns {@code null} if an {@link Exception}
	 * would have been thrown and optionally prints the stack trace of said {@link Exception}.
	 * 
	 * @param printStackTrace
	 *            whether to print the stack trace of {@link Exception} thrown by this {@link ExceptedSupplier} when called from within the
	 *            wrapper
	 * @return a {@link Supplier} that wraps this {@link ExceptedSupplier}
	 */
	public default Supplier<T> toSupplier(boolean printStackTrace) {
		if (printStackTrace)
			return () -> {
				try {
					return this.get();
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			};
		else
			return () -> {
				try {
					return this.get();
				}
				catch (Exception e) {
					return null;
				}
			};
	}
}
