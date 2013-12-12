package com.syncron.ps.tools.fileMonitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

/**
 * 
 * An implementation of FileSender that uses FTP
 * to transfer local files to a remote location.
 * <br/>
 * TODO: No 'retry' functionality implemented.
 * <br/>
 * TODO: No temp directory on the remote side implemented (is it necessary?)
 * 
 * @author micsie
 *
 */
public class FTPFileSender implements FileSender {
	
	public static final Logger logger = Logger.getLogger(SFTPFileSender.class);
	
	private String user;
	private String password;
	private String host;

	@Override
	public void transferFile(Path path, String remoteDir)
			throws TransferFailedException {
		
		FTPClient ftpClient = new FTPClient();
		FileInputStream inputStream = null;
		
		try {
			
			ftpClient.connect(host);
			
			try {
				ftpClient.login(user, password);
				inputStream = new FileInputStream(path.toFile());
				ftpClient.storeFile(remoteDir, inputStream);
			} catch (IOException e) {
				if (inputStream != null) {
					inputStream.close();
				}
				ftpClient.disconnect();
				throw e;
			}
			
			
		} catch (IOException e) {
			throw new TransferFailedException(path.getFileName().toString());
		}
		
	}

}
