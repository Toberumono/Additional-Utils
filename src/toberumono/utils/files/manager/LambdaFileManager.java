package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.function.BiConsumer;

import toberumono.utils.functions.IOExceptedConsumer;

/**
 * An implementation of {@link AbstractFileManager} that forwards additions, changes, and removals to
 * {@link IOExceptedConsumer} lambdas that are supplied on construction.
 * 
 * @author Toberumono
 */
public class LambdaFileManager extends AbstractFileManager {
	private final IOExceptedConsumer<Path> onAddFile, onAddDirectory, onRemoveFile, onRemoveDirectory, onChangeFile, onChangeDirectory;
	private final BiConsumer<Path, Throwable> handleThrowable;
	
	/**
	 * Constructs a {@link LambdaFileManager} on the default {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths} (applies to both files and directories)
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths} (applies to both files and directories)
	 * @param onChange
	 *            the function to call when the {@link WatchService} detects a change in the managed directories
	 * @param handleException
	 *            the function to call when any of the on* methods throws an exception
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public LambdaFileManager(IOExceptedConsumer<Path> onAdd, IOExceptedConsumer<Path> onRemove, IOExceptedConsumer<Path> onChange, BiConsumer<Path, Throwable> handleException) throws IOException {
		this(onAdd, onRemove, onChange, handleException, FileSystems.getDefault());
	}
	
	/**
	 * Constructs a {@link LambdaFileManager} on the given {@link FileSystem} with the given actions.
	 * 
	 * @param onAdd
	 *            the function to call on newly added {@link Path Paths} (applies to both files and directories)
	 * @param onRemove
	 *            the function to call on newly removed {@link Path Paths} (applies to both files and directories)
	 * @param onChange
	 *            the function to call on changed {@link Path Paths} (as detected by the {@link WatchService}, applies to
	 *            both files and directories)
	 * @param handleException
	 *            the function to call when any of the on* methods throws an exception
	 * @param fileSystem
	 *            the {@link FileSystem} that the {@link WatchService} will be monitoring
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public LambdaFileManager(IOExceptedConsumer<Path> onAdd, IOExceptedConsumer<Path> onRemove, IOExceptedConsumer<Path> onChange, BiConsumer<Path, Throwable> handleException, FileSystem fileSystem)
			throws IOException {
		this(onAdd, onAdd, onRemove, onRemove, onChange, onChange, handleException, fileSystem);
	}
	
	/**
	 * Construct a {@link LambdaFileManager} on the given {@link FileSystem} with the given actions.
	 * 
	 * @param onAddFile
	 *            the function to call on newly added files
	 * @param onAddDirectory
	 *            the function to call on newly added directories
	 * @param onRemoveFile
	 *            the function to call on newly removed files
	 * @param onRemoveDirectory
	 *            the function to call on newly removed directories
	 * @param onChangeFile
	 *            the function to call on changed files (as detected by the {@link WatchService})
	 * @param onChangeDirectory
	 *            the function to call on changed directories (as detected by the {@link WatchService})
	 * @param handleThrowable
	 *            the function to call when any of the on* methods throws an exception
	 * @param fileSystem
	 *            the {@link FileSystem} that the {@link WatchService} will be monitoring
	 * @throws IOException
	 *             if an I/O error occurs while initializing the {@link WatchService}
	 */
	public LambdaFileManager(IOExceptedConsumer<Path> onAddFile, IOExceptedConsumer<Path> onAddDirectory, IOExceptedConsumer<Path> onRemoveFile, IOExceptedConsumer<Path> onRemoveDirectory,
			IOExceptedConsumer<Path> onChangeFile, IOExceptedConsumer<Path> onChangeDirectory, BiConsumer<Path, Throwable> handleThrowable, FileSystem fileSystem) throws IOException {
		super(fileSystem);
		this.onAddFile = Objects.requireNonNull(onAddFile, "onAddFile cannot be null");
		this.onAddDirectory = Objects.requireNonNull(onAddDirectory, "onAddDirectory cannot be null");
		this.onChangeFile = Objects.requireNonNull(onChangeFile, "onChangeFile cannot be null");
		this.onChangeDirectory = Objects.requireNonNull(onChangeFile, "onChangeDirectory cannot be null");
		this.onRemoveFile = Objects.requireNonNull(onRemoveFile, "onRemoveFile cannot be null");
		this.onRemoveDirectory = Objects.requireNonNull(onRemoveDirectory, "onRemoveDirectory cannot be null");
		this.handleThrowable = Objects.requireNonNull(handleThrowable, "handleThrowable cannot be null");
	}
	
	@Override
	protected void onAddFile(Path path) throws IOException {
		onAddFile.accept(path);
	}
	
	@Override
	protected void onAddDirectory(Path path) throws IOException {
		onAddDirectory.accept(path);
	}
	
	@Override
	protected void onChangeFile(Path path) throws IOException {
		onChangeFile.accept(path);
	}
	
	@Override
	protected void onChangeDirectory(Path path) throws IOException {
		onChangeDirectory.accept(path);
	}
	
	@Override
	protected void onRemoveFile(Path path) throws IOException {
		onRemoveFile.accept(path);
	}
	
	@Override
	protected void onRemoveDirectory(Path path) throws IOException {
		onRemoveDirectory.accept(path);
	}
	
	@Override
	protected void handleException(Path path, Throwable t) {
		handleThrowable.accept(path, t);
	}
}
