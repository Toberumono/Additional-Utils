package toberumono.utils.files.manager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Interface that lays out the required methods for a {@link FileManager}.
 * 
 * @author Toberumono
 */
public interface FileManager extends Closeable {
	/**
	 * A {@link Comparator} for {@link StandardWatchEventKinds} that prioritizes the kinds from greatest to least as follows:
	 * <ol>
	 * <li>{@link StandardWatchEventKinds#ENTRY_DELETE ENTRY_DELETE}</li>
	 * <li>{@link StandardWatchEventKinds#ENTRY_MODIFY ENTRY_MODIFY}</li>
	 * <li>{@link StandardWatchEventKinds#ENTRY_CREATE ENTRY_CREATE}</li>
	 * </ol>
	 */
	public static final Comparator<WatchEvent<?>> DEFAULT_KINDS_COMPARATOR = (a, b) -> { //The StandardWatchEventKinds do not override the equals method
		if (a.kind() == b.kind()) //We only need to check this once
			return 0;
		if (a.kind() == StandardWatchEventKinds.ENTRY_DELETE) //ENTRY_DELETE has first priority, and we know that a.kind() != b.kind(), so a > b
			return 1;
		if (a.kind() == StandardWatchEventKinds.ENTRY_MODIFY) //ENTRY_MODIFY has second priority (after ENTRY_CREATE, but before ENTRY_DELETE)
			return b.kind() == StandardWatchEventKinds.ENTRY_DELETE ? -1 : 1; //ENTRY_DELETE has higher priority than ENTRY_MODIFY, so b > a
		if (a.kind() == StandardWatchEventKinds.ENTRY_CREATE) //ENTRY_CREATE has last priority, and we know that a.kind() != b.kind(), so b > a
			return -1;
		return 0;
	};
	
	/**
	 * An array that holds {@link StandardWatchEventKinds#ENTRY_CREATE}, {@link StandardWatchEventKinds#ENTRY_DELETE}, and
	 * {@link StandardWatchEventKinds#ENTRY_MODIFY}.
	 */
	public static final WatchEvent.Kind<?>[] ALL_STANDARD_KINDS_ARRAY = {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};
	
	/**
	 * A {@link Set} that contains the {@link FileVisitOption#FOLLOW_LINKS} value. For use with
	 * {@link Files#walkFileTree(Path, Set, int, FileVisitor)}
	 */
	public static final Set<FileVisitOption> FOLLOW_LINKS_SET = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
	
	/**
	 * Adds the directory pointed to by the given {@link Path} and all of its subdirectories to the {@link FileManager FileManager's} managed
	 * files.<br>
	 * If the directory specified by {@link Path path} or its contents (including those of subdirectories) are modified while the operation is in
	 * progress and an error occurs, the result is undefined.
	 * 
	 * @param path
	 *            the {@link Path} to watch. This <i>must</i> point to a directory
	 * @throws IOException
	 *             if an I/O error occurs while adding files or folders
	 * @throws NotDirectoryException
	 *             if {@code path} does not point to a directory
	 */
	public void add(Path path) throws IOException;
	
	/**
	 * Removes the directory pointed to by the given {@link Path} and all of its subdirectories from the {@link FileManager FileManager's} managed
	 * files.<br>
	 * If the directory specified by {@link Path path} or its contents (including those of subdirectories) are modified while the operation is in
	 * progress and an error occurs, the result is undefined.
	 * 
	 * @param path
	 *            the {@link Path} to watch. This <i>must</i> point to a directory
	 * @throws IOException
	 *             if an I/O error occurs while removing files or folders
	 * @throws NotDirectoryException
	 *             if {@code path} does not point to a directory
	 */
	public void remove(Path path) throws IOException;
	
	/**
	 * @return an <b>unmodifiable</b> view of the {@link Path Paths} managed by the {@link FileManager}
	 */
	public Collection<Path> getPaths();
}
