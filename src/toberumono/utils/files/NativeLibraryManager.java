package toberumono.utils.files;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import toberumono.utils.security.NativeLibraryPermission;

public final class NativeLibraryManager {
	private static final String extension;
	private static final Set<Path> sources;
	private static final Set<String> loaded;
	private static final Path unpacked;
	private static boolean unpackedIsTemp;
	
	static {
		sources = new HashSet<>();
		loaded = new HashSet<>();
		synchronized (sources) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.indexOf("win") > -1)
				extension = ".dll";
			else if (os.indexOf("mac") > -1)
				extension = ".jnilib";
			else
				//Because if the OS cannot be identified, it is probably a version of Linux/Unix.
				extension = ".so";
			unpacked = getUnpackDirectory();
			if (unpackedIsTemp) {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						try {
							Files.walkFileTree(unpacked, new RecursiveEraser());
						}
						catch (IOException e) {
							
						}
					}
				});
			}
			loadDefaults();
		}
	}
	
	private static final Path getUnpackDirectory() {
		try {
			unpackedIsTemp = true;
			return Files.createTempDirectory("natives"); //This is the best option
		}
		catch (IOException e) {
			unpackedIsTemp = false;
			try {
				Path temp = Paths.get(NativeLibraryManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().resolve("natives_failsafe"); //This is an okay option
				if (Files.isReadable(temp) && Files.isDirectory(temp))
					return temp;
			}
			catch (URISyntaxException e1) {}
		}
		return Paths.get("natives_failsafe").toAbsolutePath(); //This is a last resort, and probably won't work, but we need a failsafe
	}
	
	private static final void loadDefaults() {
		for (String path : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			Path p = Paths.get(path);
			String fname = p.getFileName().toString();
			if (Files.isDirectory(p)) {
				addSource(p.resolve("native libraries"));
			}
			else if (fname.endsWith(".jar") || fname.endsWith(".zip")) {
				try (FileSystem jar = FileSystems.newFileSystem(p, null)) {
					for (Path root : jar.getRootDirectories())
						addSource(root.resolve("native libraries"));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/* ************************************************* */
	/*                BEGIN ACCESSOR CODE                */
	/* ************************************************* */
	
	/**
	 * Adds a {@link Path} to a <i>directory</i> in which to look for native libraries. This can be in any {@link FileSystem}
	 * that files can be copied out of.
	 * 
	 * @param source
	 *            a {@link Path} to the source to be added
	 * @return {@code true} if the source is in set of locations in which the {@link NativeLibraryManager} searches at the
	 *         end of the method, otherwise {@code false}
	 */
	public static final boolean addSource(Path source) {
		synchronized (sources) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(new NativeLibraryPermission("sources.add"));
			if (Files.isDirectory(source) && Files.isReadable(source)) {
				sources.add(source);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Removes a source from the set of locations in which the {@link NativeLibraryManager} searches.
	 * 
	 * @param source
	 *            a {@link Path} representing the source to be removed
	 * @return whether the source was removed (a return of {@code false} does not necessarily mean that the source is in the
	 *         set - it just means that it wasn't removed)
	 */
	public static final boolean removeSource(Path source) {
		synchronized (sources) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(new NativeLibraryPermission("sources.remove"));
			return sources.remove(source);
		}
	}
	
	/**
	 * @param source
	 *            a {@link Path} representing the source to be tested for
	 * @return {@code true} if the {@link NativeLibraryManager} is searching in <tt>source</tt> when attempting to load
	 *         native libraries
	 */
	public static final boolean isUsingSource(Path source) {
		synchronized (sources) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(new NativeLibraryPermission("sources.using"));
			return sources.contains(source);
		}
	}
	
	/**
	 * @return an <i>unmodifiable</i> {@link Set} backed by the {@link NativeLibraryManager} of the {@link Path Paths}
	 *         representing the sources in which the {@link NativeLibraryManager} searches for libraries
	 */
	public static final Set<Path> getSources() {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new NativeLibraryPermission("sources.view"));
		return Collections.unmodifiableSet(sources);
	}
	
	/**
	 * Attempts to load the given library. This method first attempts to load the library via a call to
	 * {@link System#load(String)} or {@link System#loadLibrary(String)} as appropriate before searching for the library in
	 * its sources.
	 * 
	 * @param name
	 *            the name of the library to load, the absolute path to it, or the path to it relative to the source that
	 *            contains it
	 * @return {@code true} if the library was loaded
	 * @see System#load(String)
	 * @see System#loadLibrary(String)
	 */
	public static final boolean loadLibrary(String name) {
		synchronized (sources) {
			SecurityManager security = System.getSecurityManager();
			if (security != null) //No need to test the link permission - that will be implicitly tested by System.loadLibrary
				security.checkPermission(new NativeLibraryPermission("libraries.load"));
			try {
				if (name.indexOf((int) File.separatorChar) != -1)
					System.load(name);
				else
					System.loadLibrary(name);
				loaded.add(name);
				return true;
			}
			catch (UnsatisfiedLinkError e) {
				String lib = name + extension;
				for (Path source : sources) {
					Path p = source.resolve(lib);
					if (!Files.isRegularFile(p) || !Files.isReadable(p)) {
						p = source.resolve("lib" + lib); //Load the library with the added lib prefix - this is to support
						if (!Files.isRegularFile(p) || !Files.isReadable(p))
							continue;
					}
					//At this point, p is a valid path to an existing file.
					try {
						Path l = unpackLibrary(p, source.relativize(p));
						System.load(l.toAbsolutePath().toString());
						loaded.add(name);
						return true;
					}
					catch (IOException e2) {/* Continue trying other sources */}
				}
				throw new LinkageError(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Tests whether the library with the given name is has been loaded.
	 * 
	 * @param name
	 *            the name of the library
	 * @return {@code true} if the library has been loaded
	 */
	public static final boolean isLibraryLoaded(String name) {
		synchronized (sources) {
			SecurityManager security = System.getSecurityManager();
			if (security != null)
				security.checkPermission(new NativeLibraryPermission("libraries.using"));
			if (loaded.contains(name))
				return true;
			String lib = name + extension;
			if (Files.isRegularFile(unpacked.resolve(lib)) || Files.isRegularFile(unpacked.resolve("lib" + lib)))
				return true;
			return false;
		}
	}
	
	/**
	 * @return an <i>unmodifiable</i> {@link Set} backed by the {@link NativeLibraryManager} of the names of the currently
	 *         loaded libraries
	 */
	public static final Set<String> getLoadedLibraries() {
		SecurityManager security = System.getSecurityManager();
		if (security != null)
			security.checkPermission(new NativeLibraryPermission("libraries.view"));
		return Collections.unmodifiableSet(loaded);
	}
	
	/**
	 * This technically just copies the library file from one location to it's corresponding location in the target
	 * directory.
	 * 
	 * @param source
	 *            the absolute path to the source location of the library
	 * @param lib
	 *            the path to the library relative to the source directory that contains it
	 * @return the path to the library in the <tt>unpacked</tt> directory
	 * @throws IOException
	 *             if the location could not be created or the library could not be copied to it.
	 */
	private static final Path unpackLibrary(Path source, Path lib) throws IOException {
		Path target = unpacked.resolve(lib);
		target = Files.createDirectories(target);
		return Files.copy(lib, target, StandardCopyOption.COPY_ATTRIBUTES);
	}
}
