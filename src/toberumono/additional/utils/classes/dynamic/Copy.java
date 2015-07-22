package toberumono.additional.utils.classes.dynamic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark fields that should be copied without cloning it when cloning the {@link DynamicCloner} instance it is in.<br>
 * All fields marked with this annotation <i>must</i> be declared as public.
 * 
 * @author Joshua Lipstone
 * @see Property
 * @see Clone
 * @see DynamicCloner
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Copy {}
