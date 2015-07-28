package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import toberumono.utils.functions.Filter;
import toberumono.utils.functions.IOExceptedBiFunction;
import toberumono.utils.general.MutedLogger;

/**
 * A {@link FileVisitor} that is designed to duplicate the directory structure it encounters into a target directory and
 * copy, move, or link (or anything that takes two {@link Path Paths} and returns a {@link Path} while potentially throwing
 * an {@link IOException}, really) the files within the source tree to the target tree.
 * 
 * @author Toberumono
 * @see Files#copy(Path, Path, CopyOption...)
 * @see Files#move(Path, Path, CopyOption...)
 * @see Files#createLink(Path, Path)
 * @see Files#createSymbolicLink(Path, Path, FileAttribute...)
 */
public class TransferFileWalker extends LoggedFileWalker {
	/**
	 * A convenience interface - this just makes a few method names a <i>lot</i> shorter.<br>
	 * This indicates that the passed function will be called on files encountered by the corresponding
	 * {@link TransferFileWalker}.
	 * 
	 * @author Toberumono
	 * @see TransferFileWalker#TransferFileWalker(Path, TransferAction)
	 * @see TransferFileWalker#TransferFileWalker(Path, TransferAction, Filter, BiFunction, Logger)
	 * @see TransferFileWalker#TransferFileWalker(Path, TransferAction, Filter, Filter, BiFunction, Logger)
	 */
	@FunctionalInterface
	public static interface TransferAction extends IOExceptedBiFunction<Path, Path, Path> {}
	
	/**
	 * The default {@link Filter} for {@link TransferFileWalker TransferFileWalkers}. It accepts every file (it just returns
	 * {@code true}).
	 */
	public static final Filter<Path> DEFAULT_FILTER = p -> true;
	
	private final Path target;
	private Path source; //This is determined when it starts walking, so we cannot make it final.
	private int depth;
	private final Filter<Path> fileFilter, directoryFilter;
	private final TransferAction action;
	
	/**
	 * Construct a new {@link TransferFileWalker} with the given <tt>target</tt> and <tt>action</tt>.<br>
	 * This will use the {@link #DEFAULT_FILTER} for filtering items, {@link #DEFAULT_ON_FAILURE_ACTION} action when a file
	 * visit fails and log to an anonymous logger.
	 * 
	 * @param target
	 *            a {@link Path} to the root directory into which the files should be transferred (this need not exist before
	 *            calling {@link Files#walkFileTree(Path, FileVisitor)}.
	 * @param action
	 *            the action to use to transfer each file into the new directory tree. Generally, using {@code Files::copy}
	 *            or {@code Files::move} is sufficient.
	 * @see Files#copy(Path, Path, CopyOption...)
	 * @see Files#move(Path, Path, CopyOption...)
	 * @see Files#createLink(Path, Path)
	 * @see Files#createSymbolicLink(Path, Path, FileAttribute...)
	 */
	public TransferFileWalker(Path target, TransferAction action) {
		this(target, action, null, null, null);
	}
	
	/**
	 * Constructs a new {@link TransferFileWalker} with the same filter criteria for files and directories. All arguments to
	 * this method except for <tt>target</tt> and <tt>action</tt> can be null, in which case their default values will be
	 * used.
	 * 
	 * @param target
	 *            a {@link Path} to the root directory into which the files should be transferred (this need not exist before
	 *            starting the transfer).
	 * @param action
	 *            the action to use to transfer each file into the new directory tree. Generally, using {@code Files::copy}
	 *            or {@code Files::move} is sufficient.
	 * @param filter
	 *            a {@link Filter} for whether a given file or directory should be transferred. Default:
	 *            {@link #DEFAULT_FILTER}
	 * @param onFailure
	 *            the action to take if a file visit fails. Default: {@link #DEFAULT_ON_FAILURE_ACTION}
	 * @param log
	 *            the {@link Logger} to use for logging. Default: {@link MutedLogger#getMutedLogger()}
	 * @see Files#copy(Path, Path, CopyOption...)
	 * @see Files#move(Path, Path, CopyOption...)
	 * @see Files#createLink(Path, Path)
	 * @see Files#createSymbolicLink(Path, Path, FileAttribute...)
	 */
	public TransferFileWalker(Path target, TransferAction action, Filter<Path> filter, BiFunction<Path, IOException, FileVisitResult> onFailure, Logger log) {
		this(target, action, filter, filter, onFailure, log);
	}
	
	/**
	 * Constructs a new {@link TransferFileWalker} with the same filter criteria for files and directories. All arguments to
	 * this method except for <tt>target</tt> and <tt>action</tt> can be null, in which case their default values will be
	 * used.
	 * 
	 * @param target
	 *            a {@link Path} to the root directory into which the files should be transferred (this need not exist before
	 *            starting the transfer).
	 * @param action
	 *            the action to use to transfer each file into the new directory tree. Generally, using {@code Files::copy}
	 *            or {@code Files::move} is sufficient.
	 * @param fileFilter
	 *            a {@link Filter} for whether a given file should be processed. Default: {@link #DEFAULT_FILTER}
	 * @param directoryFilter
	 *            a {@link Filter} for whether a given directory should be processed. Default: {@link #DEFAULT_FILTER}
	 * @param onFailure
	 *            the action to take if a file visit fails. Default: {@link #DEFAULT_ON_FAILURE_ACTION}
	 * @param log
	 *            the {@link Logger} to use for logging. Default: {@link MutedLogger#getMutedLogger()}
	 * @see Files#copy(Path, Path, CopyOption...)
	 * @see Files#move(Path, Path, CopyOption...)
	 * @see Files#createLink(Path, Path)
	 * @see Files#createSymbolicLink(Path, Path, FileAttribute...)
	 */
	public TransferFileWalker(Path target, TransferAction action, Filter<Path> fileFilter, Filter<Path> directoryFilter,
			BiFunction<Path, IOException, FileVisitResult> onFailure, Logger log) {
		super("Started", "Transferred", "Finished", onFailure, log);
		if (target == null)
			throw new NullPointerException("Cannot have a null target.");
		this.target = target;
		if (action == null)
			throw new NullPointerException("Cannot have a null action.");
		this.action = action;
		this.fileFilter = fileFilter == null ? DEFAULT_FILTER : fileFilter;
		this.directoryFilter = directoryFilter == null ? DEFAULT_FILTER : directoryFilter;
		this.depth = 0;
	}
	
	@Override
	public FileVisitResult preVisitDirectoryAction(Path dir, BasicFileAttributes attrs) throws IOException {
		if (!directoryFilter.apply(dir))
			return FileVisitResult.SKIP_SUBTREE;
		if (depth++ == 0) {
			source = dir;
			log.info("Started transfer: " + source + " -> " + target);
		}
		Files.createDirectories(target.resolve(source.relativize(dir)));
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileAction(Path file, BasicFileAttributes attrs) throws IOException {
		if (!fileFilter.apply(file))
			return FileVisitResult.SKIP_SUBTREE;
		action.apply(file, target.resolve(source.relativize(file)));
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectoryAction(Path dir, IOException exc) throws IOException {
		if (exc != null)
			onFailure(dir, exc);
		if (--depth == 0) {
			if (exc != null)
				log.info("Completed transfer: " + source + " -> " + target);
			else
				log.warning("Failed transfer: " + source + " -> " + target);
			source = null;
		}
		return FileVisitResult.CONTINUE;
	}
	
}
