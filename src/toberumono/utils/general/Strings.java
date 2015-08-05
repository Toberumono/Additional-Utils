package toberumono.utils.general;

import java.io.UnsupportedEncodingException;

/**
 * A static class containing a few helper methods for working with {@link String Strings}.
 * 
 * @author Toberumono
 */
public class Strings {
	
	private Strings() {/* This is a static class. */}
	
	/**
	 * Converts all of the Java special characters (['tbnrf"\]) into their escaped form, mostly for printing {@link String
	 * Strings} to files.
	 * 
	 * @param str
	 *            the {@link String} to escape
	 * @return the escaped form of <tt>str</tt>
	 * @see #unescape(String)
	 */
	public static final String escape(String str) {
		StringBuilder sb = new StringBuilder(str.length());
		str.chars().forEach(c -> {
			if (c == '\t')
				sb.append("\\t");
				else if (c == '\b')
					sb.append("\\b");
				else if (c == '\n')
					sb.append("\\n");
				else if (c == '\r')
					sb.append("\\r");
				else if (c == '\f')
					sb.append("\\f");
				else if (c == '\'')
					sb.append("\\'");
				else if (c == '\"')
					sb.append("\\\"");
				else if (c == '\\')
					sb.append("\\\\");
				else
					sb.append((char) c);
			});
		return sb.toString();
	}
	
	/**
	 * Unescapes an escaped {@link String} such that {@code str.equals(unescape(escape(str)))} returns true.
	 * 
	 * @param str
	 *            the escaped {@link String} to unescape
	 * @return the original (unescaped) form of <tt>str</tt>
	 * @throws UnsupportedEncodingException
	 *             if there is an invalid escape sequence in <tt>str</tt>
	 * @see #escape(String)
	 */
	public static final String unescape(String str) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(str.length());
		try {
			char[] s = str.toCharArray();
			for (int i = 0; i < s.length; i++)
				if (s[i] == '\\') {
					char c = s[++i];
					if (c == 't')
						sb.append('\t');
					else if (c == 'b')
						sb.append("\b");
					else if (c == 'n')
						sb.append("\n");
					else if (c == 'r')
						sb.append("\r");
					else if (c == 'f')
						sb.append("\f");
					else if (c == '\'')
						sb.append("'");
					else if (c == '"')
						sb.append("\"");
					else if (c == '\\')
						sb.append("\\");
					else
						throw new UnsupportedEncodingException("\\" + s[i] + " is not a valid escape sequence.");
				}
				else
					sb.append(s[i]);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new UnsupportedEncodingException("String cannot end with a \\");
		}
		return sb.toString().trim();
	}
}
