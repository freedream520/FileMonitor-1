package com.syncron.ps.tools.fileMonitoring;

import java.nio.file.Path;

/**
 * 
 * An interface for transferring files to a remote location.
 * @author micsie
 *
 */
public interface FileSender {
	
	/**
	 * Transfers a file to a remote location.
	 * @param path			A path to a local file.
	 * @param remoteDir		A directory on the remote server.
	 * @throws TransferFailedException
	 */
	public void transferFile(Path path, String remoteDir)
			throws TransferFailedException;
	
}
