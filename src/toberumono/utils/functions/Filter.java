package toberumono.utils.functions;

import java.util.function.Function;

/**
 * A {@link Filter} is a {@link Function} that tests whether an item should be accepted for processing.<br>
 * The implied contract for a {@link Filter} is that it returns {@code true} if the item should be accepted and {@code false}
 * if the item should be rejected. However, there is no way to enforce this, so it is up to the programmer to ensure that
 * their implementation satisfies this contract.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the item to test
 */
@FunctionalInterface
public interface Filter<T> extends Function<T, Boolean> {
	
}
