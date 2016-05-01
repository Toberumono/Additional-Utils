package toberumono.utils.general;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import toberumono.utils.exceptions.OutOfRangeException;

/**
 * A static class containing a few helper methods for working with {@link Number Numbers}.
 * 
 * @author Toberumono
 */
public class Numbers {
	/**
	 * A {@link BigDecimal} representing the number 2
	 */
	public static final BigDecimal TWO = new BigDecimal(2);
	
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
	
	/**
	 * Implementation of bucketRounding for {@code int} values.<br>
	 * <b>Note:</b> The values in {@code buckets} must be sorted in ascending order (the item at index 0 must be the
	 * smallest)
	 * 
	 * @param value
	 *            the value to be rounded
	 * @param rm
	 *            the {@link RoundingMode} to be used; if {@code rm} equals {@link RoundingMode#UNNECESSARY}, {@code value}
	 *            is returned immediately
	 * @param buckets
	 *            the values to which {@code value} can be rounded. Rounding goes to the appropriate bucket value as
	 *            specified by the {@link RoundingMode} passed to {@code rm}
	 * @return {@code value} rounded to the appropriate bucket value as specified by the {@link RoundingMode}
	 * @throws OutOfRangeException
	 *             if there is not a valid bucket value to which {@code value} can be rounded according to the
	 *             {@link RoundingMode} passed to {@code rm}
	 */
	public static int bucketRounding(int value, RoundingMode rm, int... buckets) {
		if (rm == RoundingMode.UNNECESSARY)
			return value;
		int bucket = SupportFunctions.findBucket(value, buckets); //bucket cannot be less than 1
		if (value == buckets[bucket]) //Fast return if value was equal to one of the bucket values
			return buckets[bucket]; //For pointer consistency
		switch (rm) {
			case CEILING:
				if (bucket == buckets.length) //value is greater than the largest bucket 
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[bucket];
			case DOWN:
				if (value < 0) {
					if (bucket == buckets.length) //Negative and greater than the largest bucket
						throw new OutOfRangeException(buckets[bucket - 1], value);
				}
				else if (bucket == 0) //Positive and less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[value < 0 ? bucket : bucket - 1]; //Rounds towards 0 for negative values
			case FLOOR:
				if (bucket == 0) //value is less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[bucket - 1];
			case HALF_DOWN:
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value > SupportFunctions.average(buckets[bucket - 1], buckets[bucket]) ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case HALF_EVEN:
				if (bucket == 0)
					return buckets[bucket];
				else if (bucket == buckets.length)
					return buckets[bucket - 1];
				double average = SupportFunctions.average(buckets[bucket - 1], buckets[bucket]);
				return buckets[value > average || (value == average && (value % 2) == 1) ? bucket : bucket - 1]; //If value == average and value is odd, round up
			case HALF_UP:
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value >= SupportFunctions.average(buckets[bucket - 1], buckets[bucket]) ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case UP:
				if (value < 0) {
					if (bucket == 0) //Negative and less than the smallest bucket
						throw new OutOfRangeException(buckets[bucket], value);
				}
				else if (bucket == buckets.length) //Positive and greater than the largest bucket
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[value < 0 ? bucket - 1 : bucket]; //Rounds away from 0 for negative values
			default: //This cannot be reached with valid RoundingMethods
				throw new UnsupportedOperationException(String.valueOf(rm) + " is not a valid rounding method");
		}
	}
	
	/**
	 * Implementation of bucketRounding for {@code double} values.<br>
	 * <b>Note:</b> The values in {@code buckets} must be sorted in ascending order (the item at index 0 must be the
	 * smallest)
	 * 
	 * @param value
	 *            the value to be rounded
	 * @param rm
	 *            the {@link RoundingMode} to be used; if {@code rm} equals {@link RoundingMode#UNNECESSARY}, {@code value}
	 *            is returned immediately
	 * @param buckets
	 *            the values to which {@code value} can be rounded. Rounding goes to the appropriate bucket value as
	 *            specified by the {@link RoundingMode} passed to {@code rm}
	 * @return {@code value} rounded to the appropriate bucket value as specified by the {@link RoundingMode}
	 * @throws OutOfRangeException
	 *             if there is not a valid bucket value to which {@code value} can be rounded according to the
	 *             {@link RoundingMode} passed to {@code rm}
	 */
	public static double bucketRounding(double value, RoundingMode rm, double... buckets) {
		if (rm == RoundingMode.UNNECESSARY)
			return value;
		int bucket = SupportFunctions.findBucket(value, buckets); //bucket cannot be less than 1
		if (value == buckets[bucket]) //Fast return if value was equal to one of the bucket values
			return buckets[bucket]; //For pointer consistency
		switch (rm) {
			case CEILING:
				if (bucket == buckets.length) //value is greater than the largest bucket 
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[bucket];
			case DOWN:
				if (value < 0) {
					if (bucket == buckets.length) //Negative and greater than the largest bucket
						throw new OutOfRangeException(buckets[bucket - 1], value);
				}
				else if (bucket == 0) //Positive and less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[value < 0 ? bucket : bucket - 1]; //Rounds towards 0 for negative values
			case FLOOR:
				if (bucket == 0) //value is less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[bucket - 1];
			case HALF_DOWN:
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value > SupportFunctions.average(buckets[bucket - 1], buckets[bucket]) ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case HALF_EVEN:
				if (bucket == 0)
					return buckets[bucket];
				else if (bucket == buckets.length)
					return buckets[bucket - 1];
				double average = SupportFunctions.average(buckets[bucket - 1], buckets[bucket]);
				return buckets[value > average || (value == average && (value % 2) == 1) ? bucket : bucket - 1]; //If value == average and value is odd, round up
			case HALF_UP:
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value >= SupportFunctions.average(buckets[bucket - 1], buckets[bucket]) ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case UP:
				if (value < 0) {
					if (bucket == 0) //Negative and less than the smallest bucket
						throw new OutOfRangeException(buckets[bucket], value);
				}
				else if (bucket == buckets.length) //Positive and greater than the largest bucket
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[value < 0 ? bucket - 1 : bucket]; //Rounds away from 0 for negative values
			default: //This cannot be reached with valid RoundingMethods
				throw new UnsupportedOperationException(String.valueOf(rm) + " is not a valid rounding method");
		}
	}
	
	/**
	 * Implementation of bucketRounding for {@link BigDecimal} values.<br>
	 * This is a convenience method for
	 * {@link #bucketRounding(Comparable, RoundingMode, Predicate, BiFunction, Predicate, Comparable...)} that works with all
	 * {@link RoundingMode RoundingModes}.<br>
	 * <b>Note:</b> The values in {@code buckets} must be sorted in ascending order (the item at index 0 must be the
	 * smallest)
	 * 
	 * @param value
	 *            the value to be rounded
	 * @param rm
	 *            the {@link RoundingMode} to be used; if {@code rm} equals {@link RoundingMode#UNNECESSARY}, {@code value}
	 *            is returned immediately
	 * @param buckets
	 *            the values to which {@code value} can be rounded. Rounding goes to the appropriate bucket value as
	 *            specified by the {@link RoundingMode} passed to {@code rm}
	 * @return {@code value} rounded to the appropriate bucket value as specified by the {@link RoundingMode}
	 * @throws OutOfRangeException
	 *             if there is not a valid bucket value to which {@code value} can be rounded according to the
	 *             {@link RoundingMode} passed to {@code rm}
	 */
	public static BigDecimal bucketRounding(BigDecimal value, RoundingMode rm, BigDecimal... buckets) {
		return bucketRounding(value, rm, SupportFunctions.isNegative, SupportFunctions::average, SupportFunctions.isOdd, buckets);
	}
	
	/**
	 * An implementation of bucketRounding for arbitrary {@link Comparable} values.<br>
	 * This is a convenience method for
	 * {@link #bucketRounding(Comparable, RoundingMode, Predicate, BiFunction, Predicate, Comparable...)
	 * bucketRounding(value, rm, null, null, null, buckets)}. Therefore, this method will throw a
	 * {@link NullPointerException} if {@code rm} equals {@link RoundingMode#UP}, {@link RoundingMode#DOWN},
	 * {@link RoundingMode#HALF_UP}, {@link RoundingMode#HALF_EVEN}, or {@link RoundingMode#HALF_DOWN}.<br>
	 * <b>Note:</b> The values in {@code buckets} must be sorted in ascending order (the item at index 0 must be the
	 * smallest)
	 * 
	 * @param value
	 *            the value to be rounded
	 * @param rm
	 *            the {@link RoundingMode} to be used; if {@code rm} equals {@link RoundingMode#UNNECESSARY}, {@code value}
	 *            is returned immediately
	 * @param buckets
	 *            the values to which {@code value} can be rounded. Rounding goes to the appropriate bucket value as
	 *            specified by the {@link RoundingMode} passed to {@code rm}
	 * @param <T>
	 *            the type of {@link Comparable} being rounded
	 * @return {@code value} rounded to the appropriate bucket value as specified by the {@link RoundingMode}
	 * @throws NullPointerException
	 *             if {@code rm} equals {@link RoundingMode#UP}, {@link RoundingMode#DOWN}, {@link RoundingMode#HALF_UP},
	 *             {@link RoundingMode#HALF_EVEN}, or {@link RoundingMode#HALF_DOWN}
	 * @throws OutOfRangeException
	 *             if there is not a valid bucket value to which {@code value} can be rounded according to the
	 *             {@link RoundingMode} passed to {@code rm}
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> T bucketRounding(T value, RoundingMode rm, T... buckets) {
		return bucketRounding(value, rm, null, null, null, buckets);
	}
	
	/**
	 * An implementation of bucketRounding for arbitrary comparable values.<br>
	 * <b>Note:</b> {@code isNegative}, {@code average}, and {@code isOdd} are not required by all {@link RoundingMode
	 * RoundingModes} and can be {@code null} if they are not required. See their individual parameter descriptions for which
	 * {@link RoundingMode RoundingModes} require them.<br>
	 * <b>Note:</b> The values in {@code buckets} must be sorted in ascending order (the item at index 0 must be the
	 * smallest)
	 * 
	 * @param value
	 *            the value to be rounded
	 * @param rm
	 *            the {@link RoundingMode} to be used; if {@code rm} equals {@link RoundingMode#UNNECESSARY}, {@code value}
	 *            is returned immediately
	 * @param isNegative
	 *            a {@link Predicate} that returns {@code true} iff {@code value} is less than the equivalent to 0<br>
	 *            <i>Required for:</i> {@link RoundingMode#DOWN DOWN} and {@link RoundingMode#UP UP}
	 * @param average
	 *            a {@link BiFunction} that returns the average (arithmetic mean) of two values.<br>
	 *            <i>Required for:</i> {@link RoundingMode#HALF_EVEN HALF_EVEN}, {@link RoundingMode#HALF_DOWN HALF_DOWN},
	 *            and {@link RoundingMode#HALF_UP HALF_UP}
	 * @param isOdd
	 *            a {@link Predicate} that returns {@code true} iff {@code value} is odd<br>
	 *            <i>Required for:</i> {@link RoundingMode#HALF_EVEN HALF_EVEN}
	 * @param buckets
	 *            the values to which {@code value} can be rounded. Rounding goes to the appropriate bucket value as
	 *            specified by the {@link RoundingMode} passed to {@code rm}.
	 * @param <T>
	 *            the type of {@link Comparable} being rounded
	 * @return {@code value} rounded to the appropriate bucket value as specified by the {@link RoundingMode}
	 * @throws NullPointerException
	 *             if {@code isNegative}, {@code average}, or {@code isOdd} is {@code null} and is required by the
	 *             {@link RoundingMode} passed to {@code rm}
	 * @throws OutOfRangeException
	 *             if there is not a valid bucket value to which {@code value} can be rounded according to the
	 *             {@link RoundingMode} passed to {@code rm}
	 */
	@SafeVarargs
	public static <T extends Comparable<? super T>> T bucketRounding(T value, RoundingMode rm, Predicate<T> isNegative, BiFunction<T, T, T> average, Predicate<T> isOdd, T... buckets) {
		if (rm == RoundingMode.UNNECESSARY)
			return value;
		int bucket = SupportFunctions.findBucket(value, buckets); //bucket cannot be less than 1
		if (value.compareTo(buckets[bucket]) == 0) //Fast return if value was equal to one of the bucket values
			return buckets[bucket]; //For pointer consistency
		switch (rm) {
			case CEILING:
				if (bucket == buckets.length) //value is greater than the largest bucket 
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[bucket];
			case DOWN:
				Objects.requireNonNull(isNegative, "isNegative cannot be null for RoundingMode.DOWN");
				if (isNegative.test(value)) {
					if (bucket == buckets.length) //Negative and greater than the largest bucket
						throw new OutOfRangeException(buckets[bucket - 1], value);
				}
				else if (bucket == 0) //Positive and less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[isNegative.test(value) ? bucket : bucket - 1]; //Rounds towards 0 for negative values
			case FLOOR:
				if (bucket == 0) //value is less than the smallest bucket
					throw new OutOfRangeException(buckets[bucket], value);
				return buckets[bucket - 1];
			case HALF_DOWN:
				Objects.requireNonNull(average, "average cannot be null for RoundingMode.HALF_DOWN");
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value.compareTo(average.apply(buckets[bucket - 1], buckets[bucket])) > 0 ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case HALF_EVEN:
				Objects.requireNonNull(average, "average cannot be null for RoundingMode.HALF_EVEN");
				Objects.requireNonNull(isOdd, "isOdd cannot be null for RoundingMode.HALF_EVEN");
				if (bucket == 0)
					return buckets[bucket]; //Always round up if value is less than the smallest bucket
				else if (bucket == buckets.length)
					return buckets[bucket - 1]; //Always round down if value is greater than the largest bucket
				T avg = average.apply(buckets[bucket - 1], buckets[bucket]);
				return buckets[value.compareTo(avg) > 0 || (value.compareTo(avg) == 0 && isOdd.test(value)) ? bucket : bucket - 1]; //If value == average and value is odd, round up
			case HALF_UP:
				Objects.requireNonNull(average, "average cannot be null for RoundingMode.HALF_UP");
				if (bucket == buckets.length) //Always round down if value is greater than the largest bucket
					return buckets[bucket - 1];
				return buckets[bucket == 0 || value.compareTo(average.apply(buckets[bucket - 1], buckets[bucket])) >= 0 ? bucket : bucket - 1]; //Always round up if value is less than the smallest bucket
			case UP:
				Objects.requireNonNull(isNegative, "isNegative cannot be null for RoundingMode.UP");
				if (isNegative.test(value)) {
					if (bucket == 0) //Negative and less than the smallest bucket
						throw new OutOfRangeException(buckets[bucket], value);
				}
				else if (bucket == buckets.length) //Positive and greater than the largest bucket
					throw new OutOfRangeException(buckets[bucket - 1], value);
				return buckets[isNegative.test(value) ? bucket - 1 : bucket]; //Rounds away from 0 for negative values
			default: //This cannot be reached with valid RoundingMethods
				throw new UnsupportedOperationException(String.valueOf(rm) + " is not a valid rounding method");
		}
	}
}

class SupportFunctions {
	static final Predicate<BigDecimal> isOdd = value -> value.divideToIntegralValue(Numbers.TWO).compareTo(BigDecimal.ZERO) != 0, isNegative = value -> value.signum() < 0;
	
	static final BigDecimal average(BigDecimal a, BigDecimal b) {
		return a.add(b.subtract(a).divide(Numbers.TWO));
	}
	
	static final double average(double a, double b) {
		return b + (a - b) / 2;
	}
	
	static int findBucket(int value, int[] buckets) {
		for (int min = 0, max = buckets.length, mid = min + (max - min) / 2;; mid = min + (max - min) / 2) {
			if (min >= max - 1)
				return value == buckets[min] ? min : max;
			else if (value < buckets[mid])
				max = mid;
			else if (value == buckets[mid])
				return mid;
			else
				min = mid + 1;
		}
	}
	
	static int findBucket(double value, double[] buckets) {
		for (int min = 0, max = buckets.length, mid = min + (max - min) / 2;; mid = min + (max - min) / 2) {
			if (min >= max - 1)
				return value == buckets[min] ? min : max;
			else if (value < buckets[mid])
				max = mid;
			else if (value == buckets[mid])
				return mid;
			else
				min = mid + 1;
		}
	}
	
	static final <T extends Comparable<? super T>> int findBucket(T value, T[] buckets) {
		for (int min = 0, max = buckets.length, mid = min + (max - min) / 2;; mid = min + (max - min) / 2) {
			if (min >= max - 1)
				return value.compareTo(buckets[min]) == 0 ? min : max;
			else if (value.compareTo(buckets[mid]) < 0)
				max = mid;
			else if (value.compareTo(buckets[mid]) == 0)
				return mid;
			else
				min = mid + 1;
		}
	}
}
