package com.syncron.ps.tools.fileMonitoring;

/**
 * 
 * An exception thrown when file transfer failed. The exception object
 * holds information about necessity of re-sending the file.
 * 
 * @author micsie
 *
 */
public class TransferFailedException extends Exception {

	private static final long serialVersionUID = -2804219155590641036L;

	/**
	 * The name of the file transfer of which failed.
	 */
	private String fileName;
	
	/**
	 * If {@code true}, means the monitor should try to transfer the file
	 * again later.
	 */
	private boolean retry;

	/**
	 * The constructor sets the file name.
	 * @param fileName	The name of the file transfer of which failed.
	 */
	public TransferFailedException(String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * The constructor sets the file name and, additionally, states if
	 * the file should be re-sent.
	 * @param fileName	The name of the file transfer of which failed.
	 * @param retry		If the file should be re-sent.
	 */
	public TransferFailedException(String fileName, boolean retry) {
		this.fileName = fileName;
		this.retry = retry;
	}
	
	@Override
	public String getMessage() {
		return "Transfer of the file " + fileName + " failed.";
	}
	
	public boolean retry() {
		return this.retry;
	}
}
