package toberumono.utils.files;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import toberumono.utils.functions.IOExceptedConsumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A simple abstraction for recursively monitoring directories.
 * 
 * @author Toberumono
 */
public class FileManager implements Closeable {
	private static final HashSet<FileVisitOption> FOLLOW_LINKS_SET = new HashSet<>(Arrays.asList(new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS}));
	
	private final WatchService watcher;
	private final Map<Path, WatchKey> paths;
	private final IOExceptedConsumer<Path> onAddFile, onAddDirectory, onRemoveFile, onRemoveDirectory;
	private final IOExceptedConsumer<WatchKey> onChange;
	private final ReadWriteLock closeLock;
	private final int maxDepth;
	private boolean closed;
	private Set<Path> pathSet = null;
	
	/**
	 * Constructs a {@link FileManager} on the default {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths} (applies to both files and directories)
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths} (applies to both files and directories)
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the managed directories
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public FileManager(IOExceptedConsumer<Path> onAdd, IOExceptedConsumer<Path> onRemove, IOExceptedConsumer<WatchKey> onChange) throws IOException {
		this(onAdd, onRemove, onChange, FileSystems.getDefault());
	}
	
	/**
	 * Constructs a {@link FileManager} on the given {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths} (applies to both files and directories)
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths} (applies to both files and directories)
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the managed directories
	 * @param fs
	 *            the {@link FileSystem} that the {@link WatchService} will be monitoring
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public FileManager(IOExceptedConsumer<Path> onAdd, IOExceptedConsumer<Path> onRemove, IOExceptedConsumer<WatchKey> onChange, FileSystem fs) throws IOException {
		this(onAdd, onAdd, onRemove, onRemove, onChange, fs);
	}
	
	/**
	 * Construct a {@link FileManager} on the given {@link FileSystem} with the given actions.
	 * 
	 * @param onAddFile
	 *            the function to call on newly added files
	 * @param onAddDirectory
	 *            the function to call on newly added directories
	 * @param onRemoveFile
	 *            the function to call on newly removed files
	 * @param onRemoveDirectory
	 *            the function to call on newly removed directories
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the managed directories
	 * @param fs
	 *            the {@link FileSystem} that the {@link WatchService} will be monitoring
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public FileManager(IOExceptedConsumer<Path> onAddFile, IOExceptedConsumer<Path> onAddDirectory, IOExceptedConsumer<Path> onRemoveFile, IOExceptedConsumer<Path> onRemoveDirectory,
			IOExceptedConsumer<WatchKey> onChange, FileSystem fs) throws IOException {
		closeLock = new ReentrantReadWriteLock();
		paths = Collections.synchronizedMap(new LinkedHashMap<>());
		watcher = new SimpleWatcher(this::onChange, fs.newWatchService());
		closed = false;
		this.onAddFile = onAddFile;
		this.onAddDirectory = onAddDirectory;
		this.onChange = onChange;
		this.onRemoveFile = onRemoveFile;
		this.onRemoveDirectory = onRemoveDirectory;
		this.maxDepth = Integer.MAX_VALUE;
	}
	
	/**
	 * Recursively adds the {@link Path} and any sub-directories to the {@link FileManager}. This will automatically ensure
	 * that no events are double-reported. (So you don't need to remove sub-directories before adding a directory that
	 * contains them)
	 * 
	 * @param path
	 *            the {@link Path} to add to the {@link FileManager}
	 * @return {@code true} if a {@link Path} was added
	 * @throws IOException
	 *             if an I/O error occurs while adding the {@link Path}
	 */
	public boolean add(Path path) throws IOException {
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (paths.containsKey(path))
				return false;
			if (!Files.isDirectory(path))
				return register(path);
			PathAdder adder = new PathAdder();
			Files.walkFileTree(path, FOLLOW_LINKS_SET, maxDepth, adder);
			return adder.madeChange();
		}
		finally {
			closeLock.readLock().unlock();
		}
	}
	
	/**
	 * By default, this simply forwards to the onAddFile method passed to the constructor.<br>
	 * This is implemented for overriding by more complex subclasses that need easier access to instance variables than
	 * lambdas can offer.
	 * 
	 * @param path
	 *            the {@link Path} to the file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void onAddFile(Path path) throws IOException {
		onAddFile.accept(path);
	}
	
	/**
	 * By default, this simply forwards to the onAddDirectory method passed to the constructor.<br>
	 * This is implemented for overriding by more complex subclasses that need easier access to instance variables than
	 * lambdas can offer.
	 * 
	 * @param path
	 *            the {@link Path} to the file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void onAddDirectory(Path path) throws IOException {
		onAddDirectory.accept(path);
	}
	
	/**
	 * By default, this simply forwards to the onChange method passed to the constructor.<br>
	 * This is implemented for overriding by more complex subclasses that need easier access to instance variables than
	 * lambdas can offer.
	 * 
	 * @param key
	 *            the {@link WatchKey} that noticed the change
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void onChange(WatchKey key) throws IOException {
		onChange.accept(key);
	}
	
	/**
	 * By default, this simply forwards to the onRemoveFile method passed to the constructor.<br>
	 * This is implemented for overriding by more complex subclasses that need easier access to instance variables than
	 * lambdas can offer.
	 * 
	 * @param path
	 *            the {@link Path} to the file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void onRemoveFile(Path path) throws IOException {
		onRemoveFile.accept(path);
	}
	
	/**
	 * By default, this simply forwards to the onRemoveDirectory method passed to the constructor.<br>
	 * This is implemented for overriding by more complex subclasses that need easier access to instance variables than
	 * lambdas can offer.
	 * 
	 * @param path
	 *            the {@link Path} to the file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void onRemoveDirectory(Path path) throws IOException {
		onRemoveDirectory.accept(path);
	}
	
	/**
	 * Recursively removes the {@link Path} and any sub-directories from the {@link FileManager}.
	 * 
	 * @param path
	 *            the {@link Path} to remove from the {@link FileManager}
	 * @return {@code true} if a {@link Path} was removed
	 * @throws IOException
	 *             if an I/O error occurs while removing the {@link Path}
	 */
	public boolean remove(Path path) throws IOException {
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (!paths.containsKey(path))
				return false;
			if (!Files.isDirectory(path))
				return deregister(path);
			PathRemover remover = new PathRemover();
			Files.walkFileTree(path, FOLLOW_LINKS_SET, maxDepth, remover);
			return remover.madeChange();
		}
		finally {
			closeLock.readLock().unlock();
		}
	}
	
	/**
	 * @return the {@link Path Paths} managed by this {@link FileManager}
	 */
	public Set<Path> getPaths() {
		try {
			closeLock.readLock().lock();
			if (closed)
				throw new ClosedFileManagerException();
			if (pathSet == null)
				pathSet = Collections.unmodifiableSet(paths.keySet());
			return pathSet;
		}
		finally {
			closeLock.readLock().unlock();
		}
	}
	
	private boolean register(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		return path != paths.put(path, path.register(((SimpleWatcher) watcher).core, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
	}
	
	private boolean deregister(Path path) {
		if (closed)
			throw new ClosedFileManagerException();
		WatchKey removed = paths.remove(path);
		if (removed == null)
			return false;
		removed.cancel();
		return true;
	}
	
	private class PathAdder extends SimpleFileVisitor<Path> {
		private boolean changed = false;
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			if (register(dir))
				changed = true;
			onAddDirectory(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (Files.isDirectory(file))
				System.out.println(file);
			if (paths.containsKey(file)) {
				if (deregister(file)) //deregister because files within directories are watched if those directories are watched
					changed = true;
			}
			else
				onAddFile(file);
			return FileVisitResult.CONTINUE;
		}
		
		public boolean madeChange() {
			return changed;
		}
	}
	
	private class PathRemover extends SimpleFileVisitor<Path> {
		private boolean changed = false;
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			if (deregister(dir))
				changed = true;
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file)) {
				if (deregister(file))
					changed = true;
				onRemoveFile(file);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			onRemoveDirectory(dir);
			return FileVisitResult.CONTINUE;
		}
		
		public boolean madeChange() {
			return changed;
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			closeLock.writeLock().lock();
			if (closed)
				return;
			closed = true;
			IOException except = null;
			for (Iterator<Map.Entry<Path, WatchKey>> iter = paths.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Path, WatchKey> removed = iter.next();
				Path p = removed.getKey();
				try {
					if (Files.isDirectory(p))
						onRemoveDirectory(p);
					else
						onRemoveFile(p);
				}
				catch (IOException e) {
					e.printStackTrace();
					if (except == null)
						except = e;
				}
				removed.getValue().cancel();
				try {
					iter.remove();
				}
				catch (UnsupportedOperationException e) {}
			}
			watcher.close();
			if (except != null)
				throw except;
		}
		finally {
			closeLock.writeLock().unlock();
		}
	}
	
	public static void main(String[] args) throws IOException {
		FileManager fm = new FileManager(p -> {}, p -> {}, k -> {
			List<WatchEvent<?>> events = k.pollEvents();
			k.reset();
			for (WatchEvent<?> event : events)
				System.out.println(event.kind().toString() + ": " + event.context().toString());
		});
		long time = System.nanoTime();
		fm.add(Paths.get("/Users/joshualipstone/Downloads/"));
		fm.add(Paths.get("/Users/joshualipstone/Downloads/Compressed/"));
		System.out.println((System.nanoTime() - time) / 1000000);
		Scanner delay = new Scanner(System.in);
		delay.nextLine();
		fm.close();
		delay.close();
	}
}
