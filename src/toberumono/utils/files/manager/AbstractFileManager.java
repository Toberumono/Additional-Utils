package toberumono.utils.files.manager;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import com.sun.nio.file.SensitivityWatchEventModifier;

import toberumono.utils.functions.IOExceptedConsumer;
import toberumono.utils.functions.IOExceptedPredicate;

/**
 * An extendible implementation of the core logic needed to implement a thread-safe {@link FileManager}.
 * 
 * @author Toberumono
 */
public abstract class AbstractFileManager implements FileManager {
	
	/**
	 * A {@link Predicate} that returns {@code true} for all {@link Path Paths} that point to items that are <i>not</i> hidden (as defined by
	 * {@link Files#isHidden(Path)}).
	 */
	public static final IOExceptedPredicate<Path> DEFAULT_FILTER = ((IOExceptedPredicate<Path>) Files::isHidden).negate();
	
	/**
	 * The {@link Comparator} used for sorting {@link Path Paths} for use in
	 * {@link PathUpdater#applyToCollection(Collection, Comparator, IOExceptedPredicate, PathUpdater)}
	 */
	private static final Comparator<Path> PATH_COLLECTION_SORTER = (a, b) -> Integer.valueOf(b.getNameCount()).compareTo(Integer.valueOf(a.getNameCount()));
	private static final long watchLoopTimeout = 500;
	private static final TimeUnit watchLoopTimeoutUnit = TimeUnit.MILLISECONDS;
	private static final Object FILE_PLACEHOLDER = new Object();
	
	private final ExecutorService pool;
	private final CompletionService<Void> cs;
	private final WatchKeyProcessor watchKeyProcessor;
	private final WatchEventProcessor watchEventProcessor;
	private final ErrorHandler errorHandler;
	private final Map<Path, Object> paths;
	private final Map<Path, Path> symlinks;
	private final WatchService watchService;
	private final BlockingQueue<WatchEvent<?>> watchQueue;
	private final ReadWriteLock closeLock;
	private final Map<Path, CompletableFuture<Void>> activePaths;
	private transient volatile Set<Path> unmodifiablePaths;
	private final IOExceptedPredicate<Path> filter;
	private volatile boolean closed;
	private final int maxDepth;
	
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
	public AbstractFileManager(FileSystem fileSystem, int maxThreads, IOExceptedPredicate<Path> filter) throws IOException {
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
	public AbstractFileManager(FileSystem fileSystem, int maxThreads, IOExceptedPredicate<Path> filter, Comparator<WatchEvent<?>> watchEventKindsComparator) throws IOException {
		paths = new ConcurrentHashMap<>();
		symlinks = new ConcurrentHashMap<>();
		this.filter = filter;
		maxDepth = Integer.MAX_VALUE;
		closeLock = new ReentrantReadWriteLock(true); //Ensures that this can be closed
		activePaths = new ConcurrentHashMap<>();
		watchService = fileSystem.newWatchService();
		watchQueue = new PriorityBlockingQueue<>(4, DEFAULT_KINDS_COMPARATOR);
		pool = Executors.newWorkStealingPool(maxThreads);
		cs = new ExecutorCompletionService<>(pool);
		(watchKeyProcessor = new WatchKeyProcessor()).setDaemon(true);
		(watchEventProcessor = new WatchEventProcessor()).setDaemon(true);
		(errorHandler = new ErrorHandler()).setDaemon(true);
		watchKeyProcessor.start();
		watchEventProcessor.start();
		errorHandler.start();
	}
	
	/**
	 * Flags a {@link Path} as being processed by an operation in the {@link AbstractFileManager}. This is used to allow
	 * multiple operations to run simultaneously provided that they do not contain the same {@link Path}.
	 * 
	 * @param path
	 *            the {@link Path} to mark as active
	 * @return a {@link CompletableFuture} that should be marked as complete when the operation using the {@link Path Paths} is complete
	 */
	protected final CompletableFuture<Void> markActiveAndWait(Path path) {
		final CompletableFuture<Void> holder = new CompletableFuture<>();
		final Set<CompletableFuture<Void>> activeFutures = new HashSet<>();
		synchronized (activePaths) {
			addActiveFutures(path, holder, activeFutures);
		}
		if (activeFutures.size() > 0)
			CompletableFuture.allOf(activeFutures.toArray((CompletableFuture[]) Array.newInstance(CompletableFuture.class, activeFutures.size()))).join(); //Wait until the paths are available
		return holder;
	}
	
	/**
	 * Flags a {@link Set} of {@link Path Paths} as being processed by an operation in the {@link AbstractFileManager}. This
	 * is used to allow multiple operations to run simultaneously provided that they do not contain the same {@link Path}.
	 * All {@link Path Paths} in the {@link Set} point to the same sync object in the {@link Map}.
	 * 
	 * @param paths
	 *            the {@link Path Paths} to mark as active
	 * @return a {@link CompletableFuture} that will wait until the requested {@link Path Paths} are available
	 */
	protected CompletableFuture<Void> markActiveAndWait(Set<Path> paths) {
		final CompletableFuture<Void> holder = new CompletableFuture<>();
		final Set<CompletableFuture<Void>> activeFutures = new HashSet<>();
		synchronized (activePaths) {
			for (Path path : paths)
				addActiveFutures(path, holder, activeFutures);
		}
		if (activeFutures.size() > 0)
			CompletableFuture.allOf(activeFutures.toArray((CompletableFuture[]) Array.newInstance(CompletableFuture.class, activeFutures.size()))).join(); //Wait until the paths are available
		return holder;
	}
	
	/**
	 * Helper method for {@link #markActiveAndWait(Path)} and {@link #markActiveAndWait(Set)}.
	 * 
	 * @param path
	 *            the {@link Path} being checked
	 * @param holder
	 *            the {@link CompletableFuture} that will be marked as complete when the operation for which the {@link Path Paths} are being reserved
	 *            completes
	 * @param activeFutures
	 *            the to which the {@link CompletableFuture CompletableFutures} currently reserving the {@link Path Paths} will be added
	 */
	private void addActiveFutures(Path path, CompletableFuture<Void> holder, Set<CompletableFuture<Void>> activeFutures) {
		Entry<Path, CompletableFuture<Void>> active;
		CompletableFuture<Void> activeFuture;
		Path activePath;
		for (Iterator<Entry<Path, CompletableFuture<Void>>> iter = activePaths.entrySet().iterator(); iter.hasNext();) {
			active = iter.next();
			activePath = active.getKey();
			activeFuture = active.getValue();
			if (activeFuture != null && activePath != null && activeFuture != holder) { //Avoids potential circular dependencies
				if (activeFuture.isDone())
					iter.remove();
				else {
					//If the current path is a sub-directory of an active path or an active path is a sub-directory of the current path
					if (path.startsWith(activePath) || activePath.startsWith(path))
						activeFutures.add(activePaths.get(activeFuture));
					active.setValue(holder);
				}
			}
		}
		activePaths.put(path, holder);
	}
	
	/**
	 * Flags the given {@link CompletableFuture} as complete, thereby allowing other operations that use the {@link Path Paths} mapped to the
	 * {@link CompletableFuture} to proceed.<br>
	 * If {@code active} is {@code null}, the method will simply return immediately.
	 * 
	 * @param active
	 *            the {@link CompletableFuture} to mark as complete
	 */
	protected void markInactive(CompletableFuture<Void> active) {
		if (active == null)
			return;
		active.complete(null);
		if (!activePaths.containsValue(active)) //This is a quasi-double checked locking structure - we don't want to submit unnecessary tasks
			return;
		if (!closed)
			pool.submit(() -> { //This doesn't need to be synchronized because we're using a ConcurrentHashMap
				while (activePaths.containsValue(active))
					activePaths.values().remove(active);
			});
	}
	
	/**
	 * {@inheritDoc}<br>
	 * If an error occurs during the operation, all changes made will be reverted to before the operation started via a
	 * rollback procedure.
	 */
	@Override
	public void add(Path path) throws IOException {
		Set<Path> resources = null;
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (!Files.isDirectory(path))
				throw new NotDirectoryException(path.toString());
			if (!paths.containsKey(path)) {
				markActive(resources = analyzeTree(path)); //Just to ensure that once assigned a value, resources are immediately marked as active
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
			if (resources != null)
				markInactive(resources);
			closeLock.readLock().unlock();
		}
	}
	
	private void innerAdd(Path path) throws IOException {
		PathAdder pa = new PathAdder();
		try {
			Files.walkFileTree(path, Collections.EMPTY_SET, maxDepth, pa);
		}
		catch (IOException e) {
			pa.rewind();
			throw e;
		}
	}
	
	/**
	 * {@inheritDoc}<br>
	 * If an error occurs during the operation, all changes made will be reverted to before the operation started via a
	 * rollback procedure.
	 */
	@Override
	public void remove(Path path) throws IOException {
		if (!paths.containsKey(path))
			return;
		CompletableFuture<Void> active = null;
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (!Files.isDirectory(path))
				throw new NotDirectoryException(path.toString());
			Set<Path> treeAnalysis = analyzeTree(path);
			active = markActiveAndWait(treeAnalysis); //Just to ensure that once assigned a value, resources are immediately marked as active
			if (paths.containsKey(path))
				innerRemove(path, expandMinimalPathSet(treeAnalysis));
		}
		finally {
			markInactive(active);
			closeLock.readLock().unlock();
		}
	}
	
	private void innerRemove(Path path, Set<Path> treeAnalysis) throws IOException {
		PathRemover pr = new PathRemover();
		try {
			if (treeAnalysis != null && !Files.exists(path)) {
				PathUpdater.applyToCollection(treeAnalysis, PATH_COLLECTION_SORTER,
						p -> this.paths.get(p) instanceof WatchKey, pr);
			}
			else {
				Files.walkFileTree(path, Collections.EMPTY_SET, maxDepth, pr);
			}
		}
		catch (IOException e) {
			pr.rewind();
			throw e;
		}
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
	 * Overrides of this method should implement logic to handle any exceptions thrown when detected {@link WatchEvent
	 * WatchEvents} are processed.
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
			synchronized (this) {
				if (unmodifiablePaths == null)
					unmodifiablePaths = Collections.unmodifiableSet(paths.keySet());
			}
		return unmodifiablePaths;
	}
	
	protected final Path register(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		paths.put(path, Files.isDirectory(path) ? path.register(watchService, ALL_STANDARD_KINDS_ARRAY, SensitivityWatchEventModifier.HIGH) : FILE_PLACEHOLDER);
		return path;
	}
	
	protected final Path deregister(Path path) {
		if (closed)
			throw new ClosedFileManagerException();
		Object removed = paths.remove(path);
		if (removed instanceof WatchKey)
			((WatchKey) removed).cancel();
		return path;
	}
	
	protected void watchEventProcessor(WatchEvent.Kind<?> kind, Path path) throws IOException {
		CompletableFuture<Void> active = null;
		try {
			if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
				Set<Path> treeAnalysis = analyzeTree(path);
				active = markActiveAndWait(treeAnalysis);
				if (paths.containsKey(path)) {
					if (paths.get(path) instanceof WatchKey)
						innerRemove(path, expandMinimalPathSet(treeAnalysis));
					else
						onRemoveFile(path);
				}
			}
			else {
				active = markActiveAndWait(path); //For create and modify, we only need to reserve the current path
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					if (!paths.containsKey(path)) {
						if (Files.isDirectory(path))
							innerAdd(path);
						else
							onAddFile(register(path));
					}
				}
				else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					if (paths.containsKey(path)) {
						if (Files.isDirectory(path))
							onChangeDirectory(path);
						else
							onChangeFile(path);
					}
				}
			}
		}
		finally {
			markInactive(active);
		}
	}
	
	private Set<Path> analyzeTree(Path root) throws IOException {
		Set<Path> paths = new HashSet<>();
		if (Files.exists(root))
			Files.walkFileTree(root, Collections.EMPTY_SET, maxDepth, new TreeAnalyzer(paths, root));
		else { //If the root path doesn't exist, then we fall back on our internal path table
			buildPathTreeFromTable(root, paths);
		}
		return paths;
	}
	
	private void buildPathTreeFromTable(Path root, Set<Path> paths) {
		paths.add(root);
		for (Path path : this.paths.keySet())
			if (path.startsWith(root) && symlinks.containsKey(path))
				buildPathTreeFromTable(symlinks.get(path), paths);
	}
	
	private Set<Path> expandMinimalPathSet(Set<Path> paths) {
		Set<Path> expanded = new HashSet<>();
		for (Path path : paths)
			for (Path p : this.paths.keySet())
				if (p.startsWith(path))
					expanded.add(p);
		return expanded;
	}
	
	private class PathAdder extends PathUpdater<Path, IOExceptedConsumer<Path>> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir) || !filter.test(dir))
				return FileVisitResult.SKIP_SUBTREE;
			register(dir);
			recordStep(dir, AbstractFileManager.this::deregister);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(file) && filter.test(file)) {
				register(file);
				recordStep(file, AbstractFileManager.this::deregister);
				onAddFile(file);
				recordStep(file, AbstractFileManager.this::onRemoveFile);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc != null)
				throw exc;
			onAddDirectory(dir);
			recordStep(dir, AbstractFileManager.this::onRemoveDirectory);
			return FileVisitResult.CONTINUE;
		}
	}
	
	private class PathRemover extends PathUpdater<Path, IOExceptedConsumer<Path>> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			deregister(dir);
			recordStep(dir, AbstractFileManager.this::register);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file)) {
				deregister(file);
				recordStep(file, AbstractFileManager.this::register);
				onRemoveFile(file);
				recordStep(file, AbstractFileManager.this::onAddFile);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc != null)
				throw exc;
			onRemoveDirectory(dir);
			recordStep(dir, AbstractFileManager.this::onAddDirectory);
			return FileVisitResult.CONTINUE;
		}
	}
	
	private class ErrorHandler extends Thread {
		
		public ErrorHandler() {
			super("ErrorHandler");
		}
		
		@Override
		public final void run() {
			try { //Initializing future as null avoids a double-length wait on the first iteration
				for (Future<Void> future = null; !AbstractFileManager.this.closed; future = cs.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (future == null || future.isCancelled())
						continue;
					try {
						future.get();
					}
					catch (ExecutionException e) {
						if (e.getCause() instanceof ProcessorException) {
							ProcessorException exc = (ProcessorException) e.getCause();
							handleException(exc.getPath(), exc.getCause());
						}
						else
							handleException(null, e.getCause());
					}
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private class WatchEventProcessor extends Thread {
		
		public WatchEventProcessor() {
			super("WatchEventProcessor");
		}
		
		@Override
		public final void run() {
			try { //Initializing event as null avoids a double-length wait on the first iteration
				for (WatchEvent<?> event = null; !AbstractFileManager.this.closed; event = watchQueue.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (event == null)
						continue;
					final Path path = (Path) event.context();
					final WatchEvent.Kind<?> kind = event.kind();
					cs.submit(() -> {
						try {
							watchEventProcessor(kind, path);
							return null; //In order to throw an exception, we must return null here (Makes it Callable instead of Runnable)
						}
						catch (IOException e) {
							throw new ProcessorException(path, e);
						}
					});
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private class WatchKeyProcessor extends Thread {
		
		public WatchKeyProcessor() {
			super("WatchKeyProcessor");
		}
		
		@Override
		public final void run() {
			try { //Initializing key as null avoids a double-length wait on the first iteration
				List<WatchEvent<?>> events = null;
				for (WatchKey key = null; !AbstractFileManager.this.closed; key = watchService.poll(watchLoopTimeout, watchLoopTimeoutUnit)) {
					if (key != null) {
						events = key.pollEvents();
						key.reset();
						process((Path) key.watchable(), events);
					}
				}
			}
			catch (ClosedWatchServiceException e) {
				if (!AbstractFileManager.this.closed)
					throw e;
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to process path information."); //TODO there has to be a better way to handle this
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		public final void process(Path path, List<WatchEvent<?>> events) throws IOException {
			for (WatchEvent<?> e : events)
				if (filter.test((Path) e.context()))
					watchQueue.add(new ReWrappedWatchEvent(e, path));
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed) //If it's already closed, don't bother with the lock
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
			Thread.currentThread().interrupt();
		}
	}
	
	private class TreeAnalyzer extends SimpleFileVisitor<Path> {
		private final Set<Path> paths;
		private final Stack<Path> route;
		
		public TreeAnalyzer(Set<Path> paths, Path root) {
			this.paths = paths;
			route = new Stack<>();
			route.push(root);
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.contains(dir) || !filter.test(dir))
				return FileVisitResult.SKIP_SUBTREE;
			if (!dir.startsWith(route.peek()))
				paths.add(dir);
			route.push(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!filter.test(file))
				return FileVisitResult.CONTINUE;
			if (!file.startsWith(route.peek()))
				paths.add(file);
			if (attrs.isSymbolicLink())
				Files.walkFileTree(Files.readSymbolicLink(file), Collections.EMPTY_SET, maxDepth, this);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			route.pop();
			if (exc != null)
				throw exc;
			return FileVisitResult.CONTINUE;
		}
	}
}

class ReWrappedWatchEvent implements WatchEvent<Path> {
	private final WatchEvent<Path> core;
	private final Path context;
	
	@SuppressWarnings("unchecked")
	public ReWrappedWatchEvent(WatchEvent<?> core, Path path) {
		this.core = (WatchEvent<Path>) core;
		context = path.resolve(this.core.context());
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
