package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import toberumono.utils.general.MutedLogger;

public class RecursiveEraser extends LoggedFileWalker {
	/**
	 * The default action to take in {@link #visitFileFailed(Path, IOException)}. It just returns
	 * {@link FileVisitResult#TERMINATE}.
	 */
	public static final BiFunction<Path, IOException, FileVisitResult> DEFAULT_ON_FAILURE_ACTION = (p, e) -> FileVisitResult.TERMINATE;
	
	/**
	 * Constructs a new {@link RecursiveEraser} with default values for its onFailure action and log.
	 */
	public RecursiveEraser() {
		this(null, null);
	}
	
	/**
	 * Constructs a new {@link RecursiveEraser}.<br>
	 * Both <tt>onFailure</tt> and <tt>log</tt> can be null, in which case their default values will be used.
	 * 
	 * @param onFailure
	 *            the action to take if a file visit fails. Default: {@link #DEFAULT_ON_FAILURE_ACTION}
	 * @param log
	 *            the {@link Logger} to use for logging. Default: {@link MutedLogger#getMutedLogger()}
	 */
	public RecursiveEraser(BiFunction<Path, IOException, FileVisitResult> onFailure, Logger log) {
		super("Started Deleting", "Deleted", "Finished Deleting", onFailure == null ? DEFAULT_ON_FAILURE_ACTION : onFailure, log);
	}
	
	@Override
	public FileVisitResult preVisitDirectoryAction(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileAction(Path file, BasicFileAttributes attrs) throws IOException {
		Files.deleteIfExists(file);
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectoryAction(Path dir, IOException exc) throws IOException {
		Files.deleteIfExists(dir);
		return FileVisitResult.CONTINUE;
	}
}
