package toberumono.utils.exceptions;

/**
 * An exception to be thrown when a value is out of range.
 * 
 * @author Toberumono
 */
public class OutOfRangeException extends RuntimeException {
	/**
	 * Indicates that the {@code cap} value is the largest valid value in the range
	 */
	public static final Direction LESS_THAN = Direction.LESS_THAN;
	/**
	 * Indicates that the {@code cap} value is the smallest valid value in the range
	 */
	public static final Direction GREATER_THAN = Direction.GREATER_THAN;
	
	private final Object min, max, out;
	
	private enum Direction {
		LESS_THAN, GREATER_THAN
	};
	
	/**
	 * Constructs an {@link OutOfRangeException} with the given details.
	 * 
	 * @param min
	 *            the minimum value of the range
	 * @param max
	 *            the maximum value of the range
	 * @param out
	 *            the value that was outside of the range
	 */
	public OutOfRangeException(Object min, Object max, Object out) {
		super(String.valueOf(out) + " is out of range (" + String.valueOf(min) + ", " + String.valueOf(max) + ")");
		this.min = min;
		this.max = max;
		this.out = out;
	}
	
	/**
	 * Constructs an {@link OutOfRangeException} with the given details.
	 * 
	 * @param cap
	 *            the non-infinite bound of the range (i.e. with {@code direction} = {@link #LESS_THAN}, cap would be the
	 *            maximum value)
	 * @param direction
	 *            the side of cap that is allowable within the range
	 * @param out
	 *            the value that was outside of the range
	 */
	public OutOfRangeException(Object cap, Direction direction, Object out) {
		super(String.valueOf(out) + (direction == LESS_THAN ? " is greater than " : " is less than ") + String.valueOf(cap));
		if (direction == LESS_THAN) {
			min = null;
			max = cap;
		}
		else {
			min = cap;
			max = null;
		}
		this.out = out;
	}
	
	/**
	 * Constructs an {@link OutOfRangeException} with the given details. This is a convenience method for
	 * {@link #OutOfRangeException(Object, Direction, Object)}.
	 * 
	 * @param cap
	 *            the non-infinite bound of the range
	 * @param out
	 *            the value that was outside of the range
	 */
	public <T extends Comparable<? super T>> OutOfRangeException(T cap, T out) {
		this(cap, out.compareTo(cap) > 0 ? LESS_THAN : GREATER_THAN, out);
	}
	
	/**
	 * @return the minimum value of the range, if there is no minimum value, it is {@code null}
	 */
	public Object getMin() {
		return min;
	}
	
	/**
	 * @return the maximum value of the range, if there is no maximum value, it is {@code null}
	 */
	public Object getMax() {
		return max;
	}
	
	/**
	 * @return the value that was outside of the range
	 */
	public Object getOut() {
		return out;
	}
}
