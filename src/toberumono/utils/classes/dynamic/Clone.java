package toberumono.utils.classes.dynamic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark fields that should be cloned via a copy constructor when cloning the {@link DynamicCloner} instance it is in.<br>
 * All fields marked with this annotation <i>must</i> have <i>either</i> a copy constructor (a publicly-visible constructor that takes a single value of the type of the
 * object being cloned) or implement {@link Cloneable} and have a publicly visible {@code clone} method.
 * 
 * @author Toberumono
 * @see Property
 * @see Copy
 * @see DynamicCloner
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Clone {}
