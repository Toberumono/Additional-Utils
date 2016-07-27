package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import toberumono.utils.functions.IOExceptedConsumer;
import toberumono.utils.functions.IOExceptedPredicate;

/**
 * An extension of {@link FileVisitor} that provides functions to keep track of and roll back changes made while walking through a directory tree.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of {@link Path} to use
 * @param <S>
 *            the type of action to take as part of the rewind step
 */
public abstract class PathUpdater<T extends Path, S extends IOExceptedConsumer<T>> extends SimpleFileVisitor<T> {
	/**
	 * The {@link Comparator} used for sorting {@link Path Paths} for use in {@link #applyToCollection(Collection, IOExceptedPredicate)}
	 */
	public static final Comparator<Path> PATH_COLLECTION_SORTER = (a, b) -> Integer.valueOf(b.getNameCount()).compareTo(Integer.valueOf(a.getNameCount()));
	
	private final Stack<T> items;
	private final Stack<S> actions;
	
	/**
	 * Constructs a new {@link PathUpdater}
	 */
	public PathUpdater() {
		this(new Stack<>(), new Stack<>());
	}
	
	protected PathUpdater(Stack<T> items, Stack<S> actions) {
		this.items = items;
		this.actions = actions;
	}
	
	/**
	 * Adds a step that must be taken to rewind the changes made by the {@link PathUpdater}
	 * 
	 * @param path
	 *            the {@link Path} upon which the undo action should be performed
	 * @param action
	 *            the action to take to undo the step
	 */
	public void recordStep(T path, S action) {
		items.push(path);
		actions.push(action);
	}
	
	/**
	 * Rewinds the changes made by the {@link PathUpdater} by executing {@code action} on each {@link Path} that was visited by the
	 * {@link PathUpdater} in reverse order. (Hence, rewind).
	 * 
	 * @throws IOException
	 *             if an I/O error occurs during the rewind
	 */
	public void rewind() throws IOException {
		while (items.size() > 0)
			actions.pop().accept(items.pop());
	}
	
	/**
	 * Processes the given potentially unsorted {@link Collection} using the given {@link PathUpdater}. The {@link Collection} is sorted via the given
	 * {@link Comparator} before being processed.
	 * 
	 * @param paths
	 *            a potentially unsorted {@link Collection} of {@link Path Paths}
	 * @param comparator
	 *            a {@link Comparator} that results in the {@link Collection} being sorted such that the {@link Path Paths} closest to the root come
	 *            <i>last</i>
	 * @param isDirectory
	 *            an {@link IOExceptedPredicate} that returns {@code true} iff the {@link Path} points to or pointed to a directory
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void applyToCollection(Collection<T> paths, Comparator<? super T> comparator, IOExceptedPredicate<T> isDirectory) throws IOException {
		paths = paths.stream().sorted(comparator).collect(Collectors.toList());
		Set<T> directories = new LinkedHashSet<>();
		T path;
		for (Iterator<T> iter = paths.iterator(); iter.hasNext();) {
			path = iter.next();
			if (isDirectory.test(path)) { //If it is a directory
				iter.remove();
				directories.add(path);
				preVisitDirectory(path, null);
			}
		}
		for (T p : paths) //All of the Paths remaining in the Collection are files
			visitFile(p, null);
		for (T p : directories)
			postVisitDirectory(p, null);
	}
	
	/**
	 * Processes the given unsorted {@link Collection} using the given {@link PathUpdater} and {@link #PATH_COLLECTION_SORTER} to sort the {@link Path
	 * Paths}.
	 * 
	 * @param paths
	 *            a potentially unsorted {@link Collection} of {@link Path Paths}
	 * @param isDirectory
	 *            an {@link IOExceptedPredicate} that returns {@code true} iff the {@link Path} points or pointed to a directory
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void applyToCollection(Collection<T> paths, IOExceptedPredicate<T> isDirectory) throws IOException {
		applyToCollection(paths, PATH_COLLECTION_SORTER, isDirectory);
	}
}
