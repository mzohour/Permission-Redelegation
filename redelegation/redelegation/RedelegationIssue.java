/**
 * 
 */
package redelegation;

/**
 * @author mzohour
 *
 */
public class RedelegationIssue {
	
	private String mPermissionName;
	private String mOriginComponent;
	private String mClassName;
	private String mLine;
	
	public RedelegationIssue(
			String permissionName,
			String originComponent,
			String className, 
			String line
			) {
		setmPermissionName(permissionName);
		setmOriginComponent(originComponent);
		setmClassName(className);
		setmLine(line);
	}
	
	public void raiseIssue() {
		String issue = "\nVulnerable component: " + mOriginComponent + "\nPermission leaked: " +
				mPermissionName + "\nClass name:" + mClassName + "\nReason:" + mLine;
		System.err.println(issue);
	}

	public String getmLine() {
		return mLine;
	}

	private void setmLine(String mLine) {
		this.mLine = mLine;
	}

	public String getmClassName() {
		return mClassName;
	}

	private void setmClassName(String mClassName) {
		this.mClassName = mClassName;
	}

	public String getmOriginComponent() {
		return mOriginComponent;
	}

	private void setmOriginComponent(String mOriginComponent) {
		this.mOriginComponent = mOriginComponent;
	}

	public String getmPermissionName() {
		return mPermissionName;
	}

	private void setmPermissionName(String mPermissionName) {
		this.mPermissionName = mPermissionName;
	}
}
