package toberumono.utils.classes.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Consumer;

//TODO incomplete, but kinda boring to complete right now.
/**
 * A class that dynamically generates a clone method for itself and anything that extends it.
 * 
 * @author Toberumono
 */
public abstract class DynamicCloner implements Cloneable {
	
	/**
	 * Performs <tt>action</tt> on every {@link Field} in the class that is annotated with at least one of the
	 * {@link Annotation Annotations} in <tt>filter</tt>.
	 * 
	 * @param action
	 *            the action to perform
	 * @param filter
	 *            the {@link Annotation Annotations} by which to filter
	 */
	public void forEachField(Consumer<Field> action, Annotation... filter) {
		for (Field f : this.getClass().getFields()) {
			f.setAccessible(true); //This is required in order for the field accesses to not throw IllegalAccessExceptions
			boolean skip = true;
			for (Annotation a : filter)
				if (f.getAnnotation(a.annotationType()) != null) {
					skip = false;
					break;
				}
			if (skip)
				continue;
			action.accept(f);
		}
	}
}
