package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import toberumono.utils.general.MutedLogger;

/**
 * Base class for a {@link FileVisitor} that logs the actions it takes (using a {@link Logger}).
 * 
 * @author Toberumono
 */
public abstract class LoggedFileWalker implements FileVisitor<Path> {
	/**
	 * The default action to take in {@link #visitFileFailed(Path, IOException)}. It just returns
	 * {@link FileVisitResult#CONTINUE}.
	 */
	public static final BiFunction<Path, IOException, FileVisitResult> DEFAULT_ON_FAILURE_ACTION = (p, e) -> FileVisitResult.CONTINUE;
	
	protected final String preVisitDirectoryPrefix, fileVisitPrefix, postVisitDirectoryPrefix;
	protected final BiFunction<Path, IOException, FileVisitResult> onFailureAction;
	protected final Logger log;
	
	/**
	 * Constructs a new {@link LoggedFileWalker}. Overriding classes should produce a constructor that takes the
	 * <tt>onFailure</tt> and <tt>log</tt> parameters and passes the prefixes itself rather that requiring a user of that
	 * class to provide them (see {@link RecursiveEraser} and {@link TransferFileWalker} for examples).<br>
	 * The prefixes should not include any spacing characters - ": " will be appended to them automatically.
	 * 
	 * @param preVisitDirectoryPrefix
	 *            the prefix for the log line for {@link #preVisitDirectory(Path, BasicFileAttributes)}
	 * @param fileVisitPrefix
	 *            the prefix for the log line for {@link #visitFile(Path, BasicFileAttributes)}
	 * @param postVisitDirectoryPrefix
	 *            the prefix for the log line for {@link #postVisitDirectory(Path, IOException)}
	 * @param onFailureAction
	 *            the action to take if a file visit fails or an exception is thrown while traversing a directory. Default:
	 *            {@link #DEFAULT_ON_FAILURE_ACTION}
	 * @param log
	 *            the {@link Logger} to use for logging. Default: {@link MutedLogger#getMutedLogger()}
	 * @see Files#copy(Path, Path, java.nio.file.CopyOption...)
	 * @see Files#move(Path, Path, java.nio.file.CopyOption...)
	 */
	public LoggedFileWalker(String preVisitDirectoryPrefix, String fileVisitPrefix, String postVisitDirectoryPrefix, BiFunction<Path, IOException, FileVisitResult> onFailureAction, Logger log) {
		this.preVisitDirectoryPrefix = preVisitDirectoryPrefix + ": ";
		this.fileVisitPrefix = fileVisitPrefix + ": ";
		this.postVisitDirectoryPrefix = postVisitDirectoryPrefix + ": ";
		this.onFailureAction = onFailureAction == null ? DEFAULT_ON_FAILURE_ACTION : onFailureAction;
		this.log = log == null ? MutedLogger.getMutedLogger() : log;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Note:</b> This calls {@link #preVisitDirectoryAction(Path, BasicFileAttributes)}.
	 * 
	 * @see #preVisitDirectoryAction(Path, BasicFileAttributes)
	 */
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		FileVisitResult result = preVisitDirectoryAction(dir, attrs);
		if (Objects.requireNonNull(result) == FileVisitResult.SKIP_SUBTREE)
			log.info("Skipped: " + dir);
		else
			log.info(preVisitDirectoryPrefix + dir);
		return result;
	}
	
	/**
	 * The action to be performed prior to visiting a directory.<br>
	 * The parameters to this method are the same as those passed to
	 * {@link FileVisitor#preVisitDirectory(Object, BasicFileAttributes)}, and are forwarded directly from
	 * {@link #preVisitDirectory(Path, BasicFileAttributes)} without modification.
	 *
	 * @param dir
	 *            a reference to the directory
	 * @param attrs
	 *            the directory's basic attributes
	 * @return the visit result. Returning {@link FileVisitResult#SKIP_SUBTREE} here indicates that the directory was skipped
	 *         (for logging purposes).
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see #preVisitDirectory(Path, BasicFileAttributes)
	 */
	public abstract FileVisitResult preVisitDirectoryAction(Path dir, BasicFileAttributes attrs) throws IOException;
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Note:</b> This calls {@link #visitFileAction(Path, BasicFileAttributes)}.
	 * 
	 * @see #visitFileAction(Path, BasicFileAttributes)
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		FileVisitResult result = visitFileAction(file, attrs);
		if (Objects.requireNonNull(result) == FileVisitResult.SKIP_SUBTREE)
			log.info("Skipped: " + file);
		else
			log.info(fileVisitPrefix + file);
		return result;
	}
	
	/**
	 * The action to be performed when visiting a file.<br>
	 * The parameters to this method are the same as those passed to
	 * {@link FileVisitor#visitFile(Object, BasicFileAttributes)} , and are forwarded directly from
	 * {@link #visitFile(Path, BasicFileAttributes)} without modification.
	 *
	 * @param file
	 *            a reference to the file
	 * @param attrs
	 *            the file's basic attributes
	 * @return the visit result. Returning {@link FileVisitResult#SKIP_SUBTREE} here indicates that the file was skipped (for
	 *         logging purposes).
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see #visitFile(Path, BasicFileAttributes)
	 */
	public abstract FileVisitResult visitFileAction(Path file, BasicFileAttributes attrs) throws IOException;
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Note:</b> This forwards to {@link #onFailure(Path, IOException)}.
	 * 
	 * @see #onFailure(Path, IOException)
	 */
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return onFailure(file, exc);
	}
	
	/**
	 * This method is called when an error occurs when attempting to visit a file or while traversing a directory.
	 * 
	 * @param path
	 *            a reference to the file or directory
	 * @param exc
	 *            the I/O exception that occured
	 * @return the visit result
	 */
	public FileVisitResult onFailure(Path path, IOException exc) {
		log.warning("Failed: " + path.toString());
		log.fine(exc.getLocalizedMessage());
		for (StackTraceElement e : exc.getStackTrace())
			log.finest(e.toString());
		return onFailureAction.apply(path, exc);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Note:</b> This calls {@link #postVisitDirectoryAction(Path, IOException)}.
	 * 
	 * @see #postVisitDirectoryAction(Path, IOException)
	 * @see #onFailure(Path, IOException)
	 */
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		FileVisitResult result = postVisitDirectoryAction(dir, exc);
		if (Objects.requireNonNull(result) == FileVisitResult.SKIP_SUBTREE)
			log.info("Skipped: " + dir);
		else
			log.info(postVisitDirectoryPrefix + dir);
		return result;
	}
	
	/**
	 * The action to be performed after visiting a directory.<br>
	 * The parameters to this method are the same as those passed to
	 * {@link FileVisitor#postVisitDirectory(Object, IOException)}, and are forwarded directly from
	 * {@link #postVisitDirectory(Path, IOException)} without modification.<br>
	 *
	 * @param dir
	 *            a reference to the directory
	 * @param exc
	 *            {@code null} if the iteration of the directory completes without an error; otherwise the I/O exception that
	 *            caused the iteration of the directory to complete prematurely
	 * @return the visit result. Returning {@link FileVisitResult#SKIP_SUBTREE} here indicates that the directory was skipped
	 *         (for logging purposes).
	 * @throws IOException
	 *             if an I/O error occurs
	 * @see #postVisitDirectory(Path, IOException)
	 */
	public abstract FileVisitResult postVisitDirectoryAction(Path dir, IOException exc) throws IOException;
}
