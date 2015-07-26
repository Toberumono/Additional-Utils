package toberumono.utils.functions;

import java.io.IOException;

import toberumono.utils.files.TransferFileWalker;

/**
 * A sub-interface of {@link ExceptedBiFunction} specifically for {@link IOException IOExceptions}.<br>
 * Primarily for use with {@link TransferFileWalker}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <U>
 *            the type of the second argument
 * @param <R>
 *            the type of the returned value
 * @see TransferFileWalker
 */
@FunctionalInterface
public interface IOExceptedBiFunction<T, U, R> extends ExceptedBiFunction<T, U, R> {
	
	/**
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	public R apply(T t, U u) throws IOException;
}
