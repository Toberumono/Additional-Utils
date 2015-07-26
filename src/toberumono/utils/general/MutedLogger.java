package toberumono.utils.general;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class MutedLogger {
	
	private MutedLogger() {/* This is a static class. */}
	
	/**
	 * <i>Note: this method creates a new {@link Logger} each time it is called.</i><br>
	 * This safeguards against invalid modifications propagated to other objects that are using a muted {@link Logger}.
	 * 
	 * @return a {@link Logger} that has been muted
	 */
	public static final Logger getMutedLogger() {
		Logger mutedLogger = Logger.getAnonymousLogger();
		mutedLogger.setUseParentHandlers(false);
		mutedLogger.setLevel(Level.OFF);
		return mutedLogger;
	}
}
