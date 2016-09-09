package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.Scanner;

import toberumono.utils.functions.IOExceptedPredicate;

/**
 * A trivial implementation of {@link AbstractFileManager} that does nothing. This is useful for implementations that only
 * want to override a subset of the triggered methods (the six methods with names that start with on).<br>
 * Note: Overriding {@link #handleException(Path, Throwable)} is <i>highly</i> recommended.
 * 
 * @author Toberumono
 */
public class SimpleFileManager extends AbstractFileManager {
	
	/**
	 * Constructs an {@link SimpleFileManager} on the default {@link FileSystem} (retrieved by calling
	 * {@link FileSystems#getDefault()}) and with a maximum processing {@link Thread} count equal to one half of the
	 * available processors (retrieved by calling {@link Runtime#availableProcessors()}).
	 * 
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the default {@link FileSystem}
	 */
	public SimpleFileManager() throws IOException {
		super();
	}
	
	/**
	 * Constructs an {@link SimpleFileManager} that ignores hidden files (those with names starting with a '.') on the given
	 * {@link FileSystem} and with the given maximum processing {@link Thread} count.
	 * 
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link SimpleFileManager} can use. <i>Must
	 *            be at least 1</i>
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public SimpleFileManager(int maxThreads) throws IOException {
		super(maxThreads);
	}
	
	/**
	 * Constructs an {@link SimpleFileManager} on the given {@link FileSystem}, with the given maximum processing
	 * {@link Thread} count, and the given {@link WatchEvent} {@link Comparator} to use when prioritizing {@link WatchEvent
	 * WatchEvents} for processing.
	 * 
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link SimpleFileManager} can use. <i>Must
	 *            be at least 1</i>
	 * @param filter
	 *            an {@link IOExceptedPredicate} that returns {@code true} iff the {@link SimpleFileManager} should process
	 *            {@link WatchEvent WatchEvents} with the given {@link Path} as their {@link WatchEvent#context() context}
	 *            (this applies to both files and directories)
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public SimpleFileManager(int maxThreads, IOExceptedPredicate<Path> filter) throws IOException {
		super(maxThreads, filter);
	}
	
	/**
	 * Constructs an {@link SimpleFileManager} on the given {@link FileSystem}, with the given maximum processing
	 * {@link Thread} count, and the given {@link WatchEvent} {@link Comparator} to use when prioritizing {@link WatchEvent
	 * WatchEvents} for processing.
	 * 
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link SimpleFileManager} can use. <i>Must
	 *            be at least 1</i>
	 * @param filter
	 *            an {@link IOExceptedPredicate} that returns {@code true} iff the {@link SimpleFileManager} should process
	 *            {@link WatchEvent WatchEvents} with the given {@link Path} as their {@link WatchEvent#context() context}
	 *            (this applies to both files and directories)
	 * @param watchEventKindsComparator
	 *            the {@link Comparator} to use when prioritizing {@link WatchEvent WatchEvents}
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public SimpleFileManager(int maxThreads, IOExceptedPredicate<Path> filter, Comparator<WatchEvent<?>> watchEventKindsComparator) throws IOException {
		super(maxThreads, filter, watchEventKindsComparator);
	}
	
	@Override
	protected void onAddFile(Path path) throws IOException {}
	
	@Override
	protected void onAddDirectory(Path path) throws IOException {}
	
	@Override
	protected void onChangeFile(Path path) throws IOException {}
	
	@Override
	protected void onChangeDirectory(Path path) throws IOException {}
	
	@Override
	protected void onRemoveFile(Path path) throws IOException {}
	
	@Override
	protected void onRemoveDirectory(Path path) throws IOException {}
	
	@Override
	protected void handleException(Path path, Throwable t) {}
}
