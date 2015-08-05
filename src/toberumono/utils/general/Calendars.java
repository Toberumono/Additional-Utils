package toberumono.utils.general;

import java.util.Calendar;

/**
 * A static class containing a few helper methods for working with {@link Calendar Calendars}.
 * 
 * @author Toberumono
 */
public class Calendars {
	
	private Calendars() {/* This is a static class. */}
	
	/**
	 * Gets the last two digits of the year.
	 * 
	 * @param cal
	 *            the {@link Calendar} from which to get the year
	 * @return the last two digits of the year (e.g. 2015 -> 15)
	 */
	public static int getShortYear(Calendar cal) {
		String year = String.valueOf(cal.get(Calendar.YEAR));
		return Integer.parseInt(year.substring(year.length() - 2));
	}
}
