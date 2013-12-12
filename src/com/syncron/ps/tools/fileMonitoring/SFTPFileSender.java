package com.syncron.ps.tools.fileMonitoring;

import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 
 * An implementation of FileSender that uses SFTP
 * to transfer local files to a remote location.
 * @author micsie
 *
 */
public class SFTPFileSender implements FileSender {
	
	public static final Logger logger = Logger.getLogger(SFTPFileSender.class);
	
	private String user;
	private String password;
	private String host;
	private Integer port;
	
	/**
	 * The constructor uses the default sFTP port number 22.
	 * @param user			The user name.
	 * @param password		The password.
	 * @param host			The host name or IP address.
	 */
	public SFTPFileSender(String user, String password, String host) {
		initialize(user, password, host, 22);
	}
	
	/**
	 * The constructor uses the port number provided as an argument.
	 * @param user			The user name.
	 * @param password		The password.
	 * @param host			The host name or IP address.
	 * @param port			The port number
	 */
	public SFTPFileSender(String user, String password, String host,
			Integer port) {
		
		initialize(user, password, host, port);
	}

	/**
	 * Initializes {@link #user}, {@link #password}, {@link #host},
	 * and {@link #user} from arguments.
	 * @param user
	 * @param password
	 * @param host
	 * @param port
	 */
	private void initialize(String user, String password, String host,
			Integer port) {
		this.user = user;
		this.password = password;
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void transferFile(Path path, String remoteDir)
			throws TransferFailedException {
		
		JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;
        ChannelSftp sftpChannel = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            try {
	            
            	channel = session.openChannel("sftp");
	            channel.connect();
	            
	            try {
		            
	            	sftpChannel = (ChannelSftp) channel;
		            sftpChannel.cd(remoteDir);
		            sftpChannel.put(path.toString(),
		            		path.getFileName().toString());
		            
	            } catch (SftpException e) {
	            	// TODO: do we want to handle retrying?
	            	// 		 for now it's not handled at all
	            	throw new TransferFailedException(
	            			path.getFileName().toString(), false);
	            } finally {
	            	sftpChannel.disconnect();
	            }
            } catch (JSchException e) {
            	throw e;
            } finally {
            	session.disconnect();
            }
        } catch (JSchException e) {
            throw new TransferFailedException(path.getFileName().toString());
        }
	}
	
}
