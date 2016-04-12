package toberumono.utils.files.manager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * A trivial implementation of {@link AbstractFileManager} that does nothing. This is useful for implementations that only
 * want to override a subset of the triggered methods (the five methods with names that start with on).<br>
 * Note: Overriding {@link #handleException(Path, Throwable)} is <i>highly</i> recommended.
 * 
 * @author Toberumono
 */
public class SimpleFileManager extends AbstractFileManager {
	
	public SimpleFileManager() throws IOException {
		super();
	}
	
	public SimpleFileManager(FileSystem fileSystem) throws IOException {
		super(fileSystem);
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
