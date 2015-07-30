package toberumono.utils.files;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import toberumono.utils.files.TransferFileWalker.TransferAction;

/**
 * The four most common actions that are used with {@link TransferFileWalker}.
 * 
 * @author Toberumono
 * @see TransferFileWalker
 */
public enum BasicTransferActions implements TransferAction {
	/**
	 * A {@link TransferAction} that copies a file or empty folder, overwriting existing files if necessary.
	 */
	COPY(Files::copy),
	
	/**
	 * A {@link TransferAction} that moves a file or empty folder, overwriting existing files if necessary.
	 */
	MOVE(Files::move),
	
	/**
	 * A {@link TransferAction} that creates a hard link, overwriting existing files if necessary.
	 */
	LINK((s, t) -> Files.createLink(t, s)),
	
	/**
	 * A {@link TransferAction} that creates a symlink, overwriting existing files if necessary.
	 */
	SYMLINK((s, t) -> Files.createSymbolicLink(t, s));
	private final TransferAction action;
	
	BasicTransferActions(TransferAction action) {
		this.action = action;
	}
	
	@Override
	public Path apply(Path s, Path t) throws IOException {
		try {
			return action.apply(t, s.toRealPath());
		}
		catch (FileAlreadyExistsException e) {
			if (Files.isSameFile(s, t))
				return t;
			Files.delete(t);
			return action.apply(t, s.toRealPath());
		}
	}
}
