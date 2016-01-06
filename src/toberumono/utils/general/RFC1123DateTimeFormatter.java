package toberumono.utils.general;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.time.temporal.ChronoField.*;

/**
 * A more fully implemented {@link DateTimeFormatter} for the format defined in RFC 1123.
 * 
 * @author Toberumono
 */
public final class RFC1123DateTimeFormatter {
	private static final Map<Long, String> dow = new HashMap<>(), moy = new HashMap<>();
	private static final DateTimeFormatter localless;
	static {
		//Days
		dow.put(1L, "Mon");
		dow.put(2L, "Tue");
		dow.put(3L, "Wed");
		dow.put(4L, "Thu");
		dow.put(5L, "Fri");
		dow.put(6L, "Sat");
		dow.put(7L, "Sun");
		//Months
		moy.put(1L, "Jan");
		moy.put(2L, "Feb");
		moy.put(3L, "Mar");
		moy.put(4L, "Apr");
		moy.put(5L, "May");
		moy.put(6L, "Jun");
		moy.put(7L, "Jul");
		moy.put(8L, "Aug");
		moy.put(9L, "Sep");
		moy.put(10L, "Oct");
		moy.put(11L, "Nov");
		moy.put(12L, "Dec");
		localless = makeBuilder().toFormatter();
	}
	
	private RFC1123DateTimeFormatter() {/* This is a static class */}
	
	/**
	 * @return the {@link DateTimeFormatter} with the default {@link Locale}
	 */
	public static DateTimeFormatter get() {
		return localless;
	}
	
	/**
	 * @param locale
	 *            the {@link Locale} to be used by the formatter
	 * @return a copy of the {@link DateTimeFormatter} that uses the given {@link Locale}
	 */
	public static DateTimeFormatter get(Locale locale) {
		return get().withLocale(locale);
	}
	
	private static final DateTimeFormatterBuilder makeBuilder() {
		return new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseLenient()
				.optionalStart()
				.appendText(DAY_OF_WEEK, dow)
				.appendLiteral(", ")
				.optionalEnd()
				.appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
				.appendLiteral(' ')
				.appendText(MONTH_OF_YEAR, moy)
				.appendLiteral(' ')
				.appendValue(YEAR, 4) // 2 digit year not handled
				.appendLiteral(' ')
				.appendValue(HOUR_OF_DAY, 2)
				.appendLiteral(':')
				.appendValue(MINUTE_OF_HOUR, 2)
				.optionalStart()
				.appendLiteral(':')
				.appendValue(SECOND_OF_MINUTE, 2)
				.optionalEnd()
				.appendLiteral(' ')
				.appendZoneText(TextStyle.SHORT);
	}
}
