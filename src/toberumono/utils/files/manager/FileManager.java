package toberumono.utils.files.manager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface that lays out the required methods for a {@link FileManager}.
 * 
 * @author Toberumono
 */
public interface FileManager extends Closeable {
	/**
	 * A {@link Set} that contains the {@link FileVisitOption#FOLLOW_LINKS} value. For use with
	 * {@link Files#walkFileTree(Path, Set, int, FileVisitor)}
	 */
	public static final Set<FileVisitOption> FOLLOW_LINKS_SET = new HashSet<>(Arrays.asList(new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS}));
	
	/**
	 * Adds the directory pointed to by the given {@link Path} and all of its subdirectories to the {@link FileManager
	 * FileManager's} managed files.<br>
	 * If the directory specified by {@link Path path} or its contents (including those of subdirectories) are modified while
	 * the operation is in progress and an error occurs, the result is undefined.
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
	 * Removes the directory pointed to by the given {@link Path} and all of its subdirectories from the {@link FileManager
	 * FileManager's} managed files.<br>
	 * If the directory specified by {@link Path path} or its contents (including those of subdirectories) are modified while
	 * the operation is in progress and an error occurs, the result is undefined.
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
	public Set<Path> getPaths();
}
