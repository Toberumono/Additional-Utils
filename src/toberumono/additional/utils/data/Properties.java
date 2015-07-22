package toberumono.additional.utils.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extension of {@link java.util.HashMap HashMap} that simplifies the loading and storing of properties on disk.
 * 
 * @author Toberumono
 */
public class Properties extends HashMap<String, String> {
	/* ********************************************************************** */
	/*          Property conversion functions for some basic types            */
	/* ********************************************************************** */
	
	private static final BiFunction<String, Integer, Integer> toInt = (s, d) -> {
		try {
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			return (Integer) d;
		}
	};
	private static final BiFunction<String, Long, Long> toLong = (s, d) -> {
		try {
			return Long.parseLong(s);
		}
		catch (NumberFormatException e) {
			return (Long) d;
		}
	};
	private static final BiFunction<String, Double, Double> toDouble = (s, d) -> {
		try {
			return Double.parseDouble(s);
		}
		catch (NumberFormatException e) {
			return (Double) d;
		}
	};
	private static final BiFunction<String, Boolean, Boolean> toBoolean = (s, d) -> {
		try {
			return Integer.parseInt(s) != 0;
		}
		catch (NumberFormatException e) {
			return Boolean.valueOf(s);
		}
	};
	private static final HashMap<Class<?>, BiFunction<String, ?, ?>> converters;
	
	static {
		converters = new HashMap<>();
		addBiFunction(Integer.class, toInt);
		addBiFunction(int.class, toInt);
		addBiFunction(Long.class, toLong);
		addBiFunction(long.class, toLong);
		addBiFunction(Double.class, toDouble);
		addBiFunction(double.class, toDouble);
		addBiFunction(Boolean.class, toBoolean);
		addBiFunction(boolean.class, toBoolean);
		addBiFunction(String.class, (s, d) -> (s == null ? (String) d : s));
		addBiFunction(String[].class, (s, d) -> {
			if (s == null)
				return d;
			String in = s.trim();
			if (in.charAt(0) == '{')
				in = in.substring(1);
			if (in.charAt(in.length() - 1) == '{')
				in = in.substring(0, in.length() - 1);
			return in.split("\\s*(?<!\\\\),\\s*");//This automatically trims the elements
			});
	}
	
	/* ********************************************************************** */
	/*                  And now for the actual object code                    */
	/* ********************************************************************** */
	
	/**
	 * Creates an empty property set.
	 */
	public Properties() {
		super();
	}
	
	/**
	 * Creates a {@link Properties} object with given values.<br>
	 * Also doubles as a copy constructor.
	 * 
	 * @param defaults
	 *            the default property values
	 */
	public Properties(Map<String, String> defaults) {
		super(defaults);
	}
	
	/**
	 * Loads the properties stored in the file at <tt>file</tt>
	 * 
	 * @param file
	 *            the path to the file to load properties from
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public Properties(Path file) throws IOException {
		load(file);
	}
	
	/**
	 * Looks up a property value based on a key. The key is forced to lowercase before accessing the map.<br>
	 * 
	 * @param key
	 *            the name of the property value
	 * @return the value mapped to <tt>key</tt> as a {@link String}, or {@code null} if <tt>key</tt> is not in the map
	 */
	public String get(String key) {
		return super.get(key.toLowerCase());
	}
	
	/**
	 * Looks up a property value based on a key. The key is forced to lowercase before accessing the map.<br>
	 * If the property is found, it gets converted to the type specified by <tt>type</tt>.<br>
	 * If the property cannot be found, it returns <tt>def</tt>.
	 * 
	 * @param key
	 *            the name of the property value
	 * @param type
	 *            a {@link Class} object representing the type to which to convert the property
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @param <T>
	 *            used to synchronize the return type, class definition's type, and the type of the default (<tt>def</tt>)
	 *            return value. <i>This is automatically determined by the Java compiler if the method is used correctly.</i>
	 * @return the property converted to type <tt>T</tt> or <tt>def</tt>
	 */
	public <T> T get(String key, Class<T> type, T def) {
		@SuppressWarnings("unchecked")
		T out = (T) ((BiFunction<String, T, T>) converters.get(type)).apply(get(key), def);
		return out;
	}
	
	/**
	 * Looks up an enumerated property value based on a key. The key is forced to lowercase before accessing the map.<br>
	 * If the property is found, its index in {@code enumerated} is returned.<br>
	 * If the property cannot be found or is not in {@code enumerated}, it returns <tt>def</tt>.
	 * 
	 * @param key
	 *            the name of the property value
	 * @param enumerated
	 *            an array containing all of values of the enumerated type
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @return the index of the value mapped to key or def if the value is not in the map or enum list
	 * @see #getEnumeratedType(String, String[], int)
	 */
	public Integer get(String key, String[] enumerated, Integer def) {
		return getEnumeratedType(key, enumerated, def);
	}
	
	/**
	 * Looks up an enum property value by key. The key is forced to lowercase before accessing the map.<br>
	 * 
	 * @param key
	 *            the name of the property value
	 * @param enumerated
	 *            an {@link Enum} object representing the enumerated type that the value of this property represents
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @param <T>
	 *            used to synchronize the return type, class definition's type, and the type of the default (<tt>def</tt>)
	 *            return value. <i>This is automatically determined by the Java compiler if the method is used correctly.</i>
	 * @return the {@link Enum} value of the same name as the value stored in the property or <tt>def</tt>
	 * @see #getEnumeratedType(String, Enum, Enum)
	 */
	public <T extends Enum<?>> T get(String key, T enumerated, T def) {
		return getEnumeratedType(key, enumerated, def);
	}
	
	/**
	 * Loads properties from the file at p. A property consists of a key, and equals sign, and a value. The value cannot
	 * contain spaces. Any space, tab, !, or # after the start of the value component marks the beginning of a comment.
	 * 
	 * @param file
	 *            the {@link Path} to the file to load properties from
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public synchronized void load(Path file) throws IOException {
		final Pattern prop = Pattern.compile("(.+?) *= *(.+?)(#.*)?$");
		Files.lines(file).forEach(s -> {
			Matcher m = prop.matcher(s);
			if (m.find())
				put(m.group(1).trim().toLowerCase(), m.group(2).trim().toLowerCase());
		});
	}
	
	/**
	 * Writes the contents of the properties to System.out. For testing and debugging.
	 */
	public void show() {
		forEach((k, v) -> {
			System.out.println(k + "=" + v);
		});
	}
	
	/**
	 * Convenience method for {@link #get(String, Class, Object) get(property, int.class, def)}.
	 * 
	 * @param property
	 *            the name of the property value
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @return the value mapped to <tt>property</tt> as an int or <tt>def</tt> if the property cannot be found
	 * @see #get(String, Class, Object)
	 */
	public int getInt(String property, int def) {
		return get(property, int.class, def);
	}
	
	/**
	 * Convenience method for {@link #get(String, Class, Object) get(property, double.class, def)}.
	 * 
	 * @param property
	 *            the name of the property value
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @return the value mapped to <tt>property</tt> as a double or <tt>def</tt> if the property cannot be found
	 * @see #get(String, Class, Object)
	 */
	public double getDouble(String property, double def) {
		return get(property, double.class, def);
	}
	
	/**
	 * Convenience method for {@link #get(String, Class, Object) get(property, boolean.class, def)}.
	 * 
	 * @param property
	 *            the name of the property value
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @return the value mapped to <tt>property</tt> as a boolean or <tt>def</tt> if the property cannot be found
	 * @see #get(String, Class, Object)
	 */
	public boolean getBoolean(String property, boolean def) {
		return get(property, boolean.class, def);
	}
	
	/**
	 * Gets the property mapped to <tt>property</tt> as type <tt>T</tt>
	 * 
	 * @param property
	 *            the name of the property value
	 * @param converter
	 *            a function that converts a value from {@link String} to <tt>T</tt> or returns <tt>def</tt> if the
	 *            conversion fails
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @param <T>
	 *            used to synchronize the return type, class definition's type, and the type of the default (<tt>def</tt>)
	 *            return value. <i>This is automatically determined by the Java compiler if the method is used correctly.</i>
	 * @return the value mapped to <tt>property</tt> as <tt>T</tt> or <tt>def</tt> if the property could not be found or if
	 *         the conversion failed
	 */
	public <T> T getProperty(String property, BiFunction<String, T, T> converter, T def) {
		return converter.apply(get(property), def);
	}
	
	/**
	 * Looks up an enum property value by key. The key is forced to lowercase before accessing the map.<br>
	 * 
	 * @param property
	 *            the name of the property value
	 * @param enumerated
	 *            an {@link Enum} object representing the enumerated type that the value of this property represents
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @param <T>
	 *            used to synchronize the return type, class definition's type, and the type of the default (<tt>def</tt>)
	 *            return value. <i>This is automatically determined by the Java compiler if the method is used correctly.</i>
	 * @return the {@link Enum} value of the same name as the value stored in the property or <tt>def</tt>
	 * @see #get(String, Enum, Enum)
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<?>> T getEnumeratedType(String property, T enumerated, T def) {
		String s = get(property);
		if (s == null)
			return def;
		try {
			return (T) enumerated.getClass().getEnumConstants()[Integer.parseInt(s)]; //If the property was an index in the enum list
		}
		catch (NumberFormatException e) {}
		try {
			return (T) enumerated.valueOf(enumerated.getClass(), s); //If the property is the name of an item in the enum type
		}
		catch (IllegalArgumentException e1) {}
		return def;
	}
	
	/**
	 * Looks up the enumerated property value bound to <tt>property</tt>. <tt>property</tt> is forced to lowercase before
	 * accessing the map.<br>
	 * If the property is found, its index in {@code enumerated} is returned.<br>
	 * If the property cannot be found or is not in {@code enumerated}, it returns <tt>def</tt>.
	 * 
	 * @param property
	 *            the name of the property value
	 * @param enumerated
	 *            an array containing all of values of the enumerated type
	 * @param def
	 *            the default value which is returned if the property cannot be found
	 * @return the index of the value mapped to key or def if the value is not in the map or enum list
	 * @see #get(String, String[], Integer)
	 */
	public int getEnumeratedType(String property, String[] enumerated, int def) {
		String s = get(property);
		if (s == null)
			return def;
		try {
			//use integer value if specified
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			//check string values
			for (int i = 0; i < enumerated.length; i++)
				if (s.equalsIgnoreCase(enumerated[i]))
					return i;
			return def;
		}
	}
	
	/**
	 * Used to add a {@link BiFunction} to the converters map. The converter must have the same return type as the type it is
	 * being mapped to.
	 * 
	 * @param type
	 *            the type that this converter should be used for
	 * @param converter
	 *            the converter
	 * @param <T>
	 *            used to synchronize the <tt>converter</tt>'s return type and class definition's type. <i>This is
	 *            automatically determined by the Java compiler if the method is used correctly.</i>
	 * @return the converter that was previously mapped to <tt>type</tt> or {@code null} if there wasn't one
	 */
	@SuppressWarnings("unchecked")
	public static <T> BiFunction<String, T, T> addBiFunction(Class<T> type, BiFunction<String, T, T> converter) {
		return (BiFunction<String, T, T>) converters.put(type, converter);
	}
}
