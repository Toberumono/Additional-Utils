package toberumono.additional.utils.classes.dynamic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark fields that should be should be loaded from and saved to disk.
 * 
 * @author Joshua Lipstone
 * @see Copy
 * @see Clone
 * @see DynamicCloner
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
	/**
	 * Only used for String[]-based enums
	 * 
	 * @return the possible values for the String[]-based enum. This must not be set for any other value type.
	 */
	String[] selection() default {};
}
