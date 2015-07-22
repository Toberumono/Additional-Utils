package toberumono.additional.utils.files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;

import toberumono.additional.utils.functions.IOExceptedBiFunction;

public class TransferringFileWalker implements FileVisitor<Path> {
	private final Collection<Path> special;
	private final Path desitination;
	private final boolean test;
	private static final IOExceptedBiFunction<Path, BasicFileAttributes, FileVisitResult> firstDefault = (t, u) -> FileVisitResult.CONTINUE;
	private static final IOExceptedBiFunction<Path, IOException, FileVisitResult> secondDefault = (t, u) -> FileVisitResult.CONTINUE;
	private final IOExceptedBiFunction<Path, BasicFileAttributes, FileVisitResult> file, preDir;
	private final IOExceptedBiFunction<Path, IOException, FileVisitResult> fileFailed, postDir;
	
	/**
	 * Constructs a new {@link TransferringFileWalker}.<br>
	 * NOTE: Other than <tt>destination</tt>, <i>any</i> of these arguments can be set to {@code null} in which case a
	 * default value will be used.
	 * 
	 * @param destination
	 *            a {@link Path} to the root directory into which the directory structure found while walking will be
	 *            duplicated
	 * @param special
	 *            a {@link Collection} of relative (or absolute as the case may be) {@link Path Paths} denoting files that
	 *            should be excluded
	 * @param onFile
	 *            an action to perform on each found file (that is not in the excluded list)
	 * @param onPreDirectory
	 *            an action to perform on each found directory (that is not in the excluded list)
	 * @param onPostDirectory
	 *            an action to perform after each visited directory
	 * @param onFileFailed
	 *            an action to perform when a file visit fails
	 * @param exclude
	 *            If <tt>true</tt>, then the files or directories that match up with the {@link Path Paths} in
	 *            <tt>special</tt> will be excluded. If <tt>false</tt> then <i>only</i> files and folders that match up with
	 *            the {@link Path Paths} in <tt>special</tt> will be included.
	 */
	public TransferringFileWalker(Path destination, Collection<Path> special, IOExceptedBiFunction<Path, BasicFileAttributes, FileVisitResult> onFile,
			IOExceptedBiFunction<Path, BasicFileAttributes, FileVisitResult> onPreDirectory,
			IOExceptedBiFunction<Path, IOException, FileVisitResult> onPostDirectory, IOExceptedBiFunction<Path, IOException, FileVisitResult> onFileFailed, boolean exclude) {
		if (destination == null)
			throw new NullPointerException("destination cannot be null");
		this.desitination = destination;
		this.test = !exclude; //It's a lot easier to test against include, but exclusion is easier to explain for the constructor parameter
		this.special = special == null ? Collections.EMPTY_LIST : special;
		this.file = onFile == null ? firstDefault : onFile;
		this.preDir = onPreDirectory == null ? firstDefault : onPreDirectory;
		this.postDir = onPostDirectory == null ? secondDefault : onPostDirectory;
		this.fileFailed = onFileFailed == null ? secondDefault : onFileFailed;
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (test ^ matches(dir))
			return FileVisitResult.SKIP_SUBTREE;
		return this.preDir.apply(dir, attrs);
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (test ^ matches(file))
			return FileVisitResult.CONTINUE;
		return this.file.apply(file, attrs);
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return fileFailed.apply(file, exc);
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return postDir.apply(dir, exc);
	}
	
	private boolean matches(Path test) {
		for (Path p : special)
			if (test.endsWith(p))
				return true;
		return false;
	}
	
}
