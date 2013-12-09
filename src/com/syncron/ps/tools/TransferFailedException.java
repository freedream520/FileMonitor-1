package com.syncron.ps.tools;

public class TransferFailedException extends Exception {

	private static final long serialVersionUID = -2804219155590641036L;

	private String fileName;
	
	public TransferFailedException(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public String getMessage() {
		return "Transfer of the file " + fileName + " failed.";
	}
}
