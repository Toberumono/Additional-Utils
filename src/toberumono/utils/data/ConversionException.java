package toberumono.utils.data;

/**
 * Indicates that an error occurred during a conversion operation.
 * 
 * @author Joshua Lipstone
 */
public class ConversionException extends RuntimeException {
	
	public ConversionException(Throwable cause) {
		super(cause);
	}
}
