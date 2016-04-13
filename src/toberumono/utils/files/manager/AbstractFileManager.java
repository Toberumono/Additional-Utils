package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import toberumono.utils.files.ClosedFileManagerException;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * An extendible implementation of the core logic needed to implement a thread-safe {@link FileManager}.
 * 
 * @author Toberumono
 */
public abstract class AbstractFileManager implements FileManager {
	/**
	 * A {@link Comparator} for {@link StandardWatchEventKinds} that prioritizes the kinds from greatest to least as follows:
	 * <ol>
	 * <li>{@link StandardWatchEventKinds#ENTRY_CREATE ENTRY_CREATE}</li>
	 * <li>{@link StandardWatchEventKinds#ENTRY_MODIFY ENTRY_MODIFY}</li>
	 * <li>{@link StandardWatchEventKinds#ENTRY_DELETE ENTRY_DELETE}</li>
	 * </ol>
	 */
	public static final Comparator<WatchEvent<?>> DEFAULT_KINDS_COMPARATOR = (a, b) -> {
		if (a.kind().equals(b.kind())) //We only need to check this once
			return 0;
		if (a.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) //ENTRY_CREATE has first priority, and we know that a.kind() != b.kind(), so a > b
			return 1;
		if (a.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) //ENTRY_MODIFY has second priority (after ENTRY_CREATE, but before ENTRY_DELETE)
			return b.kind().equals(StandardWatchEventKinds.ENTRY_CREATE) ? -1 : 1; //ENTRY_CREATE has higher priority than ENTRY_MODIFY, so b > a
		if (a.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) //ENTRY_DELETE has last priority, and we know that a.kind() != b.kind(), so b > a
			return -1;
		return 0;
	};
	
	/**
	 * A {@link Predicate} that returns {@code true} for all {@link Path Paths} whose {@link Path#getFileName() file name}
	 * components do <i>not</i> start with a '.'
	 */
	public static final Predicate<Path> DEFAULT_FILTER = p -> !p.getFileName().toString().startsWith(".");
	private static final long watchLoopTimeout = 500;
	private static final TimeUnit watchLoopTimeoutUnit = TimeUnit.MILLISECONDS;
	
	private final ExecutorService pool;
	private final WatchKeyProcessor watchKeyProcessor;
	private final WatchEventProcessor watchEventProcessor;
	private final ErrorHandler errorHandler;
	private final Map<Path, WatchKey> paths;
	private final WatchService watchService;
	private final BlockingQueue<WatchEvent<?>> watchQueue;
	private final BlockingQueue<Future<?>> futuresQueue;
	private final ReadWriteLock closeLock;
	private final Set<Path> activePaths;
	private transient Set<Path> unmodifiablePaths;
	private final ThreadGroup threadGroup;
	private final Predicate<Path> filter;
	private boolean closed;
	
	/**
	 * Constructs an {@link AbstractFileManager} on the default {@link FileSystem} (retrieved by calling
	 * {@link FileSystems#getDefault()}) and with a maximum processing {@link Thread} count equal to one half of the
	 * available processors (retrieved by calling {@link Runtime#availableProcessors()}).
	 * 
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the default {@link FileSystem}
	 */
	public AbstractFileManager() throws IOException {
		this(FileSystems.getDefault());
	}
	
	/**
	 * Constructs an {@link AbstractFileManager} on the given {@link FileSystem} and with a maximum processing {@link Thread}
	 * count equal to one half of the available processors (retrieved by calling {@link Runtime#availableProcessors()}).
	 * 
	 * @param fileSystem
	 *            the {@link FileSystem} on which the {@link AbstractFileManager} will manage files
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public AbstractFileManager(FileSystem fileSystem) throws IOException {
		this(fileSystem, Runtime.getRuntime().availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors() / 2 : 1);
	}
	
	/**
	 * Constructs an {@link AbstractFileManager} that ignores hidden files (those with names starting with a '.') on the
	 * given {@link FileSystem} and with the given maximum processing {@link Thread} count.
	 * 
	 * @param fileSystem
	 *            the {@link FileSystem} on which the {@link AbstractFileManager} will manage files
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link AbstractFileManager} can use.
	 *            <i>Must be at least 1</i>
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public AbstractFileManager(FileSystem fileSystem, int maxThreads) throws IOException {
		this(fileSystem, maxThreads, DEFAULT_FILTER, DEFAULT_KINDS_COMPARATOR);
	}
	
	/**
	 * Constructs an {@link AbstractFileManager} on the given {@link FileSystem}, with the given maximum processing
	 * {@link Thread} count, and the given {@link WatchEvent} {@link Comparator} to use when prioritizing {@link WatchEvent
	 * WatchEvents} for processing.
	 * 
	 * @param fileSystem
	 *            the {@link FileSystem} on which the {@link AbstractFileManager} will manage files
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link AbstractFileManager} can use.
	 *            <i>Must be at least 1</i>
	 * @param filter
	 *            a {@link Predicate} that returns {@code true} iff the {@link AbstractFileManager} should process
	 *            {@link WatchEvent WatchEvents} with the given {@link Path} as their {@link WatchEvent#context() context}
	 *            (this applies to both files and directories)
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public AbstractFileManager(FileSystem fileSystem, int maxThreads, Predicate<Path> filter) throws IOException {
		this(fileSystem, maxThreads, filter, DEFAULT_KINDS_COMPARATOR);
	}
	
	/**
	 * Constructs an {@link AbstractFileManager} on the given {@link FileSystem}, with the given maximum processing
	 * {@link Thread} count, and the given {@link WatchEvent} {@link Comparator} to use when prioritizing {@link WatchEvent
	 * WatchEvents} for processing.
	 * 
	 * @param fileSystem
	 *            the {@link FileSystem} on which the {@link AbstractFileManager} will manage files
	 * @param maxThreads
	 *            the maximum number of processing {@link Thread Threads} that the {@link AbstractFileManager} can use.
	 *            <i>Must be at least 1</i>
	 * @param filter
	 *            a {@link Predicate} that returns {@code true} iff the {@link AbstractFileManager} should process
	 *            {@link WatchEvent WatchEvents} with the given {@link Path} as their {@link WatchEvent#context() context}
	 *            (this applies to both files and directories)
	 * @param watchEventKindsComparator
	 *            the {@link Comparator} to use when prioritizing {@link WatchEvent WatchEvents}
	 * @throws IOException
	 *             if a {@link WatchService} could not be created on the given {@link FileSystem}
	 */
	public AbstractFileManager(FileSystem fileSystem, int maxThreads, Predicate<Path> filter, Comparator<WatchEvent<?>> watchEventKindsComparator) throws IOException {
		paths = Collections.synchronizedMap(new LinkedHashMap<>());
		this.filter = filter;
		closeLock = new ReentrantReadWriteLock(true); //Ensures that this can be closed
		activePaths = new HashSet<>();
		watchService = fileSystem.newWatchService();
		watchQueue = new PriorityBlockingQueue<>(4, watchEventKindsComparator);
		futuresQueue = new LinkedBlockingQueue<>();
		pool = Executors.newWorkStealingPool(maxThreads);
		(threadGroup = new ThreadGroup(getClass().getName() + "#threadGroup")).setDaemon(true); //TODO might want to use this to handle uncaught exceptions
		(watchKeyProcessor = new WatchKeyProcessor(threadGroup)).setDaemon(true);
		(watchEventProcessor = new WatchEventProcessor(threadGroup)).setDaemon(true);
		(errorHandler = new ErrorHandler(threadGroup)).setDaemon(true);
		watchKeyProcessor.start();
		watchEventProcessor.start();
		errorHandler.start();
	}
	
	protected final void markActive(Path path) {
		for (;;) {
			synchronized (activePaths) {
				for (Path active : activePaths) {
					if (path.startsWith(active) || path.endsWith(active)) {
						synchronized (active) {
							try {
								active.wait();
							}
							catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					else {
						activePaths.add(path);
						return;
					}
				}
			}
		}
	}
	
	protected final void markInactive(Path path) {
		synchronized (activePaths) {
			synchronized (path) {
				activePaths.remove(path);
				path.notify(); //TODO check that this works for synchronization
			}
		}
	}
	
	/**
	 * {@inheritDoc}<br>
	 * If an error occurs during the operation, all changes made will be reverted to before the operation started via a rollback procedure.
	 */
	@Override
	public void add(Path path) throws IOException {
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (!Files.isDirectory(path))
				throw new NotDirectoryException(path.toString());
			markActive(path);
			if (!paths.containsKey(path)) {
				PathAdder pa = new PathAdder();
				try {
					Files.walkFileTree(path, FOLLOW_LINKS_SET, Integer.MAX_VALUE, pa);
				}
				catch (IOException e) {
					pa.rewind(this::rewindAdd);
					throw e;
				}
			}
		}
		finally {
			markInactive(path);
			closeLock.readLock().unlock();
		}
	}
	
	protected void rewindAdd(Path p, Integer state) throws IOException {
		if (state == 2) {
			if (Files.isDirectory(p))
				onRemoveDirectory(p);
			else
				onRemoveFile(p);
		}
		else if (state == 1)
			deregister(p);
	}

	/**
	 * {@inheritDoc}<br>
	 * If an error occurs during the operation, all changes made will be reverted to before the operation started via a rollback procedure.
	 */
	@Override
	public void remove(Path path) throws IOException {
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (!Files.isDirectory(path))
				throw new NotDirectoryException(path.toString());
			markActive(path);
			if (paths.containsKey(path)) {
				PathRemover pr = new PathRemover();
				try {
					Files.walkFileTree(path, FOLLOW_LINKS_SET, Integer.MAX_VALUE, pr);
				}
				catch (IOException e) {
					pr.rewind(this::rewindRemove);
					throw e;
				}
			}
		}
		finally {
			markInactive(path);
			closeLock.readLock().unlock();
		}
	}
	
	protected void rewindRemove(Path p, Integer state) throws IOException {
		if (state == 2) {
			if (Files.isDirectory(p))
				onAddDirectory(p);
			else
				onAddFile(p);
		}
		else if (state == 1)
			register(p);
	}
	
	/**
	 * Overrides of this method should implement the actions to take when a file is added to the {@link FileManager} or
	 * created in a directory that the {@link FileManager} is watching.<br>
	 * <b>Note:</b> If this method is called from outside of the {@link FileManager FileManager's} internal {@link #add(Path)
	 * addition}, {@link #remove(Path) removal}, or update logic, the result is undefined.
	 * 
	 * @param path
	 *            the {@link Path} to the file being added
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onAddFile(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement the actions to take when a directory is added to the {@link FileManager} or
	 * created in a directory that the {@link FileManager} is watching.
	 * 
	 * @param path
	 *            the {@link Path} to the directory being added
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onAddDirectory(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement the actions to take when a file within a directory that the
	 * {@link FileManager} is watching is changed.
	 * 
	 * @param path
	 *            the {@link Path} to the file that was changed
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onChangeFile(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement the actions to take when a directory that the {@link FileManager} is
	 * watching is changed.
	 * 
	 * @param path
	 *            the {@link Path} to the directory that was changed
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onChangeDirectory(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement the actions to take when a file is removed from the {@link FileManager} or
	 * deleted in a directory that the {@link FileManager} is watching.
	 * 
	 * @param path
	 *            the {@link Path} to the file being removed
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onRemoveFile(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement the actions to take when a directory is removed from the {@link FileManager}
	 * or deleted in a directory that the {@link FileManager} is watching.
	 * 
	 * @param path
	 *            the {@link Path} to the directory being removed
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected abstract void onRemoveDirectory(Path path) throws IOException;
	
	/**
	 * Overrides of this method should implement logic to handle any exceptions thrown within the {@code on*} functions.
	 * 
	 * @param path
	 *            the {@link Path} that was being processed at the time of the exception; if the {@link Path} is not known,
	 *            this parameter will be {@code null}
	 * @param t
	 *            the thrown exception
	 */
	protected abstract void handleException(Path path, Throwable t);
	
	/**
	 * Optional convenience method for implementations that have specific logic for {@link IOException IOExceptions}.<br>
	 * The default implementation forwards to {@link #handleException(Path, Throwable)}.
	 * 
	 * @param path
	 *            the {@link Path} that was being processed at the time of the exception; if the {@link Path} is not known,
	 *            this parameter will be {@code null}
	 * @param e
	 *            the thrown {@link IOException}
	 * @see #handleException(Path, Throwable)
	 */
	protected void handleException(Path path, IOException e) {
		handleException(path, (Throwable) e);
	}
	
	@Override
	public Set<Path> getPaths() {
		if (unmodifiablePaths == null)
			unmodifiablePaths = Collections.unmodifiableSet(paths.keySet());
		return unmodifiablePaths;
	}
	
	protected final void register(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		paths.put(path, Files.isDirectory(path) ? path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) : null);
	}
	
	protected final void deregister(Path path) {
		if (closed)
			throw new ClosedFileManagerException();
		WatchKey removed = paths.remove(path);
		if (removed != null)
			removed.cancel();
	}
	
	protected void watchEventProcessor(WatchEvent.Kind<?> kind, Path path) throws IOException {
		if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
			register(path);
			if (Files.isDirectory(path))
				onAddDirectory(path);
			else
				onAddFile(path);
		}
		else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			if (Files.isDirectory(path))
				onChangeDirectory(path);
			else
				onChangeFile(path);
		}
		else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
			deregister(path);
			if (Files.isDirectory(path))
				onRemoveDirectory(path);
			else
				onRemoveFile(path);
		}
	}
	
	private class PathAdder extends PathUpdater<Path, Integer> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir) || !filter.test(dir))
				return FileVisitResult.SKIP_SUBTREE;
			register(dir);
			updateState(dir, 1);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(file) || !filter.test(file)) {
				register(file);
				updateState(file, 1);
				onAddFile(file);
				updateState(file, 2);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc != null)
				throw exc;
			onAddDirectory(dir);
			updateState(dir, 2);
			return FileVisitResult.CONTINUE;
		}
	}
	
	private class PathRemover extends PathUpdater<Path, Integer> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			deregister(dir);
			updateState(dir, 1);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file)) {
				deregister(file);
				updateState(file, 1);
				onRemoveFile(file);
				updateState(file, 2);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc != null)
				throw exc;
			onRemoveDirectory(dir);
			updateState(dir, 2);
			return FileVisitResult.CONTINUE;
		}
	}
	
	protected class ErrorHandler extends Thread {
		
		public ErrorHandler(ThreadGroup group) {
			super(group, "ErrorHandler");
		}
		
		@Override
		public final void run() {
			List<Future<?>> active = new ArrayList<>();
			try { //Initializing future as null avoids a double-length wait on the first iteration
				for (Future<?> future = null; !AbstractFileManager.this.closed; future = futuresQueue.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (future != null) {
						active.add(future);
						futuresQueue.drainTo(active);
					}
					for (Iterator<Future<?>> iter = active.iterator(); iter.hasNext();) {
						future = iter.next(); //We can safely re-use future here, thereby avoiding an additional memory allocation
						if (future.isDone()) {
							try {
								iter.remove();
								future.get(); //This won't wait because we're only calling it if the task is complete
							}
							catch (ExecutionException e) {
								if (e.getCause() instanceof ProcessorException) {
									ProcessorException exc = (ProcessorException) e.getCause();
									handleException(exc.getPath(), exc.getCause());
								}
								else
									handleException(null, e.getCause());
							}
							catch (CancellationException | InterruptedException e) {
								if (!AbstractFileManager.this.closed)
									e.printStackTrace();
							}
						}
					}
				}
			}
			catch (InterruptedException e) {
				if (!AbstractFileManager.this.closed)
					e.printStackTrace();
			}
		}
	}
	
	protected class WatchEventProcessor extends Thread {
		
		public WatchEventProcessor(ThreadGroup group) {
			super(group, "WatchEventProcessor");
		}
		
		@Override
		public final void run() {
			try { //Initializing event as null avoids a double-length wait on the first iteration
				for (WatchEvent<?> event = null; !AbstractFileManager.this.closed; event = watchQueue.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (event == null)
						continue;
					Path path = (Path) event.context();
					WatchEvent.Kind<?> kind = event.kind();
					futuresQueue.put(pool.submit(() -> {
						try {
							watchEventProcessor(kind, path);
							return null; //In order to throw an exception, we must return null here (Makes it Callable instead of Runnable)
						}
						catch (IOException e) {
							throw new ProcessorException(path, e);
						}
					}));
				}
			}
			catch (InterruptedException e) {
				if (!AbstractFileManager.this.closed)
					e.printStackTrace();
			}
		}
	}
	
	protected class WatchKeyProcessor extends Thread {
		
		public WatchKeyProcessor(ThreadGroup group) {
			super(group, "WatchKeyProcessor");
		}
		
		@Override
		public final void run() {
			try { //Initializing key as null avoids a double-length wait on the first iteration
				List<WatchEvent<?>> events = null;
				for (WatchKey key = null; !AbstractFileManager.this.closed; key = watchService.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (key == null)
						continue;
					events = key.pollEvents();
					key.reset();
					process(key, events);
				}
			}
			catch (ClosedWatchServiceException e) {
				if (!AbstractFileManager.this.closed)
					throw e;
			}
			catch (InterruptedException e) {
				if (!AbstractFileManager.this.closed)
					e.printStackTrace();
			}
		}
		
		@SuppressWarnings("unchecked")
		public final void process(WatchKey key, List<WatchEvent<?>> events) {
			List<WatchEvent<?>> temp = new ArrayList<>(); //TODO might be faster to not create the object...
			for (WatchEvent<?> e : events) {
				if (filter.test((Path) e.context()))
					temp.add(new ReWrappedWatchEvent((WatchEvent<Path>) e, key));
			}
			watchQueue.addAll(temp);
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed) //If it's already closed, don't bother with the 
			return;
		try {
			closeLock.writeLock().lock();
			if (closed)
				return;
			closed = true;
		}
		finally {
			closeLock.writeLock().unlock();
		}
		try {
			watchService.close();
			pool.shutdown();
			while (!pool.awaitTermination(watchLoopTimeout, watchLoopTimeoutUnit));
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class ReWrappedWatchEvent implements WatchEvent<Path> {
	private final WatchEvent<Path> core;
	private final Path context;
	
	public ReWrappedWatchEvent(WatchEvent<Path> core, WatchKey key) {
		this.core = core;
		context = ((Path) key.watchable()).resolve(this.core.context());
	}
	
	@Override
	public Kind<Path> kind() {
		return core.kind();
	}
	
	@Override
	public int count() {
		return core.count();
	}
	
	@Override
	public Path context() {
		return context;
	}
	
}

class ProcessorException extends Exception {
	private final Path path;
	
	public ProcessorException(Path path, Throwable wrapped) {
		super(wrapped);
		this.path = path;
	}
	
	public Path getPath() {
		return path;
	}
}
