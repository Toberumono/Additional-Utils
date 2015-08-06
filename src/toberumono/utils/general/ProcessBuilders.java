package toberumono.utils.general;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

/**
 * A static class containing methods that assist in working with {@link ProcessBuilder ProcessBuilders}.
 * 
 * @author Toberumono
 */
public class ProcessBuilders {
	private ProcessBuilders() {/* This is a static class. */}
	
	/**
	 * Starts a {@link Process} using the given {@link ProcessBuilder} and command and waits for its completion.
	 * 
	 * @param pb
	 *            the {@link ProcessBuilder} with which to execute the command
	 * @param command
	 *            the command to execute
	 * @return the exit value of the {@link Process} that started
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws InterruptedException
	 *             if the {@link Process} was interrupted
	 */
	public static int runPB(ProcessBuilder pb, String... command) throws IOException, InterruptedException {
		pb.command(command);
		Process p = pb.start();
		return p.waitFor();
	}
	
	/**
	 * Performs the common steps for setting up the {@link ProcessBuilder ProcessBuilders} used in this system. (Basically
	 * just avoids some copy and paste)
	 * 
	 * @param directory
	 *            the working directory for the {@link ProcessBuilder}
	 * @return a {@link ProcessBuilder} with {@link Redirect#INHERIT} redirections and the given working directory
	 */
	public static ProcessBuilder makePB(File directory) {
		ProcessBuilder pb = new ProcessBuilder();
		pb.redirectError(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		pb.directory(directory);
		return pb;
	}
}
