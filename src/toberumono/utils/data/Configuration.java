package toberumono.utils.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import toberumono.utils.classes.dynamic.Clone;
import toberumono.utils.classes.dynamic.Copy;
import toberumono.utils.classes.dynamic.Property;

/**
 * This class is designed to facilitate container classes that can be extended quickly and relatively painlessly by using the reflections API and
 * marking fields with the {@link Property}, {@link Clone}, and {@link Copy} annotations to dynamically create the copy constructor (which calls
 * {@link #transferFields(Configuration, Configuration)}) and the {@link #equals(Object)}, {@link #load(Properties)}, and {@link #save(Writer)}
 * methods for all of its subclasses at runtime.
 *
 * @author Toberumono
 * @version 1.1
 * @see Property
 * @see Clone
 * @see Copy
 */
public class Configuration implements Cloneable {
	
	/**
	 * This is used in {@link #save(PrintWriter)} to make the = signs line up.
	 */
	private int fieldNameWidth = 35;
	
	/**
	 * Public null constructor used to create a set of variables with default values and also during stream loading.
	 */
	public Configuration() {/* All default values are in the field declarations */}
	
	/**
	 * Copy constructor for a {@link Configuration} object
	 * 
	 * @param original
	 *            the {@link Configuration} to be copied
	 */
	public Configuration(Configuration original) {
		original.transferFields(this);
	}
	
	/**
	 * Copies or clones all of the appropriate fields
	 * 
	 * @param source
	 *            the {@link Configuration} instance to copy from
	 * @param destination
	 *            the {@link Configuration} instance to copy to
	 * @param <T>
	 *            used to synchronize the types in the transfer. This will be automatically determined if the method is used correctly.
	 */
	public static final <T extends Configuration> void transferFields(T source, T destination) {
		source.transferFields(destination);
	}
	
	protected void transferFields(Configuration destination) {
		try {
			for (Field f : getClass().getFields()) {
				f.setAccessible(true); //This is required in order for the field access to not throw IllegalAccessExceptions
				if (f.getName().length() > destination.fieldNameWidth) //For use in the printing function
					destination.fieldNameWidth = f.getName().length();
				if (f.isAnnotationPresent(Clone.class)) { //If this field should be cloned when the Configuration is cloned, use the copy constructor
					cloneAttempt: {
						if (Cloneable.class.isAssignableFrom(f.getType())) {
							try {
								Method clone = f.getType().getMethod("clone");
								clone.setAccessible(true);
								f.set(destination, clone.invoke(f.get(this)));
								break cloneAttempt;
							}
							catch (IllegalArgumentException | SecurityException | ReflectiveOperationException | ExceptionInInitializerError e) {
								//Nothing to do here - we just use the clone method if possible
							}
						}
						Constructor<?> cons = f.getType().getConstructor(f.getType());
						cons.setAccessible(true);
						f.set(destination, cons.newInstance(f.get(this)));
					}
				}
				//Otherwise, if this is a property or some other value that should be copied, copy it
				else if (f.isAnnotationPresent(Property.class) || f.isAnnotationPresent(Copy.class))
					f.set(destination, f.get(this));
			}
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException("Unable to transfer all fields.", e);
		}
	}
	
	/**
	 * Calls {@code new Configuration(this)} and <i>must</i> be overridden by extending classes.
	 *
	 * @return a clone of this {@link Configuration}
	 */
	@Override
	public synchronized Object clone() {
		try {
			Configuration out = (Configuration) super.clone();
			transferFields(out);
			return out;
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); //CloneNotSupportedException cannot occur
		}
	}
	
	/**
	 * Determines whether this set of variables equals another object. It returns true if <tt>other</tt> is not null, is an instance of
	 * {@link Configuration} or a subclass thereof, and contains the same field values.<br>
	 * This method tests only the public fields annotated with {@link Property}.
	 *
	 * @param other
	 *            any object reference including null; however, this method will only return true if <tt>other</tt> is non-null and an instance of
	 *            {@link Configuration} or a subclass thereof
	 * @return true if this and <tt>other</tt>'s public fields annotated with {@link Property} are equivalent.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null || !this.getClass().equals(other.getClass()))
			return false;
		try {
			for (Field f : this.getClass().getFields()) {
				f.setAccessible(true); //This is required in order for the field access to not throw IllegalAccessExceptions
				if (f.isAnnotationPresent(Property.class)) {
					Object val = f.get(this);
					if ((val == null && val != f.get(other)) || (val != null && !val.equals(f.get(other))))
						return false;
				}
			}
		}
		catch (IllegalArgumentException | ReflectiveOperationException e) {
			System.err.println("Unable to complete an equality check.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Loads the values from a {@link Properties} container (read from a configuration file) into the {@link Configuration Configuration's} fields. If
	 * the property strings are invalid, default values for the affected fields remain unchanged. If props is null, nothing happens.
	 * 
	 * @param props
	 *            the {@link Properties} container to load from
	 */
	@SuppressWarnings("unchecked")
	public synchronized final void load(Properties props) {
		if (props == null)
			return;
		try {
			Property p = null;
			for (Field f : this.getClass().getFields()) {
				f.setAccessible(true); //This is required in order for the field access to not throw IllegalAccessExceptions
				if (f.getName().length() > fieldNameWidth) //For use in the printing function
					fieldNameWidth = f.getName().length();
				if ((p = f.getAnnotation(Property.class)) != null) { //If this field is a property, then we can load it
					if (f.getType().isEnum()) {
						Enum<?> en = (Enum<?>) f.get(this);
						f.set(this, props.get(f.getName(), en, en)); //This converts a String to a specific value in an enum type
					}
					else if (p.selection().length != 0 && f.get(this) instanceof Integer) //If this is an integer index in a String[]-based enum
						f.set(this, props.get(f.getName(), p.selection(), (Integer) f.get(this)));
					else
						//If this is not a Java enum and is not an integer index in a String[]-based enum
						f.set(this, props.get(f.getName(), (Class<Object>) f.getType(), f.get(this)));
				}
			}
		}
		catch (IllegalArgumentException | ReflectiveOperationException e) {
			System.err.println("Unable to load properties.");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the {@link Configuration} fields in a format that can be read by an {@link Properties} object.
	 * 
	 * @param writer
	 *            the {@link Writer} onto which to write the data
	 * @throws IOException
	 *             if an IO error occurred
	 */
	public void save(Writer writer) throws IOException {
		try {
			Property p = null;
			String propertyFormatString = "%-" + fieldNameWidth + "s = ";
			Field[] fields = this.getClass().getFields();
			Arrays.parallelSort(fields, (a, b) -> a.getName().compareTo(b.getName()));
			for (Field f : fields) {
				f.setAccessible(true); //This is required in order for the field access to not throw IllegalAccessExceptions
				if ((p = f.getAnnotation(Property.class)) != null) { //If this is a Property
					writer.write(String.format(propertyFormatString, f.getName())); //Print the name of the property all nice and pretty
					if (f.getType().isEnum()) //If this is a Java enum
						writer.write(((Enum<?>) f.get(this)).name() + "\n");
					else if (p.selection().length != 0 && f.get(this) instanceof Integer) //If this is an integer index in a String[]-based enum
						writer.write(p.selection()[(Integer) f.get(this)] + "\n");
					else
						//If this is not a Java enum and is not an integer index in a String[]-based enum
						writer.write(arrayToString(f.get(this)) + "\n");
				}
			}
		}
		catch (IllegalArgumentException | ReflectiveOperationException e) {
			throw new IOException(e); //Something went wrong with the IO... It stands to reason that this should be re-thrown as an IOException.
		}
	}
	
	/**
	 * Recursively constructs a {@link String} representation of an array.
	 * 
	 * @param o
	 *            an {@link Object} of any type, including arrays, or {@code null}
	 * @return "null" if o is {@code null}, {@code o.toString()} if <tt>o</tt> is not an array, or a {@link String} representation of the array with
	 *         recursive calls to {@link #arrayToString(Object)}
	 */
	private String arrayToString(Object o) {
		if (o == null)
			return "null";
		if (!o.getClass().isArray())
			return o.toString();
		final StringBuilder sb = new StringBuilder();
		sb.append("{");
		Arrays.stream((Object[]) o).forEach(i -> sb.append(arrayToString(i)).append(", "));
		if (sb.length() > 1)
			sb.delete(sb.length() - 2, sb.length());
		else
			sb.append(" ");
		return sb.append("}").toString();
	}
}
