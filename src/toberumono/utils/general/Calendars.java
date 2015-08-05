package toberumono.utils.general;

import java.util.Calendar;
import java.util.Formatter;
import java.util.regex.Pattern;

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
	 * @return the last two digits of the year (e.g. 2015 --&gt; 15)
	 */
	public static int getShortYear(Calendar cal) {
		String year = String.valueOf(cal.get(Calendar.YEAR));
		return Integer.parseInt(year.substring(year.length() - 2));
	}
	
	/**
	 * Uses the {@link Formatter Formatter's}
	 * <a href="http://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#dt">Date/Time syntax</a> as specified in
	 * the JavaDoc to replace date/time markers in the given {@link String} with the values from the given {@link Calendar}.
	 * Quick reference of helpful flags:
	 * <ul>
	 * <li>Y --&gt; year with at least four digits</li>
	 * <li>y --&gt; last two digits of the year</li>
	 * <li>m --&gt; month in the year with two digits</li>
	 * <li>d --&gt; day within the month with two digits</li>
	 * <li>H --&gt; 24-hour time in the day with two digits</li>
	 * <li>M --&gt; minutes in the hour with two digits</li>
	 * <li>S --&gt; seconds in the minute with two digits</li>
	 * </ul>
	 * 
	 * @param input
	 *            the {@link String} containing calendar markers
	 * @param cal
	 *            the {@link Calendar} containing the values with which to replace the calendar markers
	 * @return the {@link String} with the calendar markers replaced with the appropriate values
	 */
	public static String writeCalendarToString(String input, Calendar cal) {
		return String.format(Pattern.compile("%([\\Q-#+ 0,(\\E]*?[tT])").matcher(input).replaceAll("%\\$1$1"), cal);
	}
}
