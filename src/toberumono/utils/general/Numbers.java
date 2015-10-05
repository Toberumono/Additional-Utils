package toberumono.utils.general;

/**
 * A static class containing a few helper methods for working with {@link Number Numbers}.
 * 
 * @author Toberumono
 */
public class Numbers {
	
	private Numbers() {/* This is a static class. */}
	
	/**
	 * While this might not be the best name, it is close to the meaning and a bit of a pun, so, it'll stay for now.<br>
	 * The semifloor is the largest multiple (closest to positive infinity) of {@code floor(base * fraction)} &le; x.<br>
	 * e.g. {@code semifloor(24, 0.25, 11) = 6} and {@code semifloor(24, 0.5, 11) = 0}<br>
	 * Special Cases:
	 * <ul>
	 * <li>x equals an infinity --&lt; x</li>
	 * </ul>
	 * 
	 * @param base
	 *            the base
	 * @param fraction
	 *            the multiplier
	 * @param x
	 *            the upper bound
	 * @return the semifloor of base, fraction, and x
	 */
	public static double semifloor(int base, double fraction, double x) {
		if (x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY)
			return x;
		return x - x % (base * fraction);
	}
	
}
