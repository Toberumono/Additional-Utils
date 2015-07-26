package toberumono.utils.security;

import java.security.BasicPermission;
import java.security.Permission;

import toberumono.utils.files.NativeLibraryManager;

/**
 * Permissions for the {@link NativeLibraryManager}.
 * 
 * @author Toberumono
 * @see NativeLibraryManager
 */
public class NativeLibraryPermission extends BasicPermission {
	private final String start;
	
	/**
	 * Constructs a {@link NativeLibraryPermission}.<br>
	 * Name structure: (sources.(view | add | remove | using) | libraries.(view | load | using))
	 * 
	 * @param name
	 *            the name of this permission
	 */
	public NativeLibraryPermission(String name) {
		super(name);
		start = name.substring(0, name.indexOf('.'));
	}
	
	/**
	 * Constructs a {@link NativeLibraryPermission}.<br>
	 * Name structure: (sources.(view | add | remove | using) | libraries.(view | load | using))
	 * 
	 * @param name
	 *            the name of this permission
	 * @param actions
	 *            ignored
	 */
	public NativeLibraryPermission(String name, String actions) {
		super(name, actions);
		start = name.substring(0, name.indexOf('.'));
	}
	
	@Override
	public boolean implies(Permission p) {
		if (super.implies(p))
			return true;
		if (p == null || (p.getClass() != getClass()))
			return false;
		//If the requester holds any sources permissions and wants to test if a source has been added, then it can.
		//If the requester holds any libraries permissions and wants to test if a library has been loaded, then it can.
		if (start.equals(((NativeLibraryPermission) p).start) && p.getName().startsWith("using", start.length()))
			return true;
		return false;
	}
}
