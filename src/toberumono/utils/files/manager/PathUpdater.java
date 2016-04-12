package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Stack;

import toberumono.utils.functions.IOExceptedBiConsumer;

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
	private final Stack<T> items;
	private final Stack<S> states;
	
	/**
	 * Constructs a new {@link PathUpdater}
	 */
	public PathUpdater() {
		items = new Stack<>();
		states = new Stack<>();
	}
	
	/**
	 * Sets the state of the given {@link Path} to the given {@code state}.
	 * 
	 * @param path
	 *            the {@link Path} whose state is to be updated
	 * @param state
	 *            the new value of the {@link Path Path's} state
	 */
	protected void updateState(T path, S state) {
		items.push(path);
		states.push(state);
	}
	
	/**
	 * Rewinds the changes made by the {@link PathUpdater} by executing {@code action} on each {@link Path} that was visited
	 * by the {@link PathUpdater} in reverse order. (Hence, rewind).
	 * 
	 * @param action
	 *            the action to take on each item that was visited by the {@link PathUpdater}
	 * @throws IOException
	 *             if an I/O error occurs during the rewind
	 */
	protected void rewind(IOExceptedBiConsumer<T, S> action) throws IOException {
		if (items.size() == 0)
			return;
		T item = items.pop();
		for (S state = states.pop(); items.size() > 0; item = items.pop(), state = states.pop())
			action.accept(item, state);
	}
}
