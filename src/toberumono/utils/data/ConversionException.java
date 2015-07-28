package toberumono.utils.data;

/**
 * Indicates that an error occurred during a conversion operation.
 * 
 * @author Toberumono
 */
public class ConversionException extends RuntimeException {
	
	/**
	 * Basic constructor, used for wrapping causes
	 * 
	 * @param cause
	 *            the cause to wrap
	 */
	public ConversionException(Throwable cause) {
		super(cause);
	}
}
