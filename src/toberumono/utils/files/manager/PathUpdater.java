package toberumono.utils.files.manager;

import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;

/**
 * An abstract extension of {@link FileVisitor} that implements the basic functions to keep track of changes made while
 * walking through a directory tree.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of {@link Path} to use
 * @param <S>
 *            the type to use to store the state for each {@link Path}
 */
public abstract class PathUpdater<T extends Path, S> extends SimpleFileVisitor<T> {
	private final Stack<T> updated;
	private final Map<T, S> states;
	
	/**
	 * Constructs a new {@link PathUpdater}
	 */
	public PathUpdater() {
		updated = new Stack<>();
		states = new HashMap<>();
	}
	
	/**
	 * Sets the state of the given {@link Path} to the given {@code state}.
	 * 
	 * @param path
	 *            the {@link Path} whose state is to be updated
	 * @param state
	 *            the new value of the {@link Path Path's} state
	 */
	protected void setState(T path, S state) {
		if (!states.containsKey(path))
			updated.push(path);
		states.put(path, state);
	}
	
	/**
	 * Rewinds the changes made by the {@link PathUpdater} by executing {@code action} on each {@link Path} that was visited
	 * by the {@link PathUpdater} in reverse order. (Hence, rewind).
	 * 
	 * @param action
	 *            the action to take on each item that was visited by the {@link PathUpdater}
	 */
	protected void rewind(BiConsumer<T, S> action) {
		if (updated.size() == 0)
			return;
		for (T item = updated.pop(); updated.size() > 0; item = updated.pop())
			action.accept(item, states.remove(item));
	}
}
