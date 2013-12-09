package com.syncron.ps.tools;

/**
 * 
 * An exception thrown in case connection details cannot be read from the property file.
 * @author micsie
 *
 */
public class ConnectionDetailsException extends Exception {

	private static final long serialVersionUID = -7228986799097686992L;
	
	private String property;
	
	public ConnectionDetailsException(String property) {
		this.property = property;
	}
	
	@Override
	public String getMessage() {
		return "Cannot load connection details. No property: " + property;
	}

}
