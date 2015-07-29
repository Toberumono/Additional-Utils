package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

import toberumono.utils.functions.Filter;
import toberumono.utils.general.MutedLogger;

/**
 * A simple implementation of {@link LoggedFileWalker} that recursively erases a directory (or file) and all of its contents.
 * <b>Use with care.</b>
 * 
 * @author Toberumono
 */
public class RecursiveEraser extends LoggedFileWalker {
	private final DeletionBound del;
	
	private static class DeletionBound {
		int depth, bound;
		
		DeletionBound() {
			depth = 0;
			bound = 0;
		}
		
		void update() {
			if (depth > bound)
				bound = depth;
		}
	}
	
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
		this(null, null, null, onFailure, log);
	}
	
	/**
	 * Constructs a new {@link RecursiveEraser}.<br>
	 * Both <tt>onFailure</tt> and <tt>log</tt> can be null, in which case their default values will be used.
	 * 
	 * @param fileFilter
	 *            a {@link Filter} for whether a given file should be processed. Default: {@link #DEFAULT_FILTER}
	 * @param directoryFilter
	 *            a {@link Filter} for whether a given directory should be processed. Default: {@link #DEFAULT_FILTER}
	 * @param onSkip
	 *            an action to perform when a file or directory is skipped. Default: {@link #DEFAULT_ON_SKIP_ACTION}
	 * @param onFailure
	 *            the action to take if a file visit fails. Default: {@link #DEFAULT_ON_FAILURE_ACTION}
	 * @param log
	 *            the {@link Logger} to use for logging. Default: {@link MutedLogger#getMutedLogger()}
	 */
	public RecursiveEraser(Filter<Path> fileFilter, Filter<Path> directoryFilter, Consumer<Path> onSkip, BiFunction<Path, IOException, FileVisitResult> onFailure, Logger log) {
		super("Started Deleting", "Deleted", "Finished Deleting", null, null, null, null, log);
		del = new DeletionBound();
		this.onFailureAction = constructOnFailure(onFailure);
		this.onSkipAction = constructOnSkip(onSkip);
	}
	
	private Consumer<Path> constructOnSkip(Consumer<Path> onSkip) {
		final Consumer<Path> os = onSkip == null ? DEFAULT_ON_SKIP_ACTION : onSkip;
		return p -> {
			del.update();
			os.accept(p);
		};
	}
	
	private BiFunction<Path, IOException, FileVisitResult> constructOnFailure(BiFunction<Path, IOException, FileVisitResult> onFailure) {
		final BiFunction<Path, IOException, FileVisitResult> of = onFailure == null ? DEFAULT_ON_FAILURE_ACTION : onFailure;
		return (p, e) -> {
			del.update();
			return of.apply(p, e);
		};
	}
	
	@Override
	public FileVisitResult preVisitDirectoryAction(Path dir, BasicFileAttributes attrs) throws IOException {
		del.depth++;
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileAction(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			Files.deleteIfExists(file);
		}
		catch (Throwable t) {
			if (del.depth > del.bound)
				del.bound = del.depth;
			throw t;
		}
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectoryAction(Path dir, IOException exc) throws IOException {
		if (--del.depth >= del.bound)
			Files.deleteIfExists(dir);
		return FileVisitResult.CONTINUE;
	}
}
