package com.syncron.ps.tools.fileMonitoring;

/**
 * 
 * An exception thrown in case connection details
 * cannot be read from the property file.
 * 
 * @author micsie
 *
 */
public class ConnectionDetailsException extends Exception {

	private static final long serialVersionUID = -7228986799097686992L;
	
	private String property;
	
	/**
	 * The constructor sets the property that is missing
	 * in the configuration file.
	 * @param property	The missing property.
	 */
	public ConnectionDetailsException(String property) {
		this.property = property;
	}
	
	@Override
	public String getMessage() {
		return "Cannot load connection details. No property: " + property;
	}

}
