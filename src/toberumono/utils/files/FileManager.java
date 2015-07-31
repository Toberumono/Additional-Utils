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
import java.util.HashMap;
import java.util.Iterator;
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
	private final Consumer<Path> onAdd, onRemove;
	
	/**
	 * Construct a {@link FileManager} on the default {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths}
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths}
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the {@link FileSystem}
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public FileManager(Consumer<Path> onAdd, Consumer<Path> onRemove, Consumer<WatchKey> onChange) throws IOException {
		this(onAdd, onRemove, onChange, FileSystems.getDefault());
	}
	
	/**
	 * Construct a {@link FileManager} on the given {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths}
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths}
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the {@link FileSystem}
	 * @param fs
	 *            the {@link FileSystem} that the {@link WatchService} will be monitoring
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public FileManager(Consumer<Path> onAdd, Consumer<Path> onRemove, Consumer<WatchKey> onChange, FileSystem fs) throws IOException {
		this.paths = new HashMap<>();
		watcher = new SimpleWatcher(onChange, fs.newWatchService());
		adder = new PathAdder();
		remover = new PathRemover();
		this.onAdd = onAdd;
		this.onRemove = onRemove;
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
	public void add(Path path) throws IOException {
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
	public void remove(Path path) throws IOException {
		if (!paths.containsKey(path))
			return;
		Files.walkFileTree(path, remover);
	}
	
	/**
	 * @return the {@link Path Paths} managed by this {@link FileManager}
	 */
	public Set<Path> getPaths() {
		return Collections.unmodifiableSet(paths.keySet());
	}
	
	private void register(Path path) throws IOException {
		paths.put(path, path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
	}
	
	private void deregister(Path path) {
		paths.remove(path).cancel();
	}
	
	private class PathAdder extends SimpleFileVisitor<Path> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(dir))
				return FileVisitResult.SKIP_SUBTREE;
			register(dir);
			onAdd.accept(dir);
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (paths.containsKey(file))
				deregister(file);
			else
				onAdd.accept(file);
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
				onRemove.accept(file);
			}
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			onRemove.accept(dir);
			return FileVisitResult.CONTINUE;
		}
	}
	
	@Override
	public void close() throws IOException {
		Iterator<Map.Entry<Path, WatchKey>> iter = paths.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Path, WatchKey> removed = iter.next();
			removed.getValue().cancel();
			iter.remove();
		}
		watcher.close();
	}
}
