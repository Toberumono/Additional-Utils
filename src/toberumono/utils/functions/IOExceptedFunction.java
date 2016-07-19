package toberumono.utils.functions;

import java.io.IOException;

/**
 * A sub-interface of {@link ExceptedFunction} specifically for {@link IOException IOExceptions}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <R>
 *            the type of the returned value
 */
@FunctionalInterface
public interface IOExceptedFunction<T, R> extends ExceptedFunction<T, R> {
	
	/**
	 * Applies this function to the given argument.
	 * 
	 * @param t
	 *            the argument
	 * @return the function result
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	public R apply(T t) throws IOException;
}
