package toberumono.utils.classes.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Consumer;

//TODO incomplete, but kinda boring to complete right now.
public abstract class DynamicCloner implements Cloneable {
	
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
