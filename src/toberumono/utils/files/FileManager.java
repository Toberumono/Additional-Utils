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
import java.util.function.Consumer;

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
	private final Consumer<Path> onAddFile, onAddDirectory, onRemoveFile, onRemoveDirectory;
	private boolean closed;
	
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
	public FileManager(Consumer<Path> onAdd, Consumer<Path> onRemove, Consumer<WatchKey> onChange) throws IOException {
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
	public FileManager(Consumer<Path> onAdd, Consumer<Path> onRemove, Consumer<WatchKey> onChange, FileSystem fs) throws IOException {
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
	public FileManager(Consumer<Path> onAddFile, Consumer<Path> onAddDirectory, Consumer<Path> onRemoveFile, Consumer<Path> onRemoveDirectory, Consumer<WatchKey> onChange, FileSystem fs)
			throws IOException {
		paths = new LinkedHashMap<>();
		watcher = new SimpleWatcher(onChange, fs.newWatchService());
		adder = new PathAdder();
		remover = new PathRemover();
		closed = false;
		this.onAddFile = onAddFile;
		this.onAddDirectory = onAddDirectory;
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
	 * @throws IOException
	 *             if an I/O error occurs while adding the {@link Path}
	 */
	public synchronized void add(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		if (paths.containsKey(path))
			return;
		if (!Files.isDirectory(path))
			register(path);
		else
			Files.walkFileTree(path, adder);
	}
	
	/**
	 * Recursively removes the {@link Path} and any sub-directories from the {@link FileManager}.
	 * 
	 * @param path
	 *            the {@link Path} to remove from the {@link FileManager}
	 * @throws IOException
	 *             if an I/O error occurs while removing the {@link Path}
	 */
	public synchronized void remove(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		if (!paths.containsKey(path))
			return;
		Files.walkFileTree(path, remover);
	}
	
	/**
	 * @return the {@link Path Paths} managed by this {@link FileManager}
	 */
	public Set<Path> getPaths() {
		if (closed)
			throw new ClosedFileManagerException();
		return Collections.unmodifiableSet(paths.keySet());
	}
	
	private void register(Path path) throws IOException {
		if (closed)
			throw new ClosedFileManagerException();
		paths.put(path, path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
	}
	
	private void deregister(Path path) {
		if (closed)
			throw new ClosedFileManagerException();
		paths.remove(path).cancel();
	}
	
	private class PathAdder extends SimpleFileVisitor<Path> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			register(dir);
			onAddDirectory.accept(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file))
				deregister(file);
			else
				onAddFile.accept(file);
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
				onRemoveFile.accept(file);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			onRemoveDirectory.accept(dir);
			return FileVisitResult.CONTINUE;
		}
	}
	
	@Override
	public synchronized void close() throws IOException {
		if (closed)
			return;
		closed = true;
		Iterator<Map.Entry<Path, WatchKey>> iter = paths.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Path, WatchKey> removed = iter.next();
			removed.getValue().cancel();
			iter.remove();
		}
		watcher.close();
	}
}
