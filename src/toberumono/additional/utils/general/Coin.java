package toberumono.additional.utils.general;

import java.util.Random;

/**
 * Extends {@link Random} with a few convenience methods.
 * 
 * @author Toberumono
 */
public class Coin extends Random {
	/**
	 * An instance of {@link Coin}
	 */
	public static final Coin coin = new Coin();
	
	/**
	 * Emulates flipping a fair coin (50/50 probability)
	 * 
	 * @return true if the next double returned is &lt; 0.5
	 * @see #flip(double)
	 */
	public boolean flip() {
		return flip(0.5);
	}
	
	/**
	 * Emulates flipping a coin with a probability of landing on heads equal to <tt>bound</tt>
	 * 
	 * @param bound
	 *            the probability of the function returning true
	 * @return true if the next double returned is &lt; <tt>bound</tt>
	 */
	public boolean flip(double bound) {
		return super.nextDouble() < bound;
	}
	
	/**
	 * Returns the next int with the given offset and bound
	 * 
	 * @param offset
	 *            the smallest returnable value
	 * @param bound
	 *            the length of the range (exclusive)
	 * @return an int in the range, [offset, offset + bound)
	 * @see #nextIntInRange(int, int)
	 */
	public int nextInt(int offset, int bound) {
		return offset + super.nextInt(bound);
	}
	
	/**
	 * Returns the next int in the given range.
	 * 
	 * @param min
	 *            the lower bound of the range
	 * @param max
	 *            the upper bound of the range
	 * @return an int in the range, [min, max)
	 * @see #nextInt(int, int)
	 */
	public int nextIntInRange(int min, int max) {
		return nextInt(min, max - min);
	}
}
