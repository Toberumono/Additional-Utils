package toberumono.utils.files;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import toberumono.utils.functions.IOExceptedConsumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A simple abstraction for recursively monitoring directories.
 * 
 * @author Toberumono
 */
public class FileManager implements Closeable {
	private final WatchService watcher;
	private final Map<Path, WatchKey> paths;
	private final PathAdder adder;
	private final PathRemover remover;
	private final IOExceptedConsumer<Path> onAddFile, onAddDirectory, onRemoveFile, onRemoveDirectory;
	private final IOExceptedConsumer<WatchKey> onChange;
	private boolean closed, changed;
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
			IOExceptedConsumer<WatchKey> onChange, FileSystem fs)
					throws IOException {
		paths = new LinkedHashMap<>();
		watcher = new SimpleWatcher(this::onChange, fs.newWatchService());
		adder = new PathAdder();
		remover = new PathRemover();
		closed = false;
		changed = false;
		this.onAddFile = onAddFile;
		this.onAddDirectory = onAddDirectory;
		this.onChange = onChange;
		this.onRemoveFile = onRemoveFile;
		this.onRemoveDirectory = onRemoveDirectory;
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
	public synchronized boolean add(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		if (paths.containsKey(path))
			return false;
		if (!Files.isDirectory(path))
			register(path);
		else
			Files.walkFileTree(path, adder);
		boolean didchange = changed;
		changed = false;
		return didchange;
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
	public synchronized boolean remove(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		if (!paths.containsKey(path))
			return false;
		Files.walkFileTree(path, remover);
		boolean didchange = changed;
		changed = false;
		return didchange;
	}
	
	/**
	 * @return the {@link Path Paths} managed by this {@link FileManager}
	 */
	public Set<Path> getPaths() {
		if (closed)
			throw new ClosedFileManagerException();
		if (pathSet == null)
			pathSet = Collections.unmodifiableSet(paths.keySet());
		return pathSet;
	}
	
	private void register(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		paths.put(path, path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
		changed = true;
	}
	
	private void deregister(Path path) {
		if (closed)
			throw new ClosedFileManagerException();
		paths.remove(path).cancel();
		changed = true;
	}
	
	private class PathAdder extends SimpleFileVisitor<Path> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			register(dir);
			onAddDirectory(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file))
				deregister(file);
			else
				onAddFile(file);
			return FileVisitResult.CONTINUE;
		}
	}
	
	private class PathRemover extends SimpleFileVisitor<Path> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			deregister(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file)) {
				deregister(file);
				onRemoveFile(file);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			onRemoveDirectory(dir);
			return FileVisitResult.CONTINUE;
		}
	}
	
	@Override
	public synchronized void close() throws IOException {
		if (closed)
			return;
		closed = true;
		Iterator<Map.Entry<Path, WatchKey>> iter = paths.entrySet().iterator();
		IOException except = null;
		while (iter.hasNext()) {
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
}
