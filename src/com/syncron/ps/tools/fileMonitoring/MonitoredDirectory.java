package com.syncron.ps.tools.fileMonitoring;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Stores information about transferring files
 * from a local directory to a remote one.
 * @author micsie
 *
 */
public class MonitoredDirectory {
	
	private String localDirectory;
	private String remoteDirectory;
	
	/**
	 * A list of regular expressions: the files with names
	 * matching any of those should be sent.
	 */
	private List<String> filterMasks;
	
	/**
	 * The constructor sets both local and remote directories, and
	 * initializes the list of filter masks.
	 * @param localDirectory
	 * @param remoteDirectory
	 */
	public MonitoredDirectory(String localDirectory, String remoteDirectory) {
		this.localDirectory = localDirectory;
		this.remoteDirectory = remoteDirectory;
		filterMasks = new LinkedList<String>();
	}
	
	/**
	 * Adds a filter mask for a file name.
	 * @param mask	regular expression
	 */
	public void registerMask(String mask) {
		filterMasks.add(mask);
	}
	
	/**
	 * Checks if fileName matches any filter mask.
	 * @param fileName	a string in question
	 * @return	true iff <b>fileName</b> matches to any filter mask
	 */
	public boolean filter(String fileName) {
		
		boolean result = false;
		
		for (String mask : filterMasks) {
			if (fileName.matches(mask)) {
				return true;
			}
		}
		
		return result;
	}
	
	public String getLocalDirectory() {
		return localDirectory;
	}
	
	public String getRemoteDirectory() {
		return remoteDirectory;
	}
	
}
